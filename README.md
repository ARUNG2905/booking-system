# Global Class Offering Booking System

A production-ready backend service for a live-learning platform where teachers create course offerings with sessions, and parents/students can book them — with full timezone handling and concurrent booking safety.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.2 |
| Security | Spring Security + JWT (jjwt 0.12) |
| Persistence | Spring Data JPA + Hibernate |
| Database | PostgreSQL 15 |
| Migrations | Flyway |
| API Docs | SpringDoc OpenAPI 3 (Swagger UI) |
| Build | Maven |
| Containerization | Docker + Docker Compose |
| Testing | JUnit 5 + Mockito + Testcontainers |

---

## Project Structure

```
src/main/java/com/undoschool/booking/
├── config/          SecurityConfig, OpenApiConfig
├── controller/      AuthController, TeacherController, ParentController
├── service/         AuthService, OfferingService, BookingService
├── repository/      UserRepository, CourseRepository, OfferingRepository,
│                    SessionRepository, BookingRepository
├── entity/          User, Course, Offering, Session, Booking
├── dto/
│   ├── request/     AuthRequests, TeacherRequests
│   └── response/    Responses (all response DTOs)
├── exception/       BookingExceptions, GlobalExceptionHandler
├── security/        JwtUtil, JwtAuthFilter
└── util/            TimezoneConverter
```

---

## Database Schema

```
users          — id, name, email, password, role (TEACHER|PARENT), timezone, created_at
courses        — id, title, description, teacher_id, created_at
offerings      — id, course_id, teacher_id, title, description, status (ACTIVE|CLOSED), created_at
sessions       — id, offering_id, teacher_id, start_time (UTC), end_time (UTC), created_at
bookings       — id, parent_id, offering_id, status (CONFIRMED|CANCELLED), booked_at
```

**Key design decisions:**
- All timestamps are stored as `TIMESTAMPTZ` in UTC
- Timezone conversion is done at the application layer, never at the database layer
- `bookings` has a `UNIQUE(parent_id, offering_id)` constraint as a database-level safety net

---

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5432` | PostgreSQL port |
| `DB_NAME` | `booking_db` | Database name |
| `DB_USER` | `postgres` | Database user |
| `DB_PASSWORD` | `postgres` | Database password |
| `JWT_SECRET` | (see config) | Base64-encoded HMAC-SHA256 key |
| `JWT_EXPIRATION_MS` | `86400000` | Token TTL in ms (default: 24h) |
| `PORT` | `8080` | Application port |

---

## Running Locally

### Option 1 — Docker Compose (recommended)

```bash
git clone <repo-url>
cd booking-system
docker-compose up --build
```

App runs at: http://localhost:8080  
Swagger UI: http://localhost:8080/swagger-ui.html

### Option 2 — Run without Docker

**Prerequisites:** Java 17, Maven 3.8+, PostgreSQL 15 running locally

```bash
# Create database
psql -U postgres -c "CREATE DATABASE booking_db;"

# Set env vars (or export them)
export DB_HOST=localhost
export DB_USER=postgres
export DB_PASSWORD=postgres

# Build and run
mvn clean package -DskipTests
java -jar target/booking-1.0.0.jar
```

---

## API Documentation

Swagger UI is available at `/swagger-ui.html` once the app is running.

### Auth endpoints (public)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/auth/register` | Register as TEACHER or PARENT |
| POST | `/api/v1/auth/login` | Login → receive JWT |

**Register request example:**
```json
{
  "name": "Alice Kumar",
  "email": "alice@example.com",
  "password": "secret123",
  "role": "TEACHER",
  "timezone": "Asia/Kolkata"
}
```

### Teacher endpoints (JWT required, role: TEACHER)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/teachers/courses` | Create a course |
| GET | `/api/v1/teachers/courses` | List my courses |
| POST | `/api/v1/teachers/offerings` | Create an offering |
| GET | `/api/v1/teachers/offerings` | List my offerings with sessions |
| GET | `/api/v1/teachers/offerings/{id}` | Get specific offering |
| POST | `/api/v1/teachers/offerings/{id}/sessions` | Add sessions (batch) |

