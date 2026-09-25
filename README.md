# movie-ticket-booking-app

Backend for a movie ticket booking system. Customers browse shows, hold seats, and book
them atomically — no double-allocation even when many users race for the same seat.
Admins manage the catalog (movies, theaters, screens, shows).

The design centerpiece is **correct seat allocation under concurrency**: many users may
race for one seat, and the system lets exactly one win. See
[Concurrency model](#concurrency-model-the-core) below.

## Tech stack

| Concern     | Choice                        | Notes                                              |
|-------------|-------------------------------|----------------------------------------------------|
| Language    | Java 21                       | Compiled with JDK 25, targeting Java 21 bytecode.  |
| Framework   | Spring Boot 3.5.3             | Web, Data MongoDB, Validation, Actuator.           |
| Build       | Maven (via `./mvnw` wrapper)  | No local Maven install required.                   |
| Database    | MongoDB 7                     | Shows, bookings, and the booked-seat uniqueness index. |

**Why Maven:** reviewer-readable declarative build config, zero-surprise ubiquity, and a
committed wrapper so anyone can build with only a JDK installed.

**Why MongoDB:** the document model fits the aggregates here (a theater with its screens
and seats; a booking with its seats). Correctness is designed *not* to depend on
multi-document transactions (local Mongo runs standalone) — see the concurrency model.

## Concurrency model (the core)

Seat booking is safe under load through **defense in depth** — two independent layers:

1. **In-process seat holds.** A singleton `SeatLockProvider` holds locks as
   `showId -> (seatId -> lock)`. The outer map is a `ConcurrentHashMap` (atomic per-show
   table creation); every read/write of a show's table happens inside
   `synchronized(table)`, so all hold/release/validate operations for one show serialize
   on that monitor. Two users racing for the same seat cannot both pass the check.
   Ownership is by user id; expiry is **lazy** (a lapsed hold is ignored on the next read,
   freeing its seats with no background sweeper).
2. **A unique index as the last line of defense.** A unique compound index on
   `(showId, seatId)` over the `booked_seats` collection makes a double-booking impossible
   to *persist*. On confirmation the seat rows are written **first**; because standalone
   Mongo has no multi-document transactions, a duplicate-key error aborts the confirmation
   and any rows already written are compensated (deleted).

The booking flow is two-step: **hold → pay → confirm**. Holding reserves seats during the
payment window; confirmation is where uniqueness is committed.

> Single-instance scope: the in-memory hold serializes within one JVM, which is correct
> for this monolith (distributed systems are out of scope). If scaled horizontally, the
> hold would move to a shared store (e.g. Redis) — but the unique index remains the
> durable guarantee regardless.

## Roles & access

Basic role-based access control with two roles: **ADMIN** and **CUSTOMER**.

Authentication itself is out of scope (no OAuth/SSO/MFA). The caller's already-authenticated
identity is passed as an **`X-User-Id`** header — the principal a real auth layer or
gateway would supply. An interceptor resolves that user, and each endpoint declares the
role it needs:

- **ADMIN** — create/manage movies, theaters, screens, seats, shows.
- **CUSTOMER** — browse, hold, pay, and view their own bookings.
- **Public** (no principal) — `POST /api/users` (bootstrap), `GET /ping`, `/actuator/**`.

Missing/unknown principal → `401`; authenticated but wrong role → `403`.

## Prerequisites

- JDK 21+ (JDK 25 works; the build targets Java 21).
- A MongoDB instance reachable at `mongodb://localhost:27017` (override with `MONGODB_URI`).

## Running locally

```bash
# 1. Ensure MongoDB is running locally on port 27017
# 2. Start the app
./mvnw spring-boot:run
# 3. Verify
curl http://localhost:8080/ping
curl http://localhost:8080/actuator/health
```

## Configuration

Settings live in `src/main/resources/application.yml`, overridable by environment variables:

| Variable                | Default                                    | Meaning                          |
|-------------------------|--------------------------------------------|----------------------------------|
| `SERVER_PORT`           | `8080`                                     | HTTP port                        |
| `MONGODB_URI`           | `mongodb://localhost:27017/moviebooking`   | Mongo connection string          |
| `HOLD_TIMEOUT_SECONDS`  | `120`                                      | How long a seat hold survives    |

## API reference

All under `/api`. Requests/responses are DTOs — persistence documents are never exposed.

### Bootstrap & users (public / authenticated)
| Method & path        | Role      | Purpose                                   |
|----------------------|-----------|-------------------------------------------|
| `POST /api/users`    | public    | Create a user; `role` optional (default CUSTOMER). |
| `GET /api/users/{id}`| any auth  | Fetch a user.                             |

### Admin — catalog management (ADMIN)
| Method & path                                        | Purpose                    |
|------------------------------------------------------|----------------------------|
| `POST /api/movies`                                   | Create a movie.            |
| `POST /api/theaters`                                 | Create a theater.          |
| `POST /api/theaters/{id}/screens`                    | Add a screen (with seats). |
| `POST /api/theaters/{id}/screens/{screenId}/seats`   | Add seats to a screen.     |
| `POST /api/shows`                                    | Create a show.             |

### Customer — browse (authenticated)
| Method & path                                  | Purpose                                  |
|------------------------------------------------|------------------------------------------|
| `GET /api/movies?name=`                        | List/search movies by partial name.      |
| `GET /api/movies/{id}`                          | Get a movie.                             |
| `GET /api/movies/{id}/theaters?city=`           | Theaters screening a movie (optionally by city). |
| `GET /api/theaters/{id}`                        | Theater layout (screens + seats).        |
| `GET /api/shows?movieId=&theaterId=`            | List showtimes (filterable).             |
| `GET /api/shows/{id}`                           | Get a show.                              |
| `GET /api/shows/{id}/available-seats`           | Seats still bookable for a show.         |

### Customer — booking (CUSTOMER)
| Method & path                              | Purpose                                          |
|--------------------------------------------|--------------------------------------------------|
| `POST /api/bookings`                       | Step 1 — hold seats (creates a CREATED booking). |
| `POST /api/bookings/{id}/payment`          | Step 2 — pay (mocked) and confirm.               |
| `GET /api/bookings`                        | The caller's booking history.                    |
| `GET /api/bookings/{id}`                   | Get one of the caller's bookings.                |

### End-to-end example

```bash
BASE=http://localhost:8080; H='Content-Type: application/json'

# Bootstrap an admin and a customer
ADMIN=$(curl -s -X POST $BASE/api/users -H "$H" -d '{"name":"Admin","email":"admin@x.com","role":"ADMIN"}' | jq -r .id)
CUST=$(curl -s -X POST $BASE/api/users -H "$H" -d '{"name":"Meet","email":"meet@x.com"}' | jq -r .id)

# Admin sets up catalog
MOVIE=$(curl -s -X POST $BASE/api/movies -H "$H" -H "X-User-Id: $ADMIN" -d '{"name":"Oppenheimer","durationMinutes":180}' | jq -r .id)
THEATER=$(curl -s -X POST $BASE/api/theaters -H "$H" -H "X-User-Id: $ADMIN" -d '{"name":"PVR","city":"Pune"}' | jq -r .id)
SCREEN=$(curl -s -X POST $BASE/api/theaters/$THEATER/screens -H "$H" -H "X-User-Id: $ADMIN" -d '{"name":"Audi 1","seats":[{"number":"A1","category":"GOLD"}]}' | jq -r .id)
SHOW=$(curl -s -X POST $BASE/api/shows -H "$H" -H "X-User-Id: $ADMIN" -d "{\"movieId\":\"$MOVIE\",\"theaterId\":\"$THEATER\",\"screenId\":\"$SCREEN\",\"startTime\":\"2030-09-01T18:00:00Z\",\"durationMinutes\":180}" | jq -r .id)

# Customer holds then pays
BOOKING=$(curl -s -X POST $BASE/api/bookings -H "$H" -H "X-User-Id: $CUST" -d "{\"showId\":\"$SHOW\",\"seatIds\":[\"A1\"]}" | jq -r .id)
curl -s -X POST $BASE/api/bookings/$BOOKING/payment -H "$H" -H "X-User-Id: $CUST" -d '{"paymentMethod":"MOCK"}'
```

## Assumptions

- **Authentication is out of scope**; the `X-User-Id` header stands in for an
  already-authenticated principal that an upstream gateway would provide. `POST /api/users`
  is public so the first admin/customer can be created; in production, user provisioning
  and role assignment would be handled by an identity system.
- **Single application instance.** The in-memory seat hold is correct for one JVM;
  horizontal scaling is out of scope (would need a distributed lock). The `(showId, seatId)`
  unique index guarantees no double-booking is persisted regardless.
- **MongoDB runs standalone** (no replica set), so no multi-document transactions are used.
  Confirmation writes seat rows first and compensates on failure instead.
- **Payment is mocked** — a `PaymentStrategy` that always approves; no real gateway is called.
- **One implicit "screen" abstraction:** a theater has one or more screens, each owning its
  seat layout; a show plays on exactly one screen. Seat numbers are unique within a screen.
- **Holds expire lazily** after `HOLD_TIMEOUT_SECONDS`; an abandoned hold frees its seats on
  the next read rather than via a scheduled sweep.
- **Seat identity** is the human seat number (e.g. `A1`), unique within a screen, and thus
  within any show on that screen.
- **Deferred (not built):** pricing tiers, discount codes, refunds/cancellation, and
  notifications. These were scoped out to keep the focus on the concurrency centerpiece.

## Testing

```bash
./mvnw test
```

Tests run against a local MongoDB using a separate `moviebooking_test` database, so they
never touch development data.

The signature test is the **concurrency ("money") test**
(`ConcurrentBookingTest`): N threads, released simultaneously, race for one seat —
exactly one booking ends `CONFIRMED`, the rest fail cleanly, and exactly one `booked_seat`
row is persisted. A companion test races the persistence layer directly to show the unique
index alone prevents a double-booking even if the in-memory hold were bypassed.

## Project structure

```
com.moviebooking
├── domain        Mongo documents + enums (Movie, Theater→Screen→Seat, Show, Booking, BookedSeat, User, Role)
├── repository    Spring Data Mongo repositories
├── service       Business logic — booking/hold/confirm, payment, catalog, seat lock provider
│   └── payment   PaymentStrategy + mock implementation
├── security      X-User-Id principal resolution, @RequiresRole, authorization interceptor
├── web           REST controllers + DTOs (+ global error handling)
└── exception     Domain exceptions mapped to HTTP status codes
```
