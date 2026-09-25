# Claude.md — how this project is built and directed

This file records the architecture decisions, conventions, and constraints I hold the
implementation to. I own the design; AI is used heavily for implementation under that
direction. It is updated as each milestone lands, so it stays a real record rather than
a back-filled summary.

## What this is

A backend for a movie ticket booking system: cities/theaters host shows, users hold and
book seats at seat level, pay (mocked), and receive confirmation. The centerpiece is
**correct seat allocation under concurrency** — many users may race for the same seat,
and the system must let exactly one win.

## Stack decisions

- **Java 21 / Spring Boot 3.5.3, Maven.** Maven over Gradle for reviewer-readable,
  declarative build config and zero-surprise ubiquity. Wrapper (`./mvnw`) so no local
  Maven install is needed. Language level 21 for portability; builds fine on JDK 25.
- **MongoDB** as the datastore. Document model fits the aggregate shapes here
  (a theater with its seats, a booking with its seats). Multi-document ACID transactions
  are intentionally *not* relied on (local Mongo runs standalone), so correctness is
  designed to not require them — see the concurrency model.
- **No Redis.** It was considered for distributed seat holds, but since holds live in an
  in-process lock (single-instance monolith), Redis had no real job and was dropped
  rather than carried as an unused dependency.

## Concurrency model (the hard problem)

Defense in depth, two independent layers:

1. **In-process seat hold.** A singleton lock component holds seat locks keyed by
   **IDs** (`showId`, `seatId`) — never by object identity — with per-show critical
   sections so concurrent hold attempts on the same show serialize. Lock ownership is
   tracked by `userId`. Expiry is **lazy** (a lock is treated as free once its timeout
   passes, checked on read). This is correct for a single-instance deployment;
   distributed locking is explicitly out of scope for this exercise.
2. **Unique index as the last line of defense.** A unique compound index on
   `(showId, seatId)` over persisted booked seats makes a double-booking impossible to
   *commit*, even if the in-memory layer were bypassed or buggy. Because Mongo is
   standalone (no transactions), confirmation writes the per-seat uniqueness records
   first and compensates (deletes them) if a later step fails.

The booking flow is two-step: **hold → pay → confirm**. Holding reserves seats during
the payment window; confirmation is the point at which uniqueness is committed.

## Conventions

- **APIs never expose persistence documents.** Every endpoint takes a request DTO and
  returns a response DTO; mappers translate to/from documents.
- **Spring-idiomatic wiring** — constructor injection, singletons managed by the
  container. No manual object graphs.
- **Validation** via Bean Validation on request DTOs; a single global exception handler
  produces a consistent error response shape.
- **IDs** are strings (Mongo-native), not incrementing counters.

## Scope

In scope now: the core booking backbone — movies, theaters with screens and per-screen
seats, shows, seat availability, holds, booking, mocked payment, confirmation — with
MongoDB persistence, REST APIs, DTOs, validation, error handling, and tests (including a
concurrency test).

Deferred until the core is solid: pricing tiers, discount codes, refund policies,
notifications, and admin/customer RBAC.

Out of scope (per the exercise): UI, deployment/containerization/CI, microservices or
distributed systems, advanced auth, production observability, and real payment or
notification integrations (all external effects are mocked).

## Testing

- Tests run against a **local MongoDB** using a separate `moviebooking_test` database,
  so they never touch development data. (Containerization is out of scope for this
  exercise; a locally running Mongo is assumed.)
- The signature test is a **concurrency test**: N threads race for one seat; exactly one
  booking is confirmed, the rest fail cleanly, and the database ends with exactly one
  booked-seat record.

## Milestone log

- **M1 — Scaffold on Spring Boot + MongoDB.** Project skeleton, Mongo connection,
  actuator health, and a `/ping` liveness endpoint. Runs against a locally running
  MongoDB.
- **M2 — Domain model + repositories + indexes.** Documents (Movie, Theater embedding
  Screens which embed Seats, Show pointing at a movie/theater/screen, Booking,
  BookedSeat, User) and Spring Data repositories. The unique compound index
  `(showId, seatId)` on `booked_seats` lands here, with an integration test proving it
  rejects a duplicate seat on the same show while allowing the same seat number on
  different shows.
- **M3a — Web/error-handling foundation.** A single `ApiError` response shape and a
  `@RestControllerAdvice` global handler mapping not-found (404), request-body and
  parameter validation (400), and any unmapped exception (500, logged, internals
  hidden). Verified with a standalone MockMvc test against a throwaway controller, so no
  real endpoint ships in this milestone. Endpoints (M3b) build on this contract.
