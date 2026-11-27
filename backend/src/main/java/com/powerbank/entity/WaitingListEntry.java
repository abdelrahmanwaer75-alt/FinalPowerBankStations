package com.powerbank.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "waiting_list",
        uniqueConstraints = @UniqueConstraint(name = "uq_waiting_user_station", columnNames = {"user_id", "station_id"}))
public class WaitingListEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "station_id", nullable = false)
    private Station station;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WaitingListStatus status = WaitingListStatus.WAITING;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;

    protected WaitingListEntry() {}

    public WaitingListEntry(UserAccount user, Station station, Instant createdAt) {
        this.user = user;
        this.station = station;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public UserAccount getUser() { return user; }
    public Station getStation() { return station; }
    public WaitingListStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }

    public void setStatus(WaitingListStatus status) { this.status = status; }

    public void rejoin(Instant now) {
        this.status = WaitingListStatus.WAITING;
        this.createdAt = now;
    }
}
