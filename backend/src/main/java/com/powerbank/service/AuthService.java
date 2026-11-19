package com.powerbank.service;

import com.powerbank.dto.AuthResponse;
import com.powerbank.dto.LoginRequest;
import com.powerbank.dto.RegisterRequest;
import com.powerbank.dto.UserSummary;
import com.powerbank.entity.AccountStatus;
import com.powerbank.entity.UserAccount;
import com.powerbank.exception.ApiException;
import com.powerbank.repository.UserAccountRepository;
import com.powerbank.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class AuthService {

    private final UserAccountRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(UserAccountRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        String phone = req.phone() == null || req.phone().isBlank() ? null : req.phone().trim();

        if (users.existsByEmailIgnoreCase(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_TAKEN", "This email is already registered");
        }
        if (phone != null && users.existsByPhone(phone)) {
            throw new ApiException(HttpStatus.CONFLICT, "PHONE_TAKEN", "This phone number is already registered");
        }

        UserAccount user = new UserAccount(req.fullName().trim(), email, phone, encoder.encode(req.password()));
        // Verification by email or SMS is not built yet, so accounts start ACTIVE.
        // When the verification flow is added, set PENDING_VERIFICATION here instead.
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setEmailVerifiedAt(Instant.now());
        users.save(user);

        return new AuthResponse(jwt.issue(user), summary(user));
    }

    @Transactional
    public AuthResponse login(LoginRequest req) {
        UserAccount user = users.findByEmailIgnoreCase(req.email().trim())
                .filter(u -> encoder.matches(req.password(), u.getPasswordHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "BAD_CREDENTIALS",
                        "Incorrect email or password"));

        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_NOT_ACTIVE",
                    "This account is not active. Contact support.");
        }

        user.setLastLoginAt(Instant.now());
        return new AuthResponse(jwt.issue(user), summary(user));
    }

    private UserSummary summary(UserAccount u) {
        return new UserSummary(u.getId(), u.getFullName(), u.getEmail(), u.getAccountStatus().name());
    }
}
