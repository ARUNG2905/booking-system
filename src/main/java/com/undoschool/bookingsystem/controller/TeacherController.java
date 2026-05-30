package com.undoschool.bookingsystem.controller;

import com.undoschool.bookingsystem.dto.request.TeacherRequests.*;
import com.undoschool.bookingsystem.dto.response.Responses.*;
import com.undoschool.bookingsystem.entity.User;
import com.undoschool.bookingsystem.service.OfferingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/teachers")
@RequiredArgsConstructor
@Tag(name = "Teacher APIs", description = "Manage courses, offerings and sessions")
@SecurityRequirement(name = "bearerAuth")
public class TeacherController {

    private final OfferingService offeringService;

    // ── Courses ──────────────────────────────────────────────────────────────

    @PostMapping("/courses")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new course")
    public ResponseEntity<CourseResponse> createCourse(
            @Valid @RequestBody CreateCourseRequest request,
            @AuthenticationPrincipal User teacher) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(offeringService.createCourse(request, teacher));
    }

    @GetMapping("/courses")
    @Operation(summary = "Get all courses created by the authenticated teacher")
    public ResponseEntity<List<CourseResponse>> getCourses(@AuthenticationPrincipal User teacher) {
        return ResponseEntity.ok(offeringService.getTeacherCourses(teacher));
    }

    // ── Offerings ─────────────────────────────────────────────────────────────

    @PostMapping("/offerings")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create an offering (section) for a course")
    public ResponseEntity<OfferingResponse> createOffering(
            @Valid @RequestBody CreateOfferingRequest request,
            @AuthenticationPrincipal User teacher) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(offeringService.createOffering(request, teacher));
    }

    @GetMapping("/offerings")
    @Operation(summary = "Get all offerings created by the authenticated teacher")
    public ResponseEntity<List<OfferingResponse>> getOfferings(@AuthenticationPrincipal User teacher) {
        return ResponseEntity.ok(offeringService.getTeacherOfferings(teacher));
    }

    // ── Sessions ──────────────────────────────────────────────────────────────

    @PostMapping("/offerings/{offeringId}/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add one or more sessions to an offering",
               description = "Provide times in your local timezone — they will be stored as UTC")
    public ResponseEntity<List<SessionResponse>> addSessions(
            @PathVariable UUID offeringId,
            @Valid @RequestBody AddSessionsRequest request,
            @AuthenticationPrincipal User teacher) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(offeringService.addSessions(offeringId, request, teacher));
    }

    @GetMapping("/offerings/{offeringId}")
    @Operation(summary = "Get a specific offering with all its sessions (in teacher's timezone)")
    public ResponseEntity<OfferingResponse> getOffering(
            @PathVariable UUID offeringId,
            @AuthenticationPrincipal User teacher) {
        return ResponseEntity.ok(offeringService.getOffering(offeringId, teacher.getTimezone()));
    }
}
