package com.powerbank.dto;

import jakarta.validation.constraints.*;

public record RegisterRequest(
        @NotBlank @Size(max = 120) String fullName,
        @NotBlank @Email @Size(max = 190) String email,
        @Pattern(regexp = "^\\+?[0-9 ()-]{7,20}$", message = "Invalid phone number") String phone,
        @NotBlank @Size(min = 8, max = 72, message = "Password must be 8 to 72 characters") String password
) {}
