package com.powerbank.entity;

import jakarta.persistence.*;

import java.time.Instant;

/** Append-only. The MySQL schema blocks UPDATE and DELETE on this table with triggers. */
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(nullable = false, length = 80)
    private String action;

    @Column(name = "entity_type", nullable = false, length = 60)
    private String entityType;

    @Column(name = "entity_id", length = 60)
    private String entityId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AuditResult result;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(columnDefinition = "json")
    private String details;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    protected AuditLog() {}

    public AuditLog(Long actorUserId, String action, String entityType, String entityId,
                    AuditResult result, String ipAddress, String details) {
        this.actorUserId = actorUserId;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.result = result;
        this.ipAddress = ipAddress;
        this.details = details;
    }

    public enum AuditResult { SUCCESS, FAILED }
}
