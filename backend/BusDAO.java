package com.busbooking.dao;

import com.busbooking.model.Bus;
import com.busbooking.model.Seat;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object executing parameterized SQL queries to search buses and retrieve seat maps.
 */
public class BusDAO {

    public List<Bus> findBusesByRoute(String origin, String destination) throws SQLException {
        List<Bus> busList = new ArrayList<>();
        String sql = "SELECT b.bus_id, b.vehicle_number, b.operator_name, b.bus_type, r.origin, r.destination, " +
                     "b.departure_time, b.arrival_time, b.base_fare, b.rating, b.total_capacity, b.available_seats " +
                     "FROM buses b JOIN routes r ON r.route_id = b.route_id " +
                     "WHERE b.is_active = TRUE AND LOWER(r.origin) = LOWER(?) AND LOWER(r.destination) = LOWER(?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, origin);
            pstmt.setString(2, destination);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Bus bus = new Bus(
                        rs.getInt("bus_id"),
                        rs.getString("vehicle_number"),
                        rs.getString("operator_name"),
                        rs.getString("bus_type"),
                        rs.getString("origin"),
                        rs.getString("destination"),
                        rs.getString("departure_time"),
                        rs.getString("arrival_time"),
                        rs.getDouble("base_fare"),
                        rs.getDouble("rating"),
                        rs.getInt("total_capacity"),
                        rs.getInt("available_seats")
                    );
                    busList.add(bus);
                }
            }
        }
        return busList;
    }

    public List<Seat> getSeatsByBusId(int busId) throws SQLException {
        List<Seat> seats = new ArrayList<>();
        String sql = "SELECT seat_id, bus_id, seat_number, deck, price, status " +
                     "FROM seats WHERE bus_id = ? ORDER BY deck ASC, seat_id ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, busId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String status = rs.getString("status");
                    Seat.Status seatStatus = switch (status.toLowerCase()) {
                        case "sold" -> Seat.Status.BOOKED;
                        case "ladies" -> Seat.Status.LADIES;
                        default -> Seat.Status.AVAILABLE;
                    };
                    Seat seat = new Seat(
                        rs.getInt("seat_id"),
                        rs.getInt("bus_id"),
                        rs.getString("seat_number"),
                        Seat.Deck.valueOf(rs.getString("deck").toUpperCase()),
                        rs.getDouble("price"),
                        seatStatus
                    );
                    seats.add(seat);
                }
            }
        }
        return seats;
    }
}
