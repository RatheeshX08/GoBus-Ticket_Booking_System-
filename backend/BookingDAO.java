package com.busbooking.dao;

import com.busbooking.model.Booking;
import com.busbooking.model.Passenger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles atomic booking transactions with commit and rollback.
 */
public class BookingDAO {

    public boolean createBookingTransaction(Booking booking, List<String> seatNumbers) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Passenger passenger = booking.getPassenger();
                String passengerSql = "INSERT INTO passengers (full_name, age, gender, phone, email) VALUES (?, ?, ?, ?, ?)";
                int passengerId;
                try (PreparedStatement statement = conn.prepareStatement(passengerSql, Statement.RETURN_GENERATED_KEYS)) {
                    statement.setString(1, passenger.getFullName());
                    statement.setInt(2, passenger.getAge());
                    statement.setString(3, passenger.getGender());
                    statement.setString(4, passenger.getPhone());
                    statement.setString(5, passenger.getEmail());
                    statement.executeUpdate();
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("Passenger insert did not return an ID.");
                        passengerId = keys.getInt(1);
                        passenger.setPassengerId(passengerId);
                    }
                }

                List<Integer> seatIds = new ArrayList<>();
                List<Double> seatPrices = new ArrayList<>();
                String lockSeatSql = "SELECT seat_id, price FROM seats " +
                        "WHERE bus_id = ? AND seat_number = ? AND status = 'available' FOR UPDATE";
                try (PreparedStatement statement = conn.prepareStatement(lockSeatSql)) {
                    for (String seatNumber : seatNumbers) {
                        statement.setInt(1, booking.getBusId());
                        statement.setString(2, seatNumber);
                        try (ResultSet result = statement.executeQuery()) {
                            if (!result.next()) {
                                throw new SQLException("Seat " + seatNumber + " is no longer available.");
                            }
                            seatIds.add(result.getInt("seat_id"));
                            seatPrices.add(result.getDouble("price"));
                        }
                    }
                }

                String bookingSql = "INSERT INTO bookings (pnr, bus_id, passenger_id, journey_date, total_amount, booking_status) " +
                        "VALUES (?, ?, ?, ?, ?, 'CONFIRMED')";
                int bookingId;
                try (PreparedStatement statement = conn.prepareStatement(bookingSql, Statement.RETURN_GENERATED_KEYS)) {
                    statement.setString(1, booking.getPnr());
                    statement.setInt(2, booking.getBusId());
                    statement.setInt(3, passengerId);
                    statement.setString(4, booking.getJourneyDate());
                    statement.setDouble(5, booking.getTotalAmount());
                    statement.executeUpdate();
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("Booking insert did not return an ID.");
                        bookingId = keys.getInt(1);
                    }
                }

                String updateSeatSql = "UPDATE seats SET status = 'sold' WHERE seat_id = ? AND status = 'available'";
                String bookingSeatSql = "INSERT INTO booking_seats (booking_id, seat_id, seat_fare) VALUES (?, ?, ?)";
                try (PreparedStatement updateSeat = conn.prepareStatement(updateSeatSql);
                     PreparedStatement bookingSeat = conn.prepareStatement(bookingSeatSql)) {
                    for (int i = 0; i < seatIds.size(); i++) {
                        updateSeat.setInt(1, seatIds.get(i));
                        if (updateSeat.executeUpdate() != 1) {
                            throw new SQLException("A selected seat is no longer available.");
                        }
                        bookingSeat.setInt(1, bookingId);
                        bookingSeat.setInt(2, seatIds.get(i));
                        bookingSeat.setDouble(3, seatPrices.get(i));
                        bookingSeat.executeUpdate();
                    }
                }

                try (PreparedStatement statement = conn.prepareStatement(
                        "UPDATE buses SET available_seats = GREATEST(0, available_seats - ?) WHERE bus_id = ?")) {
                    statement.setInt(1, seatNumbers.size());
                    statement.setInt(2, booking.getBusId());
                    statement.executeUpdate();
                }
                conn.commit();
                System.out.println("[Transaction Success] Booking confirmed with PNR: " + booking.getPnr());
                return true;
            } catch (SQLException e) {
                conn.rollback();
                System.err.println("[Transaction Error] Rolling back changes: " + e.getMessage());
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }
}
