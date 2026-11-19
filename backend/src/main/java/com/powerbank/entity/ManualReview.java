package com.powerbank.entity;

import jakarta.persistence.*;

import java.time.Instant;

/** One staff decision on a return. Kept as history, one row per decision. */
@Entity
@Table(name = "manual_reviews")
public class ManualReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "return_id", nullable = false)
    private RentalReturn rentalReturn;

    @Column(name = "reviewer_id")
    private Long reviewerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ReviewDecision decision;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    protected ManualReview() {}

    public ManualReview(RentalReturn rentalReturn, Long reviewerId, ReviewDecision decision, String notes) {
        this.rentalReturn = rentalReturn;
        this.reviewerId = reviewerId;
        this.decision = decision;
        this.notes = notes;
    }
}
