package com.busbooking.service;

import com.busbooking.model.Bus;
import com.busbooking.model.Booking;
import com.busbooking.model.Passenger;
import com.busbooking.exception.SeatAlreadyBookedException;
import com.busbooking.exception.InvalidBookingException;
import java.util.List;

/**
 * Service interface defining contract for search and reservation operations.
 */
public interface IBookingService {
    
    List<Bus> searchBuses(String origin, String destination, String date);

    List<Bus> getBusesSortedByPrice(List<Bus> buses);

    List<Bus> getBusesSortedByRating(List<Bus> buses);

    Booking bookTicket(int busId, Passenger passenger, List<String> seatNumbers, String journeyDate) 
            throws SeatAlreadyBookedException, InvalidBookingException;

    boolean cancelBooking(String pnr);

    default String generatePnr(int busId) {
        long timestamp = System.currentTimeMillis() % 100000;
        return "RV-" + busId + "-" + timestamp;
    }
}
