package com.undoschool.bookingsystem.service;

import com.undoschool.bookingsystem.dto.response.Responses.BookingResponse;
import com.undoschool.bookingsystem.entity.Booking;
import com.undoschool.bookingsystem.entity.Offering;
import com.undoschool.bookingsystem.entity.User;
import com.undoschool.bookingsystem.exception.BookingExceptions.*;
import com.undoschool.bookingsystem.repository.BookingRepository;
import com.undoschool.bookingsystem.repository.OfferingRepository;
import com.undoschool.bookingsystem.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final OfferingRepository offeringRepository;
    private final SessionRepository sessionRepository;
    private final OfferingService offeringService;

    /**
     * Books an offering for a parent.
     *
     * Concurrency strategy:
     * ─────────────────────
     * We use REPEATABLE_READ isolation + a pessimistic write lock (SELECT FOR UPDATE)
     * on the offering row. This ensures that:
     *
     *  1. Two parents trying to book the same offering simultaneously — only one
     *     proceeds; the second waits and then re-evaluates (capacity could be added later).
     *
     *  2. A parent sending two simultaneous booking requests (e.g. double-tap) —
     *     the first acquires the lock, the second blocks. After the first commits,
     *     the second finds the UNIQUE constraint violation (parent_id, offering_id)
     *     and gets a DuplicateBookingException.
     *
     *  3. Conflict detection (interval overlap) is evaluated inside the same
     *     locked transaction, making the check-then-act atomic.
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public BookingResponse bookOffering(UUID offeringId, User parent) {

        // ── Step 1: Acquire pessimistic lock on the offering row ──────────────
        Offering offering = offeringRepository.findByIdForUpdate(offeringId)
                .orElseThrow(() -> new ResourceNotFoundException("Offering", offeringId));

        // ── Step 2: Business rule checks ──────────────────────────────────────
        if (offering.getStatus() != Offering.Status.ACTIVE) {
            throw new BookingConflictException("This offering is no longer available for booking");
        }

        if (bookingRepository.existsByParentIdAndOfferingId(parent.getId(), offeringId)) {
            throw new DuplicateBookingException();
        }

        // ── Step 3: Time conflict detection ───────────────────────────────────
        //
        //  Query counts sessions in the NEW offering that overlap with ANY
        //  session the parent has already booked (across all CONFIRMED bookings).
        //
        //  Two intervals [a_start, a_end) and [b_start, b_end) overlap iff:
        //      a_start < b_end  AND  a_end > b_start
        //
        long conflicts = sessionRepository.countConflictingSessions(parent.getId(), offeringId);

        if (conflicts > 0) {
            log.info("Booking rejected for parent {} on offering {} — {} conflicting session(s)",
                     parent.getId(), offeringId, conflicts);
            throw new BookingConflictException(
                    "Cannot book this offering: " + conflicts +
                    " session(s) overlap with your existing bookings");
        }

        // ── Step 4: Create booking ─────────────────────────────────────────────
        Booking booking = Booking.builder()
                .parent(parent)
                .offering(offering)
                .status(Booking.Status.CONFIRMED)
                .build();

        bookingRepository.save(booking);
        log.info("Booking confirmed: parent={}, offering={}", parent.getId(), offeringId);

        return toBookingResponse(booking, parent.getTimezone());
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getParentBookings(User parent) {
        return bookingRepository.findByParentId(parent.getId())
                .stream()
                .map(b -> toBookingResponse(b, parent.getTimezone()))
                .collect(Collectors.toList());
    }

    @Transactional
    public BookingResponse cancelBooking(UUID bookingId, User parent) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));

        if (!booking.getParent().getId().equals(parent.getId())) {
            throw new AccessDeniedException("You do not own this booking");
        }

        if (booking.getStatus() == Booking.Status.CANCELLED) {
            throw new BookingConflictException("Booking is already cancelled");
        }

        booking.setStatus(Booking.Status.CANCELLED);
        bookingRepository.save(booking);

        return toBookingResponse(booking, parent.getTimezone());
    }

    private BookingResponse toBookingResponse(Booking booking, String viewerTimezone) {
        var sessionResponses = sessionRepository
                .findByOfferingId(booking.getOffering().getId())
                .stream()
                .map(s -> offeringService.toSessionResponse(s, viewerTimezone))
                .collect(Collectors.toList());

        return BookingResponse.builder()
                .id(booking.getId())
                .parentId(booking.getParent().getId())
                .offeringId(booking.getOffering().getId())
                .offeringTitle(booking.getOffering().getTitle())
                .courseTitle(booking.getOffering().getCourse().getTitle())
                .status(booking.getStatus().name())
                .sessions(sessionResponses)
                .bookedAt(booking.getBookedAt())
                .build();
    }
}
