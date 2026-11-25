-- ==============================================================
-- PL/SQL: PROCEDURES, FUNCTIONS, TRIGGERS & TRANSACTIONS
-- Project: Online Bus Ticket Booking System
-- Database: bus_booking_db
-- ==============================================================

USE bus_booking_db;

DELIMITER $$

-- 1. Stored Function: Calculate Total Fare including GST (5%) and promo discount
CREATE FUNCTION fn_calculate_fare(
    p_base_fare DECIMAL(8, 2),
    p_seat_count INT,
    p_discount DECIMAL(8, 2)
)
RETURNS DECIMAL(10, 2)
DETERMINISTIC
BEGIN
    DECLARE v_subtotal DECIMAL(10, 2);
    DECLARE v_gst DECIMAL(10, 2);
    DECLARE v_total DECIMAL(10, 2);

    SET v_subtotal = (p_base_fare * p_seat_count) - p_discount;
    IF v_subtotal < 0 THEN
        SET v_subtotal = 0;
    END IF;

    SET v_gst = v_subtotal * 0.05; -- 5% GST
    SET v_total = v_subtotal + v_gst;

    RETURN v_total;
END$$

-- 2. Stored Procedure: Atomic Ticket Booking with ACID Transaction & Lock
CREATE PROCEDURE sp_book_bus_seat(
    IN p_bus_id INT,
    IN p_seat_number VARCHAR(10),
    IN p_passenger_id INT,
    IN p_journey_date DATE,
    IN p_pnr VARCHAR(30),
    OUT p_status_message VARCHAR(100)
)
BEGIN
    DECLARE v_seat_status VARCHAR(20);
    DECLARE v_seat_id INT;
    DECLARE v_seat_price DECIMAL(8, 2);
    DECLARE v_booking_id INT;

    -- Exception Handler: Rollback on any SQL Exception
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SET p_status_message = 'ERROR: Database transaction failed. Rolled back.';
    END;

    START TRANSACTION;

    -- Check if seat exists and is available with row-level lock
    SELECT seat_id, status, price INTO v_seat_id, v_seat_status, v_seat_price
    FROM seats 
    WHERE bus_id = p_bus_id AND seat_number = p_seat_number
    FOR UPDATE;

    IF v_seat_id IS NULL THEN
        SET p_status_message = 'ERROR: Seat does not exist.';
        ROLLBACK;
    ELSEIF v_seat_status <> 'available' THEN
        SET p_status_message = CONCAT('ERROR: Seat ', p_seat_number, ' is already occupied.');
        ROLLBACK;
    ELSE
        -- Update seat status
        UPDATE seats SET status = 'sold' WHERE seat_id = v_seat_id;

        -- Insert booking master record
        INSERT INTO bookings (pnr, bus_id, passenger_id, journey_date, total_amount, booking_status)
        VALUES (p_pnr, p_bus_id, p_passenger_id, p_journey_date, v_seat_price * 1.05, 'CONFIRMED');
        
        SET v_booking_id = LAST_INSERT_ID();

        -- Insert booking seat relationship
        INSERT INTO booking_seats (booking_id, seat_id, seat_fare)
        VALUES (v_booking_id, v_seat_id, v_seat_price);

        -- Decrement bus available seat count
        UPDATE buses SET available_seats = available_seats - 1 WHERE bus_id = p_bus_id;

        COMMIT;
        SET p_status_message = CONCAT('SUCCESS: Seat ', p_seat_number, ' booked with PNR ', p_pnr);
    END IF;

END$$

-- 3. Stored Procedure: Cancel Booking & Release Seats
CREATE PROCEDURE sp_cancel_booking(
    IN p_pnr VARCHAR(30),
    OUT p_status_message VARCHAR(100)
)
BEGIN
    DECLARE v_booking_id INT;
    DECLARE v_bus_id INT;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SET p_status_message = 'ERROR: Cancellation failed.';
    END;

    START TRANSACTION;

    SELECT booking_id, bus_id INTO v_booking_id, v_bus_id
    FROM bookings WHERE pnr = p_pnr AND booking_status = 'CONFIRMED';

    IF v_booking_id IS NULL THEN
        SET p_status_message = 'ERROR: Active booking not found.';
        ROLLBACK;
    ELSE
        -- Mark booking cancelled
        UPDATE bookings SET booking_status = 'CANCELLED' WHERE booking_id = v_booking_id;

        -- Release seats back to available
        UPDATE seats SET status = 'available' 
        WHERE seat_id IN (SELECT seat_id FROM booking_seats WHERE booking_id = v_booking_id);

        -- Update bus available seat count
        UPDATE buses SET available_seats = available_seats + (
            SELECT COUNT(*) FROM booking_seats WHERE booking_id = v_booking_id
        ) WHERE bus_id = v_bus_id;

        COMMIT;
        SET p_status_message = CONCAT('SUCCESS: Booking ', p_pnr, ' cancelled and seats released.');
    END IF;
END$$

-- 4. Trigger: Maintain bus seats upon booking
CREATE TRIGGER trg_after_booking_insert
AFTER INSERT ON booking_seats
FOR EACH ROW
BEGIN
    UPDATE seats SET status = 'sold' WHERE seat_id = NEW.seat_id;
END$$

DELIMITER ;
