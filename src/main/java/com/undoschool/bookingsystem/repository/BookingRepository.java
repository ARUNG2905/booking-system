package com.undoschool.bookingsystem.repository;

import com.undoschool.bookingsystem.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    @Query("SELECT b FROM Booking b JOIN FETCH b.offering o JOIN FETCH o.course " +
           "WHERE b.parent.id = :parentId ORDER BY b.bookedAt DESC")
    List<Booking> findByParentId(@Param("parentId") UUID parentId);

    Optional<Booking> findByParentIdAndOfferingId(UUID parentId, UUID offeringId);

    boolean existsByParentIdAndOfferingId(UUID parentId, UUID offeringId);
}
