package com.powerbank.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "reservations")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String reference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "station_id", nullable = false)
    private Station station;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "powerbank_id")
    private PowerBank powerBank;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status = ReservationStatus.PENDING;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    protected Reservation() {}

    public Reservation(String reference, UserAccount user, Station station, PowerBank powerBank,
                       int durationMinutes, Instant expiresAt) {
        this.reference = reference;
        this.user = user;
        this.station = station;
        this.powerBank = powerBank;
        this.durationMinutes = durationMinutes;
        this.expiresAt = expiresAt;
    }

    public Long getId() { return id; }
    public String getReference() { return reference; }
    public UserAccount getUser() { return user; }
    public Station getStation() { return station; }
    public PowerBank getPowerBank() { return powerBank; }
    public ReservationStatus getStatus() { return status; }
    public void setStatus(ReservationStatus status) { this.status = status; }
    public int getDurationMinutes() { return durationMinutes; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(Instant t) { this.cancelledAt = t; }
    public Instant getCreatedAt() { return createdAt; }
}
