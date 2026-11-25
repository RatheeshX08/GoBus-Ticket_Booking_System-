package com.busbooking.service;

import com.busbooking.model.Bus;
import com.busbooking.model.Booking;
import com.busbooking.model.Passenger;
import com.busbooking.model.Seat;
import com.busbooking.exception.SeatAlreadyBookedException;
import com.busbooking.exception.InvalidBookingException;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Service implementation using Java 8+ Streams, Lambda expressions, and Collections framework.
 */
public class BusBookingService implements IBookingService {

    private List<Bus> busCatalog = new ArrayList<>();
    private Map<String, Booking> bookingRegistry = new HashMap<>();

    public BusBookingService() {
        loadFleetCatalog();
    }

    private void loadFleetCatalog() {
        busCatalog.add(new Bus(101, "TN-01-AB-1234", "THAMARAI BUS TRANSPORTS", "Bharat Benz A/C Sleeper (2+1)",
                "Koyambedu (Chennai)", "Coimbatore", "21:25", "06:45", 799.00, 4.9, 30, 18));
        busCatalog.add(new Bus(102, "TN-38-CD-5678", "KPN TRAVELS", "Volvo Multi-Axle Semi-Sleeper (2+2)",
                "Koyambedu (Chennai)", "Coimbatore", "20:15", "05:30", 699.00, 4.7, 40, 24));
        busCatalog.add(new Bus(103, "KA-01-EF-9012", "SRS TRAVELS", "Scania AC Multi Axle Sleeper (2+1)",
                "Koyambedu (Chennai)", "Coimbatore", "22:15", "07:00", 850.00, 4.6, 32, 14));
        busCatalog.add(new Bus(104, "AP-09-GH-3456", "ORANGE TOURS & TRAVELS", "Mercedes Benz Sleeper (2+1)",
                "Koyambedu (Chennai)", "Coimbatore", "23:00", "07:45", 920.00, 4.8, 28, 16));
    }

    @Override
    public List<Bus> searchBuses(String origin, String destination, String date) {
        Predicate<Bus> matchesRoute = bus ->
                bus.getOrigin().equalsIgnoreCase(origin.trim()) &&
                bus.getDestination().equalsIgnoreCase(destination.trim());

        return busCatalog.stream()
                .filter(matchesRoute)
                .collect(Collectors.toList());
    }

    @Override
    public List<Bus> getBusesSortedByPrice(List<Bus> buses) {
        return buses.stream()
                .sorted(Comparator.comparingDouble(Bus::getBaseFare))
                .collect(Collectors.toList());
    }

    @Override
    public List<Bus> getBusesSortedByRating(List<Bus> buses) {
        return buses.stream()
                .sorted((b1, b2) -> Double.compare(b2.getRating(), b1.getRating()))
                .collect(Collectors.toList());
    }

    @Override
    public Booking bookTicket(int busId, Passenger passenger, List<String> seatNumbers, String journeyDate)
            throws SeatAlreadyBookedException, InvalidBookingException {

        if (passenger == null || passenger.getFullName().trim().isEmpty()) {
            throw new InvalidBookingException("Passenger name cannot be blank.");
        }
        if (passenger.getAge() <= 0 || passenger.getAge() > 120) {
            throw new InvalidBookingException("Invalid passenger age: " + passenger.getAge());
        }
        if (seatNumbers == null || seatNumbers.isEmpty()) {
            throw new InvalidBookingException("At least one seat must be selected.");
        }

        String pnr = generatePnr(busId);
        Booking booking = new Booking(pnr, busId, passenger, journeyDate);

        for (String seatNo : seatNumbers) {
            Seat s = new Seat(0, busId, seatNo, seatNo.startsWith("L") ? Seat.Deck.LOWER : Seat.Deck.UPPER, 799.00, Seat.Status.BOOKED);
            booking.addSeat(s);
        }

        bookingRegistry.put(pnr, booking);
        System.out.println("[Service] Ticket booked successfully. PNR: " + pnr + ", Total: ₹" + booking.getTotalAmount());
        return booking;
    }

    @Override
    public boolean cancelBooking(String pnr) {
        if (bookingRegistry.containsKey(pnr)) {
            bookingRegistry.get(pnr).setBookingStatus("CANCELLED");
            System.out.println("[Service] Booking " + pnr + " has been cancelled.");
            return true;
        }
        return false;
    }
}
