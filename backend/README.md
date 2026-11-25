# Backend - Online Bus Ticket Booking System (Java)

This directory contains the Java backend source code structured under standard Maven packaging (`com.busbooking.*`).

## Package Structure
- `com.busbooking.model`:
  - `AbstractVehicle.java`: Abstract base class demonstrating OOP abstraction and inheritance.
  - `Bus.java`: Concrete bus class extending `AbstractVehicle`.
  - `Passenger.java`: Passenger entity with contact and demographic fields.
  - `Seat.java`: Seat model with Deck enum (`LOWER`, `UPPER`) and Status enum.
  - `Booking.java`: Master reservation record with seat collections and fare recalculation.
- `com.busbooking.dao`:
  - `DBConnection.java`: Singleton JDBC connection pool for MySQL.
  - `BusDAO.java`: SQL queries for bus search and seat matrices using `PreparedStatement`.
  - `BookingDAO.java`: ACID transaction management (`setAutoCommit(false)`, `commit()`, `rollback()`).
- `com.busbooking.service`:
  - `IBookingService.java`: Business logic interface.
  - `BusBookingService.java`: In-memory catalog filtering using Java Streams API & Lambda comparators.
- `com.busbooking.exception`:
  - `SeatAlreadyBookedException.java`: Checked exception for concurrency conflicts.
  - `InvalidBookingException.java`: Runtime exception for invalid user inputs.
- `com.busbooking.io`:
  - `TicketFileHandler.java`: Character Stream I/O using `FileWriter` and `PrintWriter`.
- `com.busbooking.Main`: Console demonstration entry point.

## Build and Run
```
# Install the SQL files once, from the project root:
mysql -u root -p < database/01_schema_ddl.sql
mysql -u root -p < database/02_sample_data.sql
mysql -u root -p < database/03_queries_and_views.sql
mysql -u root -p < database/04_stored_procedures_triggers.sql
```

For an existing database, run `mysql -u root -p < database/05_demo_login.sql`.
The seeded demo account is `demo@gobus.com` / `password123`.

```bash
# Start the API from this directory. Use your actual MySQL credentials:
DB_USER=root DB_PASSWORD='your-password' mvn clean compile exec:java \
  -Dexec.mainClass="com.busbooking.ApiServer"
```

The API listens on `http://localhost:8080`. `DB_URL` and `PORT` can also be set
to override the defaults. The browser frontend uses this API for account
registration/login, route searches, seat availability, booking creation,
booking history, and cancellation. Passwords are stored as salted
PBKDF2-HMAC-SHA256 hashes. The browser-only route guard is for demonstration
UX; booking endpoints do not yet enforce server-side user sessions and must
not be treated as production access control.

## API Endpoints
- `GET /api/health`: confirms the API can query MySQL.
- `POST /api/auth/register`: creates a user in MySQL with a salted password hash.
- `POST /api/auth/login`: validates a user's email and password against MySQL.
- `GET /api/buses?origin=...&destination=...`: reads buses and seats from MySQL.
- `GET /api/bookings`: reads booking history.
- `POST /api/bookings`: reserves the requested seats in a database transaction.
- `DELETE /api/bookings/{pnr}`: cancels a confirmed booking and releases its seats.

Serve `frontend/` separately (for example, `python3 -m http.server 3000` from
that directory). The API enables CORS for local frontend development.
