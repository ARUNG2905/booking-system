package com.undoschool.bookingsystem.repository;

import com.undoschool.bookingsystem.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface SessionRepository extends JpaRepository<Session, UUID> {

    @Query("SELECT s FROM Session s WHERE s.offering.id = :offeringId ORDER BY s.startTime ASC")
    List<Session> findByOfferingId(@Param("offeringId") UUID offeringId);

    /**
     * Conflict detection query.
     *
     * Finds sessions already booked by the parent (via CONFIRMED bookings)
     * that overlap with ANY session in the target offering.
     *
     * Two intervals [a_start, a_end) and [b_start, b_end) overlap when:
     *   a_start < b_end AND a_end > b_start
     *
     * We compare every session of the new offering against every already-booked session.
     */
    @Query("""
        SELECT COUNT(existing) FROM Session existing
        JOIN existing.offering existingOffering
        JOIN existingOffering.bookings b
        WHERE b.parent.id   = :parentId
          AND b.status       = 'CONFIRMED'
          AND EXISTS (
              SELECT newSession FROM Session newSession
              WHERE newSession.offering.id = :newOfferingId
                AND newSession.startTime   < existing.endTime
                AND newSession.endTime     > existing.startTime
          )
        """)
    long countConflictingSessions(@Param("parentId") UUID parentId,
                                  @Param("newOfferingId") UUID newOfferingId);

    @Query("SELECT s FROM Session s JOIN FETCH s.offering o JOIN FETCH s.teacher " +
           "WHERE s.startTime >= :from ORDER BY s.startTime ASC")
    List<Session> findUpcomingSessions(@Param("from") Instant from);
}
