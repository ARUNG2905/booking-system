package com.undoschool.bookingsystem.service;

import com.undoschool.bookingsystem.dto.request.TeacherRequests.*;
import com.undoschool.bookingsystem.dto.response.Responses.*;
import com.undoschool.bookingsystem.entity.*;
import com.undoschool.bookingsystem.exception.BookingExceptions.*;
import com.undoschool.bookingsystem.repository.*;
import com.undoschool.bookingsystem.util.TimezoneConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OfferingService {

    private final CourseRepository courseRepository;
    private final OfferingRepository offeringRepository;
    private final SessionRepository sessionRepository;
    private final TimezoneConverter timezoneConverter;

    // ── Courses ──────────────────────────────────────────────────────────────

    @Transactional
    public CourseResponse createCourse(CreateCourseRequest request, User teacher) {
        Course course = Course.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .teacher(teacher)
                .build();
        courseRepository.save(course);
        return toCourseResponse(course);
    }

    @Transactional(readOnly = true)
    public List<CourseResponse> getTeacherCourses(User teacher) {
        return courseRepository.findByTeacherId(teacher.getId())
                .stream().map(this::toCourseResponse).collect(Collectors.toList());
    }

    // ── Offerings ─────────────────────────────────────────────────────────────

    @Transactional
    public OfferingResponse createOffering(CreateOfferingRequest request, User teacher) {
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course", request.getCourseId()));

        if (!course.getTeacher().getId().equals(teacher.getId())) {
            throw new AccessDeniedException("You do not own this course");
        }

        Offering offering = Offering.builder()
                .course(course)
                .teacher(teacher)
                .title(request.getTitle())
                .description(request.getDescription())
                .build();

        offeringRepository.save(offering);
        return toOfferingResponse(offering, teacher.getTimezone());
    }

    @Transactional(readOnly = true)
    public List<OfferingResponse> getTeacherOfferings(User teacher) {
        return offeringRepository.findByTeacherId(teacher.getId())
                .stream()
                .map(o -> toOfferingResponse(o, teacher.getTimezone()))
                .collect(Collectors.toList());
    }

    // ── Sessions ──────────────────────────────────────────────────────────────

    /**
     * Adds one or more sessions to an offering.
     * Accepts LocalDateTime values in the teacher's own timezone and persists as UTC.
     */
    @Transactional
    public List<SessionResponse> addSessions(UUID offeringId,
                                              AddSessionsRequest request,
                                              User teacher) {
        Offering offering = offeringRepository.findById(offeringId)
                .orElseThrow(() -> new ResourceNotFoundException("Offering", offeringId));

        if (!offering.getTeacher().getId().equals(teacher.getId())) {
            throw new AccessDeniedException("You do not own this offering");
        }

        List<Session> sessions = request.getSessions().stream().map(req -> {
            // Validate that end is after start
            if (!req.getEndTime().isAfter(req.getStartTime())) {
                throw new InvalidSessionTimeException("End time must be after start time");
            }

            Instant startUtc = timezoneConverter.toUtc(req.getStartTime(), teacher.getTimezone());
            Instant endUtc   = timezoneConverter.toUtc(req.getEndTime(),   teacher.getTimezone());

            return Session.builder()
                    .offering(offering)
                    .teacher(teacher)
                    .startTime(startUtc)
                    .endTime(endUtc)
                    .build();
        }).collect(Collectors.toList());

        sessionRepository.saveAll(sessions);

        return sessions.stream()
                .map(s -> toSessionResponse(s, teacher.getTimezone()))
                .collect(Collectors.toList());
    }

    // ── Public (parent) offerings ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<OfferingResponse> getAvailableOfferings(String viewerTimezone) {
        return offeringRepository.findAllActive()
                .stream()
                .map(o -> toOfferingResponse(o, viewerTimezone))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OfferingResponse getOffering(UUID offeringId, String viewerTimezone) {
        Offering offering = offeringRepository.findById(offeringId)
                .orElseThrow(() -> new ResourceNotFoundException("Offering", offeringId));
        return toOfferingResponse(offering, viewerTimezone);
    }

    // ── Mappers ──────────────────────────────────────────────────────────────

    private CourseResponse toCourseResponse(Course course) {
        return CourseResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .description(course.getDescription())
                .teacherId(course.getTeacher().getId())
                .teacherName(course.getTeacher().getName())
                .createdAt(course.getCreatedAt())
                .build();
    }

    public OfferingResponse toOfferingResponse(Offering offering, String viewerTimezone) {
        List<Session> sessions = sessionRepository.findByOfferingId(offering.getId());
        List<SessionResponse> sessionResponses = sessions.stream()
                .map(s -> toSessionResponse(s, viewerTimezone))
                .collect(Collectors.toList());

        return OfferingResponse.builder()
                .id(offering.getId())
                .courseId(offering.getCourse().getId())
                .courseTitle(offering.getCourse().getTitle())
                .teacherId(offering.getTeacher().getId())
                .teacherName(offering.getTeacher().getName())
                .title(offering.getTitle())
                .description(offering.getDescription())
                .status(offering.getStatus().name())
                .sessions(sessionResponses)
                .createdAt(offering.getCreatedAt())
                .build();
    }

    public SessionResponse toSessionResponse(Session session, String viewerTimezone) {
        return SessionResponse.builder()
                .id(session.getId())
                .offeringId(session.getOffering().getId())
                .teacherId(session.getTeacher().getId())
                .startTimeUtc(session.getStartTime())
                .endTimeUtc(session.getEndTime())
                .startTimeLocal(timezoneConverter.toLocalString(session.getStartTime(), viewerTimezone))
                .endTimeLocal(timezoneConverter.toLocalString(session.getEndTime(),   viewerTimezone))
                .timezone(viewerTimezone)
                .build();
    }
}
