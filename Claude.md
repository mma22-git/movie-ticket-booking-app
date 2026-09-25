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
