package com.undoschool.bookingsystem.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bookings",
       uniqueConstraints = @UniqueConstraint(columnNames = {"parent_id", "offering_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", nullable = false)
    private User parent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "offering_id", nullable = false)
    private Offering offering;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.CONFIRMED;

    @Column(name = "booked_at", nullable = false, updatable = false)
    private Instant bookedAt;

    @PrePersist
    protected void onCreate() {
        this.bookedAt = Instant.now();
    }

    public enum Status { CONFIRMED, CANCELLED }
}
