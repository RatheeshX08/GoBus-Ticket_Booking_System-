package com.busbooking.exception;

/**
 * Custom Runtime Exception thrown when passenger or booking inputs are invalid.
 */
public class InvalidBookingException extends RuntimeException {

    public InvalidBookingException(String message) {
        super("Invalid Booking Data: " + message);
    }

    public InvalidBookingException(String message, Throwable cause) {
        super(message, cause);
    }
}
