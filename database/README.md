# Database - Online Bus Ticket Booking System

This folder contains the complete relational MySQL database schemas, sample records, views, and PL/SQL stored procedures.

## Files Overview
1. **`01_schema_ddl.sql`**: Data Definition Language (DDL) defining `routes`, `buses`, `seats`, `passengers`, `users`, `bookings`, and `booking_seats` with primary keys, foreign keys (`ON DELETE CASCADE`), unique constraints, and `CHECK` clauses.
2. **`02_sample_data.sql`**: Realistic seed records for routes, bus operators (Bharat Benz, Volvo, Scania), seat berth matrices (Lower & Upper decks), initial passengers, and a demo login account (`demo@gobus.com` / `password123`). The demo password is stored as a salted PBKDF2-HMAC-SHA256 hash.
3. **`03_queries_and_views.sql`**: JOIN relations, subqueries, aggregate metrics, and the real-time seat availability view `v_bus_seat_availability`.
4. **`04_stored_procedures_triggers.sql`**:
   - `sp_book_bus_seat`: Transactional seat reservation with row-level locking (`FOR UPDATE`) to prevent race conditions.
   - `sp_cancel_booking`: Atomic booking cancellation and seat release.
   - `fn_calculate_fare`: Fare and GST calculation function.
   - `trg_after_booking_insert`: Automatic seat status synchronization trigger.
5. **`05_demo_login.sql`**: Adds the users table and demo login to an existing database.

## How to Run in MySQL
```bash
mysql -u root -p < 01_schema_ddl.sql
mysql -u root -p < 02_sample_data.sql
mysql -u root -p < 03_queries_and_views.sql
mysql -u root -p < 04_stored_procedures_triggers.sql
```

For an existing database, run only `05_demo_login.sql` to add or reset the demo
account. Credentials: `demo@gobus.com` / `password123`.
