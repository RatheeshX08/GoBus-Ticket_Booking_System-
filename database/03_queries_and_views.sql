-- ==============================================================
-- QUERIES, VIEWS, AGGREGATES & NESTED SUBQUERIES
-- Project: Online Bus Ticket Booking System
-- Database: bus_booking_db
-- ==============================================================

USE bus_booking_db;

-- 1. Joined Relations: Search buses with route details
SELECT 
    b.bus_id,
    b.operator_name,
    b.bus_type,
    r.origin,
    r.destination,
    b.departure_time,
    b.arrival_time,
    b.base_fare,
    b.rating,
    b.available_seats
FROM buses b
INNER JOIN routes r ON b.route_id = r.route_id
WHERE r.origin = 'Koyambedu (Chennai)' AND r.destination = 'Coimbatore'
ORDER BY b.base_fare ASC;

-- 2. Aggregate Functions: Fleet Statistics
SELECT 
    COUNT(b.bus_id) AS total_buses,
    MIN(b.base_fare) AS lowest_fare,
    MAX(b.base_fare) AS highest_fare,
    AVG(b.rating) AS average_rating,
    SUM(b.available_seats) AS total_seats_left
FROM buses b;

-- 3. Nested Subquery: Find buses whose fare is lower than the average fare
SELECT bus_id, operator_name, base_fare, rating
FROM buses
WHERE base_fare < (
    SELECT AVG(base_fare) FROM buses
);

-- 4. Subquery with IN: Find passengers who booked on Bharat Benz buses
SELECT full_name, phone, email 
FROM passengers 
WHERE passenger_id IN (
    SELECT passenger_id FROM bookings 
    WHERE bus_id IN (
        SELECT bus_id FROM buses WHERE bus_type LIKE '%Bharat Benz%'
    )
);

-- 5. CREATE VIEW: Real-time Bus Seat Availability View
CREATE OR REPLACE VIEW v_bus_seat_availability AS
SELECT 
    b.bus_id,
    b.operator_name,
    b.bus_type,
    r.origin,
    r.destination,
    b.base_fare,
    COUNT(CASE WHEN s.status = 'available' THEN 1 END) AS realtime_available_seats,
    COUNT(CASE WHEN s.status = 'sold' THEN 1 END) AS booked_seats,
    COUNT(s.seat_id) AS total_configured_seats
FROM buses b
JOIN routes r ON b.route_id = r.route_id
LEFT JOIN seats s ON b.bus_id = s.bus_id
GROUP BY b.bus_id, b.operator_name, b.bus_type, r.origin, r.destination, b.base_fare;

-- Querying the view:
-- SELECT * FROM v_bus_seat_availability WHERE realtime_available_seats > 0;
