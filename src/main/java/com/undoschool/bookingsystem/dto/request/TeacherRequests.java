package com.undoschool.bookingsystem.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class TeacherRequests {

    @Data
    public static class CreateCourseRequest {
        @NotBlank(message = "Course title is required")
        private String title;

        private String description;
    }

    @Data
    public static class CreateOfferingRequest {
        @NotNull(message = "Course ID is required")
        private UUID courseId;

        @NotBlank(message = "Offering title is required")
        private String title;

        private String description;
    }

    @Data
    public static class AddSessionRequest {
        /**
         * Local date-time in the teacher's own timezone.
         * Example: "2025-06-07T18:00:00"
         * The service will convert this to UTC using the authenticated teacher's stored timezone.
         */
        @NotNull(message = "Start time is required")
        private LocalDateTime startTime;

        @NotNull(message = "End time is required")
        private LocalDateTime endTime;
    }

    @Data
    public static class AddSessionsRequest {
        @NotEmpty(message = "At least one session is required")
        @Valid
        private List<AddSessionRequest> sessions;
    }
}
