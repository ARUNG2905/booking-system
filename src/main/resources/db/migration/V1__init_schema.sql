-- V1__init_schema.sql
-- All timestamps stored as UTC (TIMESTAMPTZ)
-- Timezone conversions happen at the application layer

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ─────────────────────────────────────────────
-- USERS
-- ─────────────────────────────────────────────
CREATE TABLE users (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL,
    email       VARCHAR(255) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    role        VARCHAR(20)  NOT NULL CHECK (role IN ('TEACHER', 'PARENT')),
    timezone    VARCHAR(100) NOT NULL DEFAULT 'UTC',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role  ON users(role);

-- ─────────────────────────────────────────────
-- COURSES
-- ─────────────────────────────────────────────
CREATE TABLE courses (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title       VARCHAR(255) NOT NULL,
    description TEXT,
    teacher_id  UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_courses_teacher ON courses(teacher_id);

-- ─────────────────────────────────────────────
-- OFFERINGS
-- ─────────────────────────────────────────────
CREATE TABLE offerings (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id   UUID         NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    teacher_id  UUID         NOT NULL REFERENCES users(id),
    title       VARCHAR(255) NOT NULL,
    description TEXT,
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'CLOSED')),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_offerings_course    ON offerings(course_id);
CREATE INDEX idx_offerings_teacher   ON offerings(teacher_id);
CREATE INDEX idx_offerings_status    ON offerings(status);

-- ─────────────────────────────────────────────
-- SESSIONS
-- Each session stores times in UTC
-- ─────────────────────────────────────────────
CREATE TABLE sessions (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    offering_id UUID        NOT NULL REFERENCES offerings(id) ON DELETE CASCADE,
    teacher_id  UUID        NOT NULL REFERENCES users(id),
    start_time  TIMESTAMPTZ NOT NULL,
    end_time    TIMESTAMPTZ NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_session_times CHECK (end_time > start_time)
);

CREATE INDEX idx_sessions_offering   ON sessions(offering_id);
CREATE INDEX idx_sessions_start_time ON sessions(start_time);
CREATE INDEX idx_sessions_time_range ON sessions(start_time, end_time);

-- ─────────────────────────────────────────────
-- BOOKINGS
-- ─────────────────────────────────────────────
CREATE TABLE bookings (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    parent_id   UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    offering_id UUID        NOT NULL REFERENCES offerings(id) ON DELETE CASCADE,
    status      VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED' CHECK (status IN ('CONFIRMED', 'CANCELLED')),
    booked_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_parent_offering UNIQUE (parent_id, offering_id)
);

CREATE INDEX idx_bookings_parent   ON bookings(parent_id);
CREATE INDEX idx_bookings_offering ON bookings(offering_id);
CREATE INDEX idx_bookings_status   ON bookings(status);
