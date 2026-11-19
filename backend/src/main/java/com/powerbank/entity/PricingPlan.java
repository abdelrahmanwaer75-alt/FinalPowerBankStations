package com.powerbank.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "pricing_plans")
public class PricingPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "base_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal baseFee;

    @Column(name = "price_per_hour", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerHour;

    @Column(name = "daily_max_fee", precision = 10, scale = 2)
    private BigDecimal dailyMaxFee;

    @Column(name = "free_minutes", nullable = false)
    private int freeMinutes;

    @Column(name = "late_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal lateFee;

    @Column(name = "reservation_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal reservationFee;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    protected PricingPlan() {}

    public PricingPlan(String name, java.math.BigDecimal baseFee, java.math.BigDecimal pricePerHour,
                       java.math.BigDecimal dailyMaxFee, int freeMinutes, java.math.BigDecimal lateFee,
                       java.math.BigDecimal reservationFee) {
        this.name = name;
        this.baseFee = baseFee;
        this.pricePerHour = pricePerHour;
        this.dailyMaxFee = dailyMaxFee;
        this.freeMinutes = freeMinutes;
        this.lateFee = lateFee;
        this.reservationFee = reservationFee;
    }

    public void setActive(boolean active) { this.active = active; }

    public Integer getId() { return id; }
    public String getName() { return name; }
    public BigDecimal getBaseFee() { return baseFee; }
    public BigDecimal getPricePerHour() { return pricePerHour; }
    public BigDecimal getDailyMaxFee() { return dailyMaxFee; }
    public int getFreeMinutes() { return freeMinutes; }
    public BigDecimal getLateFee() { return lateFee; }
    public BigDecimal getReservationFee() { return reservationFee; }
    public boolean isActive() { return active; }
}
