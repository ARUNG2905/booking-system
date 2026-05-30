package com.undoschool.bookingsystem.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Responses {

    @Data @Builder
    public static class AuthResponse {
        private String token;
        private String email;
        private String role;
        private String name;
    }

    @Data @Builder
    public static class UserResponse {
        private UUID id;
        private String name;
        private String email;
        private String role;
        private String timezone;
    }

    @Data @Builder
    public static class CourseResponse {
        private UUID id;
        private String title;
        private String description;
        private UUID teacherId;
        private String teacherName;
        private Instant createdAt;
    }

    @Data @Builder
    public static class SessionResponse {
        private UUID id;
        private UUID offeringId;
        private UUID teacherId;

        // UTC ISO-8601 string — canonical storage format
        private Instant startTimeUtc;
        private Instant endTimeUtc;

        // Localised display for the requesting user's timezone
        private String startTimeLocal;
        private String endTimeLocal;
        private String timezone;
    }

    @Data @Builder
    public static class OfferingResponse {
        private UUID id;
        private UUID courseId;
        private String courseTitle;
        private UUID teacherId;
        private String teacherName;
        private String title;
        private String description;
        private String status;
        private List<SessionResponse> sessions;
        private Instant createdAt;
    }

    @Data @Builder
    public static class BookingResponse {
        private UUID id;
        private UUID parentId;
        private UUID offeringId;
        private String offeringTitle;
        private String courseTitle;
        private String status;
        private List<SessionResponse> sessions;
        private Instant bookedAt;
    }

    @Data @Builder
    public static class ApiError {
        private int status;
        private String error;
        private String message;
        private Instant timestamp;
    }
}
