package com.powerbank.service;

import com.powerbank.dto.RegisterRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegisterRequestValidationTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsRequiredPhoneAndStrongPassword() {
        RegisterRequest request = new RegisterRequest("Test Customer", "test@example.com", "+201001234567", "StrongPass123!");
        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void rejectsMissingPhoneAndWeakPassword() {
        RegisterRequest request = new RegisterRequest("Test Customer", "test@example.com", null, "password1");
        var violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("phone")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("password")));
    }
}
