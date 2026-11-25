package com.busbooking.model;

/**
 * Seat entity encapsulating lower and upper deck berth attributes.
 */
public class Seat {
    public enum Deck { LOWER, UPPER }
    public enum Status { AVAILABLE, BOOKED, SELECTED, LADIES }

    private int seatId;
    private int busId;
    private String seatNumber;  // e.g. "L1", "L2", "U1", "U2"
    private Deck deck;
    private double price;
    private Status status;

    public Seat() {
    }

    public Seat(int seatId, int busId, String seatNumber, Deck deck, double price, Status status) {
        this.seatId = seatId;
        this.busId = busId;
        this.seatNumber = seatNumber;
        this.deck = deck;
        this.price = price;
        this.status = status;
    }

    public int getSeatId() {
        return seatId;
    }

    public void setSeatId(int seatId) {
        this.seatId = seatId;
    }

    public int getBusId() {
        return busId;
    }

    public void setBusId(int busId) {
        this.busId = busId;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }

    public Deck getDeck() {
        return deck;
    }

    public void setDeck(Deck deck) {
        this.deck = deck;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public boolean isAvailable() {
        return this.status == Status.AVAILABLE;
    }

    @Override
    public String toString() {
        return seatNumber + " (" + deck + ", ₹" + price + ", " + status + ")";
    }
}
