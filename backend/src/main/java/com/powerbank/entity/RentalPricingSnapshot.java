package com.powerbank.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

/** Copy of the pricing used for one rental. Later price changes never change this row. */
@Entity
@Table(name = "rental_pricing_snapshots")
public class RentalPricingSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rental_id", nullable = false, unique = true)
    private Rental rental;

    @Column(name = "plan_name", nullable = false, length = 100)
    private String planName;

    @Column(name = "base_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal baseFee;

    @Column(name = "price_per_minute", nullable = false, precision = 10, scale = 6)
    private BigDecimal pricePerMinute;

    @Column(name = "daily_max_fee", precision = 10, scale = 2)
    private BigDecimal dailyMaxFee;

    @Column(name = "free_minutes", nullable = false)
    private int freeMinutes;

    @Column(name = "late_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal lateFee;

    @Column(name = "reservation_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal reservationFee;

    protected RentalPricingSnapshot() {}

    public RentalPricingSnapshot(Rental rental, PricingPlan p) {
        this.rental = rental;
        this.planName = p.getName();
        this.baseFee = p.getBaseFee();
        this.pricePerMinute = p.getPricePerMinute();
        this.dailyMaxFee = p.getDailyMaxFee();
        this.freeMinutes = p.getFreeMinutes();
        this.lateFee = p.getLateFee();
        this.reservationFee = p.getReservationFee();
    }

    public BigDecimal getBaseFee() { return baseFee; }
    public BigDecimal getPricePerMinute() { return pricePerMinute; }
    public BigDecimal getDailyMaxFee() { return dailyMaxFee; }
    public int getFreeMinutes() { return freeMinutes; }
    public String getPlanName() { return planName; }
}
