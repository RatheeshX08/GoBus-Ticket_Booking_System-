-- ==============================================================
-- SAMPLE DATA INSERTION (DML)
-- Project: Online Bus Ticket Booking System
-- Database: bus_booking_db
-- ==============================================================

USE bus_booking_db;

-- 1. Insert Routes
INSERT INTO routes (origin, destination, distance_km, estimated_hours) VALUES
('Koyambedu (Chennai)', 'Coimbatore', 505.00, 9.25),
('Koyambedu (Chennai)', 'Madurai', 460.00, 8.50),
('Koyambedu (Chennai)', 'Bangalore', 350.00, 6.75),
('Coimbatore', 'Bangalore', 360.00, 7.00),
('Madurai', 'Kochi', 270.00, 6.50),
('Hyderabad', 'Koyambedu (Chennai)', 625.00, 11.00);

-- 2. Insert Buses
INSERT INTO buses (vehicle_number, operator_name, bus_type, route_id, departure_time, arrival_time, base_fare, rating, total_capacity, available_seats) VALUES
('TN-01-AB-1234', 'THAMARAI BUS TRANSPORTS', 'Bharat Benz A/C Sleeper (2+1)', 1, '21:25:00', '06:45:00', 799.00, 4.9, 24, 18),
('TN-38-CD-5678', 'KPN TRAVELS', 'Volvo Multi-Axle Semi-Sleeper (2+2)', 1, '20:15:00', '05:30:00', 699.00, 4.7, 36, 24),
('KA-01-EF-9012', 'SRS TRAVELS', 'Scania AC Multi Axle Sleeper (2+1)', 1, '22:15:00', '07:00:00', 850.00, 4.6, 24, 14),
('AP-09-GH-3456', 'ORANGE TOURS & TRAVELS', 'Mercedes Benz Sleeper (2+1)', 1, '23:00:00', '07:45:00', 920.00, 4.8, 24, 16);

-- 3. Insert Lower Deck Berths for Bus ID = 1 (THAMARAI BUS TRANSPORTS)
INSERT INTO seats (bus_id, seat_number, deck, price, status, berth_type) VALUES
(1, 'L1', 'lower', 1289.00, 'available', 'sleeper'),
(1, 'L2', 'lower', 889.00,  'available', 'sleeper'),
(1, 'L3', 'lower', 889.00,  'available', 'sleeper'),
(1, 'L4', 'lower', 1289.00, 'available', 'sleeper'),
(1, 'L5', 'lower', 889.00,  'sold',      'sleeper'),
(1, 'L6', 'lower', 889.00,  'sold',      'sleeper'),
(1, 'L7', 'lower', 1289.00, 'sold',      'sleeper'),
(1, 'L8', 'lower', 889.00,  'sold',      'sleeper'),
(1, 'L9', 'lower', 889.00,  'sold',      'sleeper'),
(1, 'L10', 'lower', 1289.00, 'sold',     'sleeper'),
(1, 'L11', 'lower', 889.00,  'available', 'sleeper'),
(1, 'L12', 'lower', 889.00,  'sold',      'sleeper');

-- 4. Insert Upper Deck Berths for Bus ID = 1
INSERT INTO seats (bus_id, seat_number, deck, price, status, berth_type) VALUES
(1, 'U1', 'upper', 1089.00, 'available', 'sleeper'),
(1, 'U2', 'upper', 799.00,  'available', 'sleeper'),
(1, 'U3', 'upper', 799.00,  'available', 'sleeper'),
(1, 'U4', 'upper', 1089.00, 'sold',      'sleeper'),
(1, 'U5', 'upper', 799.00,  'available', 'sleeper'),
(1, 'U6', 'upper', 799.00,  'available', 'sleeper'),
(1, 'U7', 'upper', 1089.00, 'available', 'sleeper'),
(1, 'U8', 'upper', 799.00,  'available', 'sleeper'),
(1, 'U9', 'upper', 799.00,  'available', 'sleeper'),
(1, 'U10', 'upper', 1089.00, 'sold',     'sleeper'),
(1, 'U11', 'upper', 799.00,  'available', 'sleeper'),
(1, 'U12', 'upper', 799.00,  'sold',      'sleeper');

-- 5. Add a complete lower/upper seat map for the remaining sample buses
INSERT INTO seats (bus_id, seat_number, deck, price, status, berth_type)
SELECT b.bus_id,
       IF(n.seat_no <= FLOOR(b.total_capacity / 2),
          CONCAT('L', n.seat_no),
          CONCAT('U', n.seat_no - FLOOR(b.total_capacity / 2))),
       IF(n.seat_no <= FLOOR(b.total_capacity / 2), 'lower', 'upper'),
       b.base_fare + IF(MOD(n.seat_no, 3) = 1, 190.00, 90.00),
       IF(n.seat_no <= b.available_seats, 'available', 'sold'),
       'sleeper'
FROM buses b
CROSS JOIN (
    SELECT 1 AS seat_no UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8
    UNION ALL SELECT 9 UNION ALL SELECT 10 UNION ALL SELECT 11 UNION ALL SELECT 12
    UNION ALL SELECT 13 UNION ALL SELECT 14 UNION ALL SELECT 15 UNION ALL SELECT 16
    UNION ALL SELECT 17 UNION ALL SELECT 18 UNION ALL SELECT 19 UNION ALL SELECT 20
    UNION ALL SELECT 21 UNION ALL SELECT 22 UNION ALL SELECT 23 UNION ALL SELECT 24
    UNION ALL SELECT 25 UNION ALL SELECT 26 UNION ALL SELECT 27 UNION ALL SELECT 28
    UNION ALL SELECT 29 UNION ALL SELECT 30 UNION ALL SELECT 31 UNION ALL SELECT 32
    UNION ALL SELECT 33 UNION ALL SELECT 34 UNION ALL SELECT 35 UNION ALL SELECT 36
) n
WHERE b.bus_id IN (2, 3, 4) AND n.seat_no <= b.total_capacity;

UPDATE buses b
SET available_seats = (
    SELECT COUNT(*) FROM seats s WHERE s.bus_id = b.bus_id AND s.status = 'available'
);

-- 6. Seed a demo login account. Password: password123
-- The salt and PBKDF2-HMAC-SHA256 hash use 180,000 iterations.
INSERT INTO users (full_name, email, password_salt, password_hash, hash_iterations) VALUES
('GoBus Demo', 'demo@gobus.com',
 UNHEX('476f4275732d64656d6f2d73616c7421'),
 UNHEX('af318595a0c42a0843d75d3580218e89dd0e784ed443cebd7413eef70cef7778'),
 180000)
ON DUPLICATE KEY UPDATE
    full_name = VALUES(full_name),
    password_salt = VALUES(password_salt),
    password_hash = VALUES(password_hash),
    hash_iterations = VALUES(hash_iterations);

-- 7. Insert Sample Passengers
INSERT INTO passengers (full_name, age, gender, phone, email) VALUES
('Ratheesh Kumar', 24, 'Male', '9876543210', 'ratheesh@example.com'),
('Priya Sundaram', 22, 'Female', '9845012345', 'priya.s@example.com'),
('Karthik Rajan', 31, 'Male', '9712345678', 'karthik.r@example.com');

-- 8. Insert Sample Booking
INSERT INTO bookings (pnr, bus_id, passenger_id, journey_date, total_amount, booking_status) VALUES
('RV-102-8392', 1, 1, '2026-10-25', 1762.00, 'CONFIRMED');

INSERT INTO booking_seats (booking_id, seat_id, seat_fare) VALUES
(1, 5, 889.00),
(1, 6, 889.00);
