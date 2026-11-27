package com.powerbank.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "powerbank_status_history")
public class PowerBankStatusEvent {
    @Id
    private Long id;

    @Column(name = "powerbank_id", nullable = false)
    private Long powerBankId;

    @Column(name = "old_status", length = 40)
    private String oldStatus;

    @Column(name = "new_status", nullable = false, length = 40)
    private String newStatus;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    protected PowerBankStatusEvent() {}

    public Long getId() { return id; }
    public Long getPowerBankId() { return powerBankId; }
    public String getOldStatus() { return oldStatus; }
    public String getNewStatus() { return newStatus; }
    public Instant getChangedAt() { return changedAt; }
}
