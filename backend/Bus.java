package com.busbooking.model;

/**
 * Concrete Bus model inheriting from AbstractVehicle.
 */
public class Bus extends AbstractVehicle {
    private int busId;
    private String busType;          // e.g. Bharat Benz A/C Sleeper (2+1)
    private String origin;           // e.g. Koyambedu (Chennai)
    private String destination;      // e.g. Coimbatore
    private String departureTime;    // e.g. 21:25
    private String arrivalTime;      // e.g. 06:45
    private double baseFare;         // e.g. 799.00
    private double rating;           // e.g. 4.9
    private int availableSeats;      // e.g. 18

    public Bus() {
        super();
    }

    public Bus(int busId, String vehicleNumber, String operatorName, String busType,
               String origin, String destination, String departureTime, String arrivalTime,
               double baseFare, double rating, int totalCapacity, int availableSeats) {
        super(vehicleNumber, operatorName, totalCapacity);
        this.busId = busId;
        this.busType = busType;
        this.origin = origin;
        this.destination = destination;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.baseFare = baseFare;
        this.rating = rating;
        this.availableSeats = availableSeats;
    }

    @Override
    public String getVehicleCategory() {
        return "Commercial Passenger Bus - " + busType;
    }

    public int getBusId() {
        return busId;
    }

    public void setBusId(int busId) {
        this.busId = busId;
    }

    public String getBusType() {
        return busType;
    }

    public void setBusType(String busType) {
        this.busType = busType;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(String departureTime) {
        this.departureTime = departureTime;
    }

    public String getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(String arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public double getBaseFare() {
        return baseFare;
    }

    public void setBaseFare(double baseFare) {
        this.baseFare = baseFare;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public int getAvailableSeats() {
        return availableSeats;
    }

    public void setAvailableSeats(int availableSeats) {
        this.availableSeats = availableSeats;
    }

    @Override
    public String toString() {
        return String.format("Bus #%d [%s - %s] %s -> %s (Dep: %s, ₹%.2f, ⭐%.1f)",
                busId, operatorName, busType, origin, destination, departureTime, baseFare, rating);
    }
}
