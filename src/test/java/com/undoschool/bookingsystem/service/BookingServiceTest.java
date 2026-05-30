package com.undoschool.bookingsystem.service;

import com.undoschool.bookingsystem.entity.*;
import com.undoschool.bookingsystem.exception.BookingExceptions.*;
import com.undoschool.bookingsystem.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock BookingRepository bookingRepository;
    @Mock OfferingRepository offeringRepository;
    @Mock SessionRepository sessionRepository;
    @Mock OfferingService offeringService;

    @InjectMocks BookingService bookingService;

    private User parent;
    private User teacher;
    private Course course;
    private Offering offering;
    private UUID offeringId;

    @BeforeEach
    void setup() {
        parent = User.builder()
                .id(UUID.randomUUID())
                .email("parent@test.com")
                .name("Test Parent")
                .role(User.Role.PARENT)
                .timezone("Asia/Kolkata")
                .build();

        teacher = User.builder()
                .id(UUID.randomUUID())
                .email("teacher@test.com")
                .name("Test Teacher")
                .role(User.Role.TEACHER)
                .timezone("America/New_York")
                .build();

        course = Course.builder()
                .id(UUID.randomUUID())
                .title("Python Coding")
                .teacher(teacher)
                .build();

        offeringId = UUID.randomUUID();
        offering = Offering.builder()
                .id(offeringId)
                .course(course)
                .teacher(teacher)
                .title("Saturday Batch")
                .status(Offering.Status.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("Successful booking when no conflicts exist")
    void bookOffering_success() {
        when(offeringRepository.findByIdForUpdate(offeringId)).thenReturn(Optional.of(offering));
        when(bookingRepository.existsByParentIdAndOfferingId(parent.getId(), offeringId)).thenReturn(false);
        when(sessionRepository.countConflictingSessions(parent.getId(), offeringId)).thenReturn(0L);
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(sessionRepository.findByOfferingId(offeringId)).thenReturn(List.of());

        var response = bookingService.bookOffering(offeringId, parent);

        assertThat(response).isNotNull();
        assertThat(response.getOfferingId()).isEqualTo(offeringId);
        assertThat(response.getStatus()).isEqualTo("CONFIRMED");
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    @DisplayName("Reject booking when offering is closed")
    void bookOffering_closedOffering_throwsConflict() {
        offering.setStatus(Offering.Status.CLOSED);
        when(offeringRepository.findByIdForUpdate(offeringId)).thenReturn(Optional.of(offering));

        assertThatThrownBy(() -> bookingService.bookOffering(offeringId, parent))
                .isInstanceOf(BookingConflictException.class)
                .hasMessageContaining("no longer available");
    }

    @Test
    @DisplayName("Reject duplicate booking by same parent")
    void bookOffering_duplicate_throwsDuplicateException() {
        when(offeringRepository.findByIdForUpdate(offeringId)).thenReturn(Optional.of(offering));
        when(bookingRepository.existsByParentIdAndOfferingId(parent.getId(), offeringId)).thenReturn(true);

        assertThatThrownBy(() -> bookingService.bookOffering(offeringId, parent))
                .isInstanceOf(DuplicateBookingException.class);
    }

    @Test
    @DisplayName("Reject booking when session time conflict detected")
    void bookOffering_sessionConflict_throwsConflict() {
        when(offeringRepository.findByIdForUpdate(offeringId)).thenReturn(Optional.of(offering));
        when(bookingRepository.existsByParentIdAndOfferingId(parent.getId(), offeringId)).thenReturn(false);
        when(sessionRepository.countConflictingSessions(parent.getId(), offeringId)).thenReturn(2L);

        assertThatThrownBy(() -> bookingService.bookOffering(offeringId, parent))
                .isInstanceOf(BookingConflictException.class)
                .hasMessageContaining("2 session(s) overlap");
    }

    @Test
    @DisplayName("Throw ResourceNotFoundException for unknown offering")
    void bookOffering_unknownOffering_throwsNotFound() {
        when(offeringRepository.findByIdForUpdate(offeringId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.bookOffering(offeringId, parent))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Cancel a confirmed booking successfully")
    void cancelBooking_success() {
        UUID bookingId = UUID.randomUUID();
        Booking booking = Booking.builder()
                .id(bookingId)
                .parent(parent)
                .offering(offering)
                .status(Booking.Status.CONFIRMED)
                .build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(sessionRepository.findByOfferingId(offeringId)).thenReturn(List.of());

        var response = bookingService.cancelBooking(bookingId, parent);

        assertThat(response.getStatus()).isEqualTo("CANCELLED");
    }
}
