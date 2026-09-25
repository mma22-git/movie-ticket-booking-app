# movie-ticket-booking-app

Backend for a movie ticket booking system. Users can search shows, hold seats, and
book them atomically — no double-allocation even when many users race for the same
seat.

> Detailed design docs and the full assumptions list are added in a later phase.
> This README currently covers how to build and run the scaffold.

## Tech stack

| Concern        | Choice                          | Notes                                              |
|----------------|---------------------------------|----------------------------------------------------|
| Language       | Java 21                         | Compiled with JDK 25, targeting Java 21 bytecode.  |
| Framework      | Spring Boot 3.5.3               | Web, Data MongoDB, Validation, Actuator.           |
| Build          | Maven (via `./mvnw` wrapper)    | No local Maven install required.                   |
| Database       | MongoDB 7                       | Persists shows, bookings, and booked seats.        |

## Concurrency model (the core of this project)

Seat booking must be safe under concurrent load. The design uses defense in depth:

1. **In-process seat holds** — an in-memory lock (keyed by show + seat) serializes
   concurrent hold attempts within the application, so only one user can hold a seat
   at a time. Suited to a single-instance monolith (distributed locking is out of
   scope for this exercise).
2. **A unique index as the last line of defense** — a unique compound index on
   `(showId, seatId)` in MongoDB makes it impossible to *persist* a double-booking,
   even if the in-memory layer were bypassed.

## Prerequisites

- JDK 21+ (JDK 25 works; the build targets Java 21).
- A MongoDB instance reachable at `mongodb://localhost:27017` (override with `MONGODB_URI`).

## Running locally

1. Make sure MongoDB is running locally on port 27017.

2. Run the app:

   ```bash
   ./mvnw spring-boot:run
   ```

3. Verify it's up:

   ```bash
   curl http://localhost:8080/ping
   curl http://localhost:8080/actuator/health
   ```

## Configuration

Settings live in `src/main/resources/application.yml` and can be overridden by
environment variables:

| Variable       | Default                                       |
|----------------|-----------------------------------------------|
| `SERVER_PORT`  | `8080`                                        |
| `MONGODB_URI`  | `mongodb://localhost:27017/moviebooking`      |

## Tests

```bash
./mvnw test
```

Tests run against a local MongoDB using a separate `moviebooking_test` database, so
they never touch development data.
