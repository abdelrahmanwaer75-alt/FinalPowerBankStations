package com.powerbank.controller;

import com.powerbank.dto.AuthResponse;
import com.powerbank.dto.LoginRequest;
import com.powerbank.dto.RegisterRequest;
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

    /** Tokens are stateless. The client drops its token. The server keeps no session to clear. */
    @PostMapping("/logout")
    ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }
}
