package com.undoschool.bookingsystem.controller;

import com.undoschool.bookingsystem.dto.response.Responses.*;
import com.undoschool.bookingsystem.entity.User;
import com.undoschool.bookingsystem.service.BookingService;
import com.undoschool.bookingsystem.service.OfferingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/parents")
@RequiredArgsConstructor
@Tag(name = "Parent APIs", description = "Browse offerings and manage bookings")
@SecurityRequirement(name = "bearerAuth")
public class ParentController {

    private final OfferingService offeringService;
    private final BookingService bookingService;

    @GetMapping("/offerings")
    @Operation(summary = "Browse all available offerings",
               description = "Session times are automatically converted to the parent's stored timezone")
    public ResponseEntity<List<OfferingResponse>> getAvailableOfferings(
            @AuthenticationPrincipal User parent) {
        return ResponseEntity.ok(offeringService.getAvailableOfferings(parent.getTimezone()));
    }

    @GetMapping("/offerings/{offeringId}")
    @Operation(summary = "Get details of a specific offering (sessions in parent's timezone)")
    public ResponseEntity<OfferingResponse> getOffering(
            @PathVariable UUID offeringId,
            @AuthenticationPrincipal User parent) {
        return ResponseEntity.ok(offeringService.getOffering(offeringId, parent.getTimezone()));
    }

    @PostMapping("/bookings/{offeringId}")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Book an offering",
               description = "Books all sessions in the offering. Returns 409 if there is a time conflict with an existing booking.")
    public ResponseEntity<BookingResponse> bookOffering(
            @PathVariable UUID offeringId,
            @AuthenticationPrincipal User parent) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bookingService.bookOffering(offeringId, parent));
    }

    @GetMapping("/bookings")
    @Operation(summary = "Get all bookings for the authenticated parent",
               description = "Session times are shown in the parent's local timezone")
    public ResponseEntity<List<BookingResponse>> getBookings(@AuthenticationPrincipal User parent) {
        return ResponseEntity.ok(bookingService.getParentBookings(parent));
    }

    @DeleteMapping("/bookings/{bookingId}")
    @Operation(summary = "Cancel a booking")
    public ResponseEntity<BookingResponse> cancelBooking(
            @PathVariable UUID bookingId,
            @AuthenticationPrincipal User parent) {
        return ResponseEntity.ok(bookingService.cancelBooking(bookingId, parent));
    }
}
