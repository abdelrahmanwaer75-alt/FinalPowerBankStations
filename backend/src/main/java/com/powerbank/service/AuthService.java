package com.powerbank.service;

import com.powerbank.dto.AuthResponse;
import com.powerbank.dto.ActionResponse;
import com.powerbank.dto.IdentifierRequest;
import com.powerbank.dto.LoginRequest;
import com.powerbank.dto.RegisterRequest;
import com.powerbank.dto.ResetPasswordRequest;
import com.powerbank.dto.UserSummary;
import com.powerbank.dto.VerificationRequest;
import com.powerbank.entity.AccountStatus;
import com.powerbank.entity.AuthToken;
import com.powerbank.entity.AuthTokenPurpose;
import com.powerbank.entity.UserAccount;
import com.powerbank.exception.ApiException;
import com.powerbank.repository.AuthTokenRepository;
import com.powerbank.repository.UserAccountRepository;
import com.powerbank.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.Duration;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;

@Service
public class AuthService {

    private final UserAccountRepository users;
    private final AuthTokenRepository authTokens;
    private final AuthCodeDeliveryService codeDelivery;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(UserAccountRepository users, AuthTokenRepository authTokens, AuthCodeDeliveryService codeDelivery,
                       PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.authTokens = authTokens;
        this.codeDelivery = codeDelivery;
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
        user.setAccountStatus(AccountStatus.PENDING_VERIFICATION);
        users.save(user);
        issueCode(user, AuthTokenPurpose.VERIFY_ACCOUNT);

        return new AuthResponse(null, summary(user));
    }

    @Transactional
    public AuthResponse login(LoginRequest req) {
        String identifier = req.email().trim();
        UserAccount user = (identifier.contains("@")
                ? users.findByEmailIgnoreCase(identifier)
                : users.findByPhone(identifier))
                .filter(u -> encoder.matches(req.password(), u.getPasswordHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "BAD_CREDENTIALS",
                        "Incorrect email or phone number, or password"));

        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_NOT_ACTIVE",
                    "This account is not active. Contact support.");
        }

        user.setLastLoginAt(Instant.now());
        return new AuthResponse(jwt.issue(user), summary(user));
    }

    @Transactional
    public AuthResponse verify(VerificationRequest request) {
        UserAccount user = userFor(request.identifier());
        consumeCode(user, AuthTokenPurpose.VERIFY_ACCOUNT, request.code());
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setEmailVerifiedAt(Instant.now());
        return new AuthResponse(jwt.issue(user), summary(user));
    }

    @Transactional
    public ActionResponse resendVerification(IdentifierRequest request) {
        UserAccount user = userFor(request.identifier());
        if (user.getAccountStatus() == AccountStatus.ACTIVE) {
            return new ActionResponse("This account is already verified.");
        }
        Instant now = Instant.now();
        authTokens.findFirstByUserAndPurposeOrderByCreatedAtDesc(user, AuthTokenPurpose.VERIFY_ACCOUNT)
                .map(AuthToken::getCreatedAt)
                .filter(createdAt -> createdAt != null && createdAt.plusSeconds(60).isAfter(now))
                .ifPresent(retryAt -> {
                    long seconds = Math.max(1, Duration.between(now, retryAt.plusSeconds(60)).toSeconds() + 1);
                    throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "RESEND_COOLDOWN",
                            "Please wait " + seconds + " seconds before requesting another verification code.");
                });
        issueCode(user, AuthTokenPurpose.VERIFY_ACCOUNT);
        return new ActionResponse("A new verification code was issued.");
    }

    @Transactional
    public ActionResponse requestPasswordReset(IdentifierRequest request) {
        findUser(request.identifier()).ifPresent(user -> issueCode(user, AuthTokenPurpose.RESET_PASSWORD));
        return new ActionResponse("If this account exists, a recovery code was issued.");
    }

    @Transactional
    public ActionResponse resetPassword(ResetPasswordRequest request) {
        UserAccount user = userFor(request.identifier());
        consumeCode(user, AuthTokenPurpose.RESET_PASSWORD, request.code());
        user.setPasswordHash(encoder.encode(request.newPassword()));
        return new ActionResponse("Your password was updated. You can now sign in.");
    }

    private void issueCode(UserAccount user, AuthTokenPurpose purpose) {
        String code = String.format("%06d", new SecureRandom().nextInt(1_000_000));
        authTokens.save(new AuthToken(user, purpose, hash(code), Instant.now().plus(Duration.ofMinutes(10))));
        codeDelivery.deliver(user, purpose, code);
    }

    private void consumeCode(UserAccount user, AuthTokenPurpose purpose, String code) {
        AuthToken token = authTokens.findFirstByUserAndPurposeAndTokenHashAndUsedAtIsNullOrderByExpiresAtDesc(user, purpose, hash(code))
                .filter(t -> t.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "INVALID_CODE", "This code is invalid or has expired."));
        token.markUsed(Instant.now());
    }

    private UserAccount userFor(String identifier) {
        return findUser(identifier).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "No account matches this email or phone number."));
    }

    private java.util.Optional<UserAccount> findUser(String rawIdentifier) {
        String identifier = rawIdentifier.trim();
        return identifier.contains("@") ? users.findByEmailIgnoreCase(identifier.toLowerCase()) : users.findByPhone(identifier);
    }

    private static String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    private UserSummary summary(UserAccount u) {
        return new UserSummary(u.getId(), u.getFullName(), u.getEmail(), u.getAccountStatus().name());
    }
}
