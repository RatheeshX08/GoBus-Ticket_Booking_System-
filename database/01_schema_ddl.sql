-- ==============================================================
-- DATABASE CREATION & SCHEMA DEFINITION (DDL)
-- Project: Online Bus Ticket Booking System
-- Database: bus_booking_db
-- ==============================================================

CREATE DATABASE IF NOT EXISTS bus_booking_db;
USE bus_booking_db;

-- 1. Routes Table: Stores city pairs, distance, and estimated travel duration
CREATE TABLE IF NOT EXISTS routes (
    route_id INT AUTO_INCREMENT PRIMARY KEY,
    origin VARCHAR(100) NOT NULL,
    destination VARCHAR(100) NOT NULL,
    distance_km DECIMAL(6, 2) NOT NULL,
    estimated_hours DECIMAL(4, 2) NOT NULL,
    CONSTRAINT chk_different_cities CHECK (origin <> destination)
);

-- 2. Buses Table: Stores bus fleet information
CREATE TABLE IF NOT EXISTS buses (
    bus_id INT AUTO_INCREMENT PRIMARY KEY,
    vehicle_number VARCHAR(20) NOT NULL UNIQUE,
    operator_name VARCHAR(120) NOT NULL,
    bus_type VARCHAR(60) NOT NULL,             -- e.g. 'A/C Sleeper (2+1)'
    route_id INT NOT NULL,
    departure_time TIME NOT NULL,
    arrival_time TIME NOT NULL,
    base_fare DECIMAL(8, 2) NOT NULL,
    rating DECIMAL(2, 1) DEFAULT 4.5,
    total_capacity INT NOT NULL DEFAULT 30,
    available_seats INT NOT NULL DEFAULT 30,
    is_active BOOLEAN DEFAULT TRUE,
    CONSTRAINT fk_bus_route FOREIGN KEY (route_id) 
        REFERENCES routes(route_id) ON DELETE RESTRICT,
    CONSTRAINT chk_fare_positive CHECK (base_fare > 0),
    CONSTRAINT chk_rating_range CHECK (rating BETWEEN 1.0 AND 5.0)
);

-- 3. Seats Table: Represents lower and upper deck berths
CREATE TABLE IF NOT EXISTS seats (
    seat_id INT AUTO_INCREMENT PRIMARY KEY,
    bus_id INT NOT NULL,
    seat_number VARCHAR(10) NOT NULL,          -- e.g. 'L1', 'U1'
    deck ENUM('lower', 'upper') NOT NULL,
    price DECIMAL(8, 2) NOT NULL,
    status ENUM('available', 'sold', 'ladies') DEFAULT 'available',
    berth_type ENUM('sleeper', 'seater') DEFAULT 'sleeper',
    CONSTRAINT fk_seat_bus FOREIGN KEY (bus_id) 
        REFERENCES buses(bus_id) ON DELETE CASCADE,
    CONSTRAINT uq_bus_seat UNIQUE (bus_id, seat_number)
);

-- 4. Passengers Table: Stores traveler identity and contact
CREATE TABLE IF NOT EXISTS passengers (
    passenger_id INT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    age INT NOT NULL,
    gender ENUM('Male', 'Female', 'Other') NOT NULL,
    phone VARCHAR(15) NOT NULL,
    email VARCHAR(120) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_valid_age CHECK (age BETWEEN 1 AND 120)
);

-- 5. Users Table: Login accounts for passenger booking access
CREATE TABLE IF NOT EXISTS users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(120) NOT NULL UNIQUE,
    password_salt BINARY(16) NOT NULL,
    password_hash BINARY(32) NOT NULL,
    hash_iterations INT NOT NULL DEFAULT 180000,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 6. Bookings Table: Master reservation records
CREATE TABLE IF NOT EXISTS bookings (
    booking_id INT AUTO_INCREMENT PRIMARY KEY,
    pnr VARCHAR(30) NOT NULL UNIQUE,
    bus_id INT NOT NULL,
    passenger_id INT NOT NULL,
    journey_date DATE NOT NULL,
    total_amount DECIMAL(10, 2) NOT NULL,
    booking_status ENUM('CONFIRMED', 'CANCELLED') DEFAULT 'CONFIRMED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_booking_bus FOREIGN KEY (bus_id) 
        REFERENCES buses(bus_id),
    CONSTRAINT fk_booking_passenger FOREIGN KEY (passenger_id) 
        REFERENCES passengers(passenger_id)
);

-- 7. Booking Seats Mapping (Relational mapping for M:N relationship)
CREATE TABLE IF NOT EXISTS booking_seats (
    booking_id INT NOT NULL,
    seat_id INT NOT NULL,
    seat_fare DECIMAL(8, 2) NOT NULL,
    PRIMARY KEY (booking_id, seat_id),
    CONSTRAINT fk_bs_booking FOREIGN KEY (booking_id) 
        REFERENCES bookings(booking_id) ON DELETE CASCADE,
    CONSTRAINT fk_bs_seat FOREIGN KEY (seat_id) 
        REFERENCES seats(seat_id)
);
