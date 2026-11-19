package com.powerbank.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "returns")
public class RentalReturn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String reference;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rental_id", nullable = false, unique = true)
    private Rental rental;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_station_id")
    private Station returnStation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReturnStatus status = ReturnStatus.PENDING_REVIEW;

    @Column(name = "returned_at")
    private Instant returnedAt;

    @Column(name = "final_duration_minutes")
    private Integer finalDurationMinutes;

    @Column(name = "final_cost", precision = 10, scale = 2)
    private BigDecimal finalCost;

    protected RentalReturn() {}

    public RentalReturn(String reference, Rental rental, Station returnStation, Instant returnedAt,
                        int minutes, BigDecimal cost) {
        this.reference = reference;
        this.rental = rental;
        this.returnStation = returnStation;
        this.returnedAt = returnedAt;
        this.finalDurationMinutes = minutes;
        this.finalCost = cost;
    }

    public void setStatus(ReturnStatus status) { this.status = status; }

    public Long getId() { return id; }
    public String getReference() { return reference; }
    public Rental getRental() { return rental; }
    public Station getReturnStation() { return returnStation; }
    public ReturnStatus getStatus() { return status; }
    public Instant getReturnedAt() { return returnedAt; }
    public Integer getFinalDurationMinutes() { return finalDurationMinutes; }
    public BigDecimal getFinalCost() { return finalCost; }
}
