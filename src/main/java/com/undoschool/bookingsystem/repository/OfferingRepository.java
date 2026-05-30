package com.undoschool.bookingsystem.repository;

import com.undoschool.bookingsystem.entity.Offering;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OfferingRepository extends JpaRepository<Offering, UUID> {

    @Query("SELECT o FROM Offering o JOIN FETCH o.course JOIN FETCH o.teacher " +
           "WHERE o.teacher.id = :teacherId ORDER BY o.createdAt DESC")
    List<Offering> findByTeacherId(@Param("teacherId") UUID teacherId);

    @Query("SELECT o FROM Offering o JOIN FETCH o.course JOIN FETCH o.teacher " +
           "WHERE o.status = 'ACTIVE' ORDER BY o.createdAt DESC")
    List<Offering> findAllActive();

    /**
     * Pessimistic write lock — used during booking to prevent concurrent double-bookings.
     * Equivalent to SELECT ... FOR UPDATE in SQL.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Offering o WHERE o.id = :id")
    Optional<Offering> findByIdForUpdate(@Param("id") UUID id);
}
