package com.powerbank.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "admin_account_requests")
public class AdminAccountRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(nullable = false, length = 190)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(name = "requested_role", nullable = false, length = 30)
    private String requestedRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AdminAccountRequestStatus status = AdminAccountRequestStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by", nullable = false)
    private UserAccount requestedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private UserAccount reviewedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_user_id")
    private UserAccount createdUser;

    @Column(name = "review_notes", length = 500)
    private String reviewNotes;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private Instant requestedAt = Instant.now();

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    protected AdminAccountRequest() { }

    public AdminAccountRequest(String fullName, String email, String phone,
                               String requestedRole, UserAccount requestedBy) {
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.requestedRole = requestedRole;
        this.requestedBy = requestedBy;
    }

    public void approve(UserAccount reviewer, UserAccount createdUser, String notes) {
        this.status = AdminAccountRequestStatus.APPROVED;
        this.reviewedBy = reviewer;
        this.createdUser = createdUser;
        this.reviewNotes = notes;
        this.reviewedAt = Instant.now();
    }

    public void reject(UserAccount reviewer, String notes) {
        this.status = AdminAccountRequestStatus.REJECTED;
        this.reviewedBy = reviewer;
        this.reviewNotes = notes;
        this.reviewedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getRequestedRole() { return requestedRole; }
    public AdminAccountRequestStatus getStatus() { return status; }
    public UserAccount getRequestedBy() { return requestedBy; }
    public UserAccount getReviewedBy() { return reviewedBy; }
    public UserAccount getCreatedUser() { return createdUser; }
    public String getReviewNotes() { return reviewNotes; }
    public Instant getRequestedAt() { return requestedAt; }
    public Instant getReviewedAt() { return reviewedAt; }
}
