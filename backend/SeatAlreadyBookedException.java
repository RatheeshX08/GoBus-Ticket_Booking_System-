package com.busbooking.exception;

/**
 * Custom Checked Exception thrown when a user attempts to select or reserve an already occupied seat.
 */
public class SeatAlreadyBookedException extends Exception {
    
    private String seatNumber;

    public SeatAlreadyBookedException(String message) {
        super(message);
    }

    public SeatAlreadyBookedException(String seatNumber, String message) {
        super("Seat " + seatNumber + " is already booked: " + message);
        this.seatNumber = seatNumber;
    }

    public String getSeatNumber() {
        return seatNumber;
    }
}
