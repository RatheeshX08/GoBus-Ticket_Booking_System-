package com.busbooking;

import com.busbooking.dao.DBConnection;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class ApiServer {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final int PASSWORD_HASH_ITERATIONS = 180_000;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private ApiServer() {}

    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(setting("PORT", "8080"));
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/", ApiServer::handle);
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();
        System.out.println("GoBus API listening on http://localhost:" + port);
    }

    private static void handle(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        try {
            if ("GET".equals(method) && "/api/health".equals(path)) {
                health(exchange);
            } else if ("POST".equals(method) && "/api/auth/login".equals(path)) {
                login(exchange);
            } else if ("POST".equals(method) && "/api/auth/register".equals(path)) {
                register(exchange);
            } else if ("GET".equals(method) && "/api/buses".equals(path)) {
                getBuses(exchange);
            } else if ("GET".equals(method) && "/api/bookings".equals(path)) {
                getBookings(exchange);
            } else if ("POST".equals(method) && "/api/bookings".equals(path)) {
                createBooking(exchange);
            } else if ("DELETE".equals(method) && path.startsWith("/api/bookings/")) {
                cancelBooking(exchange, path.substring("/api/bookings/".length()));
            } else {
                sendJson(exchange, 404, Map.of("error", "API endpoint not found"));
            }
        } catch (IllegalArgumentException e) {
            sendJson(exchange, 400, Map.of("error", e.getMessage()));
        } catch (SeatUnavailableException e) {
            sendJson(exchange, 409, Map.of("error", e.getMessage()));
        } catch (InvalidCredentialsException e) {
            sendJson(exchange, 401, Map.of("error", e.getMessage()));
        } catch (SQLException e) {
            System.err.println("[API] Database request failed: " + e.getMessage());
            sendJson(exchange, 503, Map.of(
                    "error",
                    "Database request failed. Check MySQL is running, the schema is installed, and DB_URL/DB_USER/DB_PASSWORD are correct."
            ));
        } catch (Exception e) {
            System.err.println("[API] Request failed: " + e.getMessage());
            sendJson(exchange, 500, Map.of("error", "The server could not complete the request."));
        }
    }

    private static void login(HttpExchange exchange)
            throws IOException, SQLException, GeneralSecurityException, InvalidCredentialsException {
        JsonNode request = JSON.readTree(exchange.getRequestBody());
        String email = requiredText(request, "email").toLowerCase(Locale.ROOT);
        String password = request.path("password").asText("");
        if (password.isEmpty()) throw new IllegalArgumentException("Password is required.");
        if (password.length() > 128) throw new IllegalArgumentException("Password is invalid.");
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("A valid email address is required.");
        }

        String sql = "SELECT full_name, password_salt, password_hash, hash_iterations FROM users WHERE email = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) throw new InvalidCredentialsException();
                byte[] candidate = passwordHash(
                        password.toCharArray(),
                        result.getBytes("password_salt"),
                        result.getInt("hash_iterations")
                );
                if (!MessageDigest.isEqual(candidate, result.getBytes("password_hash"))) {
                    throw new InvalidCredentialsException();
                }
                sendJson(exchange, 200, Map.of(
                        "name", result.getString("full_name"),
                        "email", email
                ));
            }
        }
    }

    private static void register(HttpExchange exchange)
            throws IOException, SQLException, GeneralSecurityException {
        JsonNode request = JSON.readTree(exchange.getRequestBody());
        String name = requiredText(request, "name");
        String email = requiredText(request, "email").toLowerCase(Locale.ROOT);
        String password = request.path("password").asText("");
        if (name.length() > 100) throw new IllegalArgumentException("Name must be 100 characters or fewer.");
        if (!EMAIL_PATTERN.matcher(email).matches() || email.length() > 120) {
            throw new IllegalArgumentException("A valid email address is required.");
        }
        if (password.length() < 8 || password.length() > 128) {
            throw new IllegalArgumentException("Password must be between 8 and 128 characters.");
        }

        byte[] salt = new byte[16];
        SECURE_RANDOM.nextBytes(salt);
        byte[] hash = passwordHash(password.toCharArray(), salt, PASSWORD_HASH_ITERATIONS);
        String sql = "INSERT INTO users (full_name, email, password_salt, password_hash, hash_iterations) " +
                "VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            statement.setString(2, email);
            statement.setBytes(3, salt);
            statement.setBytes(4, hash);
            statement.setInt(5, PASSWORD_HASH_ITERATIONS);
            try {
                statement.executeUpdate();
            } catch (SQLException e) {
                if (e.getSQLState() != null && e.getSQLState().startsWith("23")) {
                    sendJson(exchange, 409, Map.of("error", "An account with this email already exists."));
                    return;
                }
                throw e;
            }
        }
        sendJson(exchange, 201, Map.of("name", name, "email", email));
    }

    private static byte[] passwordHash(char[] password, byte[] salt, int iterations)
            throws GeneralSecurityException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } finally {
            spec.clearPassword();
            java.util.Arrays.fill(password, '\0');
        }
    }

    private static void health(HttpExchange exchange) throws SQLException, IOException {
        try (Connection connection = DBConnection.getConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT 1")) {
            result.next();
            sendJson(exchange, 200, Map.of("ok", true, "database", "connected"));
        }
    }

    private static void getBuses(HttpExchange exchange) throws SQLException, IOException {
        Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
        String origin = query.getOrDefault("origin", "").trim();
        String destination = query.getOrDefault("destination", "").trim();
        if (origin.isEmpty() || destination.isEmpty()) {
            throw new IllegalArgumentException("Origin and destination are required.");
        }

        String sql = "SELECT b.bus_id, b.operator_name, b.bus_type, r.origin, r.destination, " +
                "b.departure_time, b.arrival_time, b.base_fare, b.rating, r.estimated_hours " +
                "FROM buses b JOIN routes r ON r.route_id = b.route_id " +
                "WHERE b.is_active = TRUE AND LOWER(r.origin) = LOWER(?) AND LOWER(r.destination) = LOWER(?) " +
                "ORDER BY b.base_fare";
        List<Map<String, Object>> buses = new ArrayList<>();
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, origin);
            statement.setString(2, destination);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    int busId = result.getInt("bus_id");
                    Map<String, Object> bus = new LinkedHashMap<>();
                    bus.put("id", "BUS-" + busId);
                    bus.put("operator", result.getString("operator_name"));
                    bus.put("busType", result.getString("bus_type"));
                    bus.put("origin", result.getString("origin"));
                    bus.put("destination", result.getString("destination"));
                    bus.put("departure", time(result.getString("departure_time")));
                    bus.put("arrival", time(result.getString("arrival_time")));
                    bus.put("duration", duration(result.getDouble("estimated_hours")));
                    bus.put("baseFare", result.getBigDecimal("base_fare"));
                    bus.put("rating", result.getBigDecimal("rating"));
                    bus.put("ratingCount", 0);
                    bus.put("lowerSeats", getSeats(connection, busId, "lower"));
                    bus.put("upperSeats", getSeats(connection, busId, "upper"));
                    buses.add(bus);
                }
            }
        }
        sendJson(exchange, 200, buses);
    }

    private static List<Map<String, Object>> getSeats(Connection connection, int busId, String deck)
            throws SQLException {
        List<Map<String, Object>> seats = new ArrayList<>();
        String sql = "SELECT seat_number, price, status FROM seats " +
                "WHERE bus_id = ? AND deck = ? ORDER BY seat_id";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, busId);
            statement.setString(2, deck);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    Map<String, Object> seat = new LinkedHashMap<>();
                    seat.put("id", result.getString("seat_number"));
                    seat.put("price", result.getBigDecimal("price"));
                    String status = result.getString("status");
                    seat.put("status", "sold".equalsIgnoreCase(status) ? "sold" : "available");
                    seats.add(seat);
                }
            }
        }
        return seats;
    }

    private static void getBookings(HttpExchange exchange) throws SQLException, IOException {
        String sql = "SELECT b.pnr, b.bus_id, bus.operator_name, bus.bus_type, " +
                "CONCAT(r.origin, ' -> ', r.destination) AS route, " +
                "DATE_FORMAT(b.journey_date, '%Y-%m-%d') AS journey_date, " +
                "TIME_FORMAT(bus.departure_time, '%H:%i') AS departure_time, " +
                "p.full_name, b.total_amount, b.booking_status, " +
                "COALESCE(GROUP_CONCAT(s.seat_number ORDER BY s.seat_id SEPARATOR ','), '') AS seat_numbers " +
                "FROM bookings b JOIN buses bus ON bus.bus_id = b.bus_id " +
                "JOIN routes r ON r.route_id = bus.route_id " +
                "JOIN passengers p ON p.passenger_id = b.passenger_id " +
                "LEFT JOIN booking_seats bs ON bs.booking_id = b.booking_id " +
                "LEFT JOIN seats s ON s.seat_id = bs.seat_id " +
                "GROUP BY b.booking_id ORDER BY b.created_at DESC";
        List<Map<String, Object>> bookings = new ArrayList<>();
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                bookings.add(bookingView(
                        result.getString("pnr"),
                        result.getInt("bus_id"),
                        result.getString("operator_name"),
                        result.getString("bus_type"),
                        result.getString("route"),
                        result.getString("journey_date"),
                        result.getString("departure_time"),
                        result.getString("full_name"),
                        splitSeats(result.getString("seat_numbers")),
                        result.getBigDecimal("total_amount"),
                        result.getString("booking_status")
                ));
            }
        }
        sendJson(exchange, 200, bookings);
    }

    private static void createBooking(HttpExchange exchange)
            throws IOException, SQLException, SeatUnavailableException {
        JsonNode request = JSON.readTree(exchange.getRequestBody());
        int busId = parseBusId(requiredText(request, "busId"));
        String journeyDate = requiredText(request, "journeyDate");
        LocalDate parsedDate;
        try {
            parsedDate = LocalDate.parse(journeyDate);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Journey date must use YYYY-MM-DD format.");
        }
        JsonNode passenger = request.path("passenger");
        String name = requiredText(passenger, "name");
        int age = passenger.path("age").asInt(0);
        String gender = requiredText(passenger, "gender");
        String phone = requiredText(passenger, "phone");
        String email = requiredText(passenger, "email");
        if (age < 1 || age > 120) throw new IllegalArgumentException("Passenger age must be between 1 and 120.");
        if (!Set.of("Male", "Female", "Other").contains(gender)) {
            throw new IllegalArgumentException("Passenger gender is invalid.");
        }
        if (phone.replaceAll("\\D", "").length() < 10) {
            throw new IllegalArgumentException("A valid phone number is required.");
        }
        if (!email.contains("@")) throw new IllegalArgumentException("A valid email address is required.");
        JsonNode requestedSeats = request.path("seatNumbers");
        if (!requestedSeats.isArray() || requestedSeats.isEmpty()) {
            throw new IllegalArgumentException("Select at least one seat.");
        }
        List<String> seatNumbers = new ArrayList<>();
        for (JsonNode seat : requestedSeats) {
            String number = seat.asText().trim();
            if (number.isEmpty() || seatNumbers.contains(number)) {
                throw new IllegalArgumentException("Seat selection contains an invalid or duplicate seat.");
            }
            seatNumbers.add(number);
        }

        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                BigDecimal baseFare = BigDecimal.ZERO;
                Map<String, Integer> seatIds = new LinkedHashMap<>();
                Map<String, BigDecimal> seatPrices = new LinkedHashMap<>();
                String seatSql = "SELECT seat_id, price FROM seats " +
                        "WHERE bus_id = ? AND seat_number = ? AND status = 'available' FOR UPDATE";
                try (PreparedStatement statement = connection.prepareStatement(seatSql)) {
                    for (String seatNumber : seatNumbers) {
                        statement.setInt(1, busId);
                        statement.setString(2, seatNumber);
                        try (ResultSet result = statement.executeQuery()) {
                            if (!result.next()) throw new SeatUnavailableException(
                                    "Seat " + seatNumber + " is no longer available. Refresh the seat map and try again."
                            );
                            seatIds.put(seatNumber, result.getInt("seat_id"));
                            BigDecimal price = result.getBigDecimal("price");
                            seatPrices.put(seatNumber, price);
                            baseFare = baseFare.add(price);
                        }
                    }
                }

                BigDecimal discount = baseFare.signum() > 0 ? new BigDecimal("100.00") : BigDecimal.ZERO;
                BigDecimal taxable = baseFare.subtract(discount).max(BigDecimal.ZERO);
                BigDecimal gst = taxable.multiply(new BigDecimal("0.05")).setScale(0, RoundingMode.HALF_UP);
                BigDecimal total = taxable.add(gst);
                String pnr = "RV-" + busId + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();

                int passengerId;
                String passengerSql = "INSERT INTO passengers (full_name, age, gender, phone, email) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement statement = connection.prepareStatement(passengerSql, Statement.RETURN_GENERATED_KEYS)) {
                    statement.setString(1, name);
                    statement.setInt(2, age);
                    statement.setString(3, gender);
                    statement.setString(4, phone);
                    statement.setString(5, email);
                    statement.executeUpdate();
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("Passenger insert did not return an ID.");
                        passengerId = keys.getInt(1);
                    }
                }

                int bookingId;
                String bookingSql = "INSERT INTO bookings (pnr, bus_id, passenger_id, journey_date, total_amount, booking_status) " +
                        "VALUES (?, ?, ?, ?, ?, 'CONFIRMED')";
                try (PreparedStatement statement = connection.prepareStatement(bookingSql, Statement.RETURN_GENERATED_KEYS)) {
                    statement.setString(1, pnr);
                    statement.setInt(2, busId);
                    statement.setInt(3, passengerId);
                    statement.setDate(4, Date.valueOf(parsedDate));
                    statement.setBigDecimal(5, total);
                    statement.executeUpdate();
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("Booking insert did not return an ID.");
                        bookingId = keys.getInt(1);
                    }
                }

                String updateSeatSql = "UPDATE seats SET status = 'sold' WHERE seat_id = ? AND status = 'available'";
                String mapSeatSql = "INSERT INTO booking_seats (booking_id, seat_id, seat_fare) VALUES (?, ?, ?)";
                try (PreparedStatement updateSeat = connection.prepareStatement(updateSeatSql);
                     PreparedStatement mapSeat = connection.prepareStatement(mapSeatSql)) {
                    for (String seatNumber : seatNumbers) {
                        updateSeat.setInt(1, seatIds.get(seatNumber));
                        if (updateSeat.executeUpdate() != 1) {
                            throw new SeatUnavailableException("Seat " + seatNumber + " was booked by someone else.");
                        }
                        mapSeat.setInt(1, bookingId);
                        mapSeat.setInt(2, seatIds.get(seatNumber));
                        mapSeat.setBigDecimal(3, seatPrices.get(seatNumber));
                        mapSeat.executeUpdate();
                    }
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE buses SET available_seats = GREATEST(0, available_seats - ?) WHERE bus_id = ?")) {
                    statement.setInt(1, seatNumbers.size());
                    statement.setInt(2, busId);
                    statement.executeUpdate();
                }
                connection.commit();
                sendJson(exchange, 201, bookingView(
                        pnr, busId, busDetails(connection, busId, "operator_name"),
                        busDetails(connection, busId, "bus_type"),
                        busDetails(connection, busId, "route"),
                        journeyDate, busDetails(connection, busId, "departure_time"),
                        name, seatNumbers, total, "CONFIRMED"
                ));
            } catch (SQLException | RuntimeException | SeatUnavailableException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    private static String busDetails(Connection connection, int busId, String field) throws SQLException {
        String sql = switch (field) {
            case "operator_name", "bus_type", "departure_time" ->
                    "SELECT " + field + " FROM buses WHERE bus_id = ?";
            case "route" ->
                    "SELECT CONCAT(r.origin, ' -> ', r.destination) AS route FROM buses b " +
                            "JOIN routes r ON r.route_id = b.route_id WHERE b.bus_id = ?";
            default -> throw new IllegalArgumentException("Unsupported bus field.");
        };
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, busId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) throw new IllegalArgumentException("Bus was not found.");
                String value = result.getString(field);
                return "departure_time".equals(field) ? time(value) : value;
            }
        }
    }

    private static void cancelBooking(HttpExchange exchange, String rawPnr) throws SQLException, IOException {
        String pnr = URLDecoder.decode(rawPnr, StandardCharsets.UTF_8);
        if (pnr.isBlank()) throw new IllegalArgumentException("PNR is required.");
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int bookingId;
                int busId;
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT booking_id, bus_id FROM bookings WHERE pnr = ? AND booking_status = 'CONFIRMED' FOR UPDATE")) {
                    statement.setString(1, pnr);
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) {
                            connection.rollback();
                            sendJson(exchange, 404, Map.of("error", "Active booking not found."));
                            return;
                        }
                        bookingId = result.getInt("booking_id");
                        busId = result.getInt("bus_id");
                    }
                }
                int seatCount;
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT COUNT(*) FROM booking_seats WHERE booking_id = ?")) {
                    statement.setInt(1, bookingId);
                    try (ResultSet result = statement.executeQuery()) {
                        result.next();
                        seatCount = result.getInt(1);
                    }
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE seats SET status = 'available' WHERE seat_id IN " +
                                "(SELECT seat_id FROM booking_seats WHERE booking_id = ?)")) {
                    statement.setInt(1, bookingId);
                    statement.executeUpdate();
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE buses SET available_seats = available_seats + ? WHERE bus_id = ?")) {
                    statement.setInt(1, seatCount);
                    statement.setInt(2, busId);
                    statement.executeUpdate();
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE bookings SET booking_status = 'CANCELLED' WHERE booking_id = ?")) {
                    statement.setInt(1, bookingId);
                    statement.executeUpdate();
                }
                connection.commit();
                sendJson(exchange, 200, Map.of("cancelled", true, "pnr", pnr));
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    private static Map<String, Object> bookingView(
            String pnr, int busId, String operator, String busType, String route,
            String date, String time, String passenger, List<String> seats,
            BigDecimal total, String status
    ) {
        Map<String, Object> booking = new LinkedHashMap<>();
        booking.put("pnr", pnr);
        booking.put("busId", "BUS-" + busId);
        booking.put("operator", operator);
        booking.put("busType", busType);
        booking.put("route", route);
        booking.put("date", date);
        booking.put("time", time);
        booking.put("passenger", passenger);
        booking.put("seats", seats);
        booking.put("total", total);
        booking.put("status", status);
        return booking;
    }

    private static List<String> splitSeats(String value) {
        if (value == null || value.isBlank()) return List.of();
        return List.of(value.split(","));
    }

    private static int parseBusId(String value) {
        try {
            return Integer.parseInt(value.replaceFirst("^BUS-", ""));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Bus ID is invalid.");
        }
    }

    private static String requiredText(JsonNode node, String field) {
        String value = node.path(field).asText("").trim();
        if (value.isEmpty()) throw new IllegalArgumentException(field + " is required.");
        return value;
    }

    private static Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> values = new LinkedHashMap<>();
        if (rawQuery == null || rawQuery.isBlank()) return values;
        for (String part : rawQuery.split("&")) {
            String[] pair = part.split("=", 2);
            String key = URLDecoder.decode(pair[0], StandardCharsets.UTF_8);
            String value = pair.length > 1 ? URLDecoder.decode(pair[1], StandardCharsets.UTF_8) : "";
            values.put(key, value);
        }
        return values;
    }

    private static String time(String value) {
        if (value == null) return "";
        return value.length() >= 5 ? value.substring(0, 5) : value;
    }

    private static String duration(double hours) {
        int minutes = (int) Math.round(hours * 60);
        return (minutes / 60) + "h " + (minutes % 60) + "m";
    }

    private static String setting(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }

    private static void sendJson(HttpExchange exchange, int status, Object body) throws IOException {
        byte[] response = JSON.writeValueAsBytes(body);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }

    private static final class SeatUnavailableException extends Exception {
        private SeatUnavailableException(String message) {
            super(message);
        }
    }

    private static final class InvalidCredentialsException extends Exception {
        private InvalidCredentialsException() {
            super("Email or password is incorrect.");
        }
    }
}
