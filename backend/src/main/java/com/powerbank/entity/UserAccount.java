package com.powerbank.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "users")
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(nullable = false, unique = true, length = 190)
    private String email;

    @Column(unique = true, length = 20)
    private String phone;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false, length = 30)
    private AccountStatus accountStatus = AccountStatus.PENDING_VERIFICATION;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "language_preference", length = 2)
    private String languagePreference;

    @Column(name = "theme_preference", length = 5)
    private String themePreference;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    protected UserAccount() {}

    public UserAccount(String fullName, String email, String phone, String passwordHash) {
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.passwordHash = passwordHash;
    }

    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getPasswordHash() { return passwordHash; }
    public AccountStatus getAccountStatus() { return accountStatus; }
    public void setAccountStatus(AccountStatus status) { this.accountStatus = status; }
    public Instant getEmailVerifiedAt() { return emailVerifiedAt; }
    public void setEmailVerifiedAt(Instant t) { this.emailVerifiedAt = t; }
    public Instant getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(Instant t) { this.lastLoginAt = t; }
    public Instant getCreatedAt() { return createdAt; }
    public String getLanguagePreference() { return languagePreference; }
    public String getThemePreference() { return themePreference; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setPasswordHash(String hash) { this.passwordHash = hash; }
    public void setLanguagePreference(String v) { this.languagePreference = v; }
    public void setThemePreference(String v) { this.themePreference = v; }
}