**Add sessions request example:**
```json
{
  "sessions": [
    { "startTime": "2025-06-07T18:00:00", "endTime": "2025-06-07T19:00:00" },
    { "startTime": "2025-06-14T18:00:00", "endTime": "2025-06-14T19:00:00" },
    { "startTime": "2025-06-21T18:00:00", "endTime": "2025-06-21T19:00:00" }
  ]
}
```
> Times are in the teacher's stored timezone. They are converted to UTC before storage.

### Parent endpoints (JWT required, role: PARENT)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/parents/offerings` | Browse available offerings |
| GET | `/api/v1/parents/offerings/{id}` | View offering details |
| POST | `/api/v1/parents/bookings/{offeringId}` | Book an offering |
| GET | `/api/v1/parents/bookings` | View my bookings |
| DELETE | `/api/v1/parents/bookings/{bookingId}` | Cancel a booking |

> All session times in parent responses are displayed in the parent's stored timezone.

---

## Timezone Handling

**Design principle: UTC in, convert on read.**

1. Teacher creates sessions by providing `LocalDateTime` values (no timezone suffix)
2. The service reads the teacher's stored IANA timezone (e.g. `America/New_York`) from their user profile
3. Converts using: `ZonedDateTime.of(localDateTime, ZoneId.of(teacherTimezone)).toInstant()`
4. Stores the UTC `Instant` in PostgreSQL `TIMESTAMPTZ`

When a parent fetches offerings:
1. The service reads the parent's stored IANA timezone (e.g. `Asia/Kolkata`)
2. Converts each session's UTC `Instant` to a local display string: `utcInstant.atZone(ZoneId.of(parentTimezone))`
3. Response includes both `startTimeUtc` (ISO-8601) and `startTimeLocal` (human-readable in parent's zone)

**Example:**
- Teacher in `America/New_York` creates a session at `2025-06-07T18:00:00` → stored as `2025-06-07T22:00:00Z`
- Parent in `Asia/Kolkata` sees: `2025-06-08 03:30:00 IST`

---

## Concurrency Handling

Three concurrent scenarios are handled:

### 1. Multiple parents booking the same offering simultaneously
- `BookingService.bookOffering()` runs under `@Transactional(isolation = REPEATABLE_READ)`
- Uses `SELECT FOR UPDATE` via `OfferingRepository.findByIdForUpdate()` (JPA `PESSIMISTIC_WRITE` lock)
- The offering row is locked for the duration of the transaction — concurrent requests queue up
- Each request evaluates conflict detection atomically within the same lock

### 2. Same parent sending duplicate booking requests (double-tap / race condition)
- The DB-level `UNIQUE(parent_id, offering_id)` constraint catches this even if the application check races
- `existsByParentIdAndOfferingId()` check is also done inside the locked transaction
- If both slip through to the INSERT, PostgreSQL throws `DataIntegrityViolationException` → 409

### 3. Parent booking two conflicting offerings simultaneously
- Conflict detection uses a JPQL set-intersection query (see `SessionRepository.countConflictingSessions`)
- The query runs inside the `PESSIMISTIC_WRITE`-locked transaction, making the check-then-act atomic
- Interval overlap condition: `a.start < b.end AND a.end > b.start`

---

## Running Tests

```bash
mvn test
```

Unit tests cover:
- Booking success and all rejection cases (conflict, duplicate, closed, not found)
- Timezone conversion accuracy (IST ↔ UTC, EST ↔ UTC)
- IANA timezone validation

---

## Assumptions

1. Authentication is email+password; no OAuth for this scope
2. A parent's timezone is set at registration and used for all display — no per-request override
3. Teachers can only manage their own courses and offerings
4. Booking is at the offering level — all sessions are booked together
5. Cancelled bookings free up the parent's time slots (conflict check excludes CANCELLED bookings)
6. No seat/capacity limits per offering (can be added in Phase 2)
7. Session times submitted by teachers are in their registered timezone (no override per session)