- **M3b — Admin/setup CRUD endpoints.** Service layer + REST endpoints under `/api` for
  movies, theaters (add screens, add seats), shows (create, get, available seats), and
  users — request/response DTOs only, never exposing documents. Adds a `ConflictException`
  (409) for duplicate screens/seats and duplicate user email (the latter also caught from
  Mongo's unique-index violation). Available seats currently = the screen's seats minus
  persisted booked seats; held seats are subtracted once the hold layer lands (M4).
  Verified with an end-to-end MockMvc test (create movie → theater → screen+seats → show →
  available seats) plus validation/not-found/conflict cases.
- **M3c — Customer browse/discovery.** The read path a customer walks before booking:
  search movies by partial name (`GET /api/movies?name=`), find theaters screening a
  movie (`GET /api/movies/{id}/theaters?city=`), and list showtimes filtered by movie
  and/or theater (`GET /api/shows?movieId=&theaterId=`). Complements the existing
  per-show available-seats and theater-layout endpoints. Verified with a browse-flow
  MockMvc test.
- **M4 — Seat holds + locking layer.** `SeatLockProvider` — a singleton holding
  `showId -> (seatId -> SeatLock)`: outer `ConcurrentHashMap` for atomic per-show table
  creation, every access to a show's table inside `synchronized(table)` so hold/release/
  validate serialize per show. Keyed by string ids, ownership by userId, lazy expiry (no
  sweeper). `POST /api/bookings` is step one of booking: validate show/user/seats,
  fast-fail if already booked, take an all-or-nothing hold, then persist a `CREATED`
  booking (releasing the hold if that save fails). Availability now also subtracts held
  seats; booking history via `GET /api/bookings?userId=`. Verified with a unit test of
  the provider (hold/conflict/release/expiry) and a hold-API integration test. The
  N-thread race test is deferred to the testing milestone.
- **M5 — Payment mock + confirmation + seat persistence.** A `PaymentStrategy` (mocked to
  approve) behind which `PaymentService` charges, then delegates to
  `BookingService.confirm`. Confirmation writes one `BookedSeat` per seat first (unique
  index = durable backstop; duplicate-key aborts and rolls the rows back since standalone
  Mongo has no transactions), then flips the booking to CONFIRMED (compensating on
  failure), then releases the holds. Guards: only the owner can pay (403), only a CREATED
  booking can be confirmed (409), a lapsed hold blocks confirmation (409), declined
  payment (402). `POST /api/bookings/{id}/payment`. Verified end to end (hold → pay →
  CONFIRMED, one booked-seat row per seat, seats stay unavailable) plus the guard cases.
- **M6 — Concurrency tests (the money test).** `ConcurrentBookingTest` releases N=20
  threads simultaneously (a ready/start/done latch harness): (1) all racing the full
  hold → pay flow for one seat — asserts exactly one CONFIRMED booking, N-1 clean
  failures, and exactly one persisted booked-seat row; (2) racing the persistence layer
  directly for the same `(showId, seatId)` — asserts exactly one insert wins and N-1 hit
  `DuplicateKeyException`, proving the unique index is the last line of defense on its
  own. Run repeatedly to confirm it is deterministic, not flaky. Full suite: 24 tests.
- **M7 — RBAC (admin + customer).** Basic role-based access control without heavyweight
  auth (advanced auth is out of scope). A `Role` (ADMIN/CUSTOMER) on `User`; the caller
  is identified by an `X-User-Id` header (the principal a real auth layer/gateway would
  supply — authentication itself is assumed done upstream). A `RoleAuthorizationInterceptor`
  resolves that user and enforces a `@RequiresRole` annotation per handler: admins manage
  the catalog, customers browse and book; `/ping`, actuator, and user creation are public
  for bootstrap. Booking and payment now take the acting user from the principal, not the
  request body, so a caller can't book as someone else. Unauthorized → 401, wrong role →
  403. Verified with a dedicated RBAC test plus header-aware updates to the existing web
  tests. Full suite: 29 tests.
- **M8 — README + assumptions.** Rewrote the README as a complete reference: overview,
  stack rationale, the concurrency model, the roles/`X-User-Id` access model, run/config
  instructions, the full API surface grouped by admin/customer with an end-to-end curl
  example, the documented assumptions list, testing, and project structure.
