package com.powerbank.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "rentals")
public class Rental {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String reference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "powerbank_id", nullable = false)
    private PowerBank powerBank;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pickup_station_id", nullable = false)
    private Station pickupStation;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", unique = true)
    private Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pricing_plan_id")
    private PricingPlan pricingPlan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RentalStatus status = RentalStatus.ACTIVE;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time")
    private Instant endTime;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "estimated_cost", precision = 10, scale = 2)
    private BigDecimal estimatedCost;

    @Column(name = "final_cost", precision = 10, scale = 2)
    private BigDecimal finalCost;

    protected Rental() {}

    public Rental(String reference, UserAccount user, PowerBank powerBank, Station pickupStation,
                  Reservation reservation, PricingPlan plan, Instant startTime) {
        this.reference = reference;
        this.user = user;
        this.powerBank = powerBank;
        this.pickupStation = pickupStation;
        this.reservation = reservation;
        this.pricingPlan = plan;
        this.startTime = startTime;
    }

    public Long getId() { return id; }
    public String getReference() { return reference; }
    public UserAccount getUser() { return user; }
    public PowerBank getPowerBank() { return powerBank; }
    public Station getPickupStation() { return pickupStation; }
    public Reservation getReservation() { return reservation; }
    public RentalStatus getStatus() { return status; }
    public void setStatus(RentalStatus status) { this.status = status; }
    public Instant getStartTime() { return startTime; }
    public Instant getEndTime() { return endTime; }
    public Integer getDurationMinutes() { return durationMinutes; }
    public BigDecimal getEstimatedCost() { return estimatedCost; }
    public BigDecimal getFinalCost() { return finalCost; }

    public void complete(Instant end, int minutes, BigDecimal cost) {
        this.status = RentalStatus.COMPLETED;
        this.endTime = end;
        this.durationMinutes = minutes;
        this.estimatedCost = cost;
        this.finalCost = cost;
    }
}
