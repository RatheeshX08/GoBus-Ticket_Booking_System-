package com.busbooking.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Booking entity holding PNR, associated Bus, Passenger, Seat list, and fare calculation.
 */
public class Booking {
    private String pnr;
    private int busId;
    private Passenger passenger;
    private List<Seat> bookedSeats;
    private String journeyDate;
    private double totalAmount;
    private String bookingStatus;   // "CONFIRMED", "CANCELLED"

    public Booking() {
        this.bookedSeats = new ArrayList<>();
    }

    public Booking(String pnr, int busId, Passenger passenger, String journeyDate) {
        this.pnr = pnr;
        this.busId = busId;
        this.passenger = passenger;
        this.journeyDate = journeyDate;
        this.bookedSeats = new ArrayList<>();
        this.bookingStatus = "CONFIRMED";
    }

    public void addSeat(Seat seat) {
        this.bookedSeats.add(seat);
        recalculateFare();
    }

    public void recalculateFare() {
        double subtotal = 0;
        for (Seat s : bookedSeats) {
            subtotal += s.getPrice();
        }
        double gst = subtotal * 0.05;
        this.totalAmount = subtotal + gst;
    }

    public String getPnr() { return pnr; }
    public void setPnr(String pnr) { this.pnr = pnr; }

    public int getBusId() { return busId; }
    public void setBusId(int busId) { this.busId = busId; }

    public Passenger getPassenger() { return passenger; }
    public void setPassenger(Passenger passenger) { this.passenger = passenger; }

    public List<Seat> getBookedSeats() { return bookedSeats; }
    public void setBookedSeats(List<Seat> bookedSeats) { 
        this.bookedSeats = bookedSeats;
        recalculateFare();
    }

    public String getJourneyDate() { return journeyDate; }
    public void setJourneyDate(String journeyDate) { this.journeyDate = journeyDate; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public String getBookingStatus() { return bookingStatus; }
    public void setBookingStatus(String bookingStatus) { this.bookingStatus = bookingStatus; }
}
