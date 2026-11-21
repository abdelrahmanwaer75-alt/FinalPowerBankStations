package com.powerbank.controller;

import com.powerbank.dto.AuthResponse;
import com.powerbank.dto.ActionResponse;
import com.powerbank.dto.IdentifierRequest;
import com.powerbank.dto.LoginRequest;
import com.powerbank.dto.RegisterRequest;
import com.powerbank.dto.ResetPasswordRequest;
import com.powerbank.dto.VerificationRequest;
import com.powerbank.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/register")
    ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(auth.register(req));
    }

    @PostMapping("/login")
    AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return auth.login(req);
    }

    @PostMapping("/verify")
    AuthResponse verify(@Valid @RequestBody VerificationRequest req) {
        return auth.verify(req);
    }

    @PostMapping("/resend-verification")
    ActionResponse resendVerification(@Valid @RequestBody IdentifierRequest req) {
        return auth.resendVerification(req);
    }

    @PostMapping("/forgot-password")
    ActionResponse forgotPassword(@Valid @RequestBody IdentifierRequest req) {
        return auth.requestPasswordReset(req);
    }

    @PostMapping("/reset-password")
    ActionResponse resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        return auth.resetPassword(req);
    }

    /** Tokens are stateless. The client drops its token. The server keeps no session to clear. */
    @PostMapping("/logout")
    ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }
}
