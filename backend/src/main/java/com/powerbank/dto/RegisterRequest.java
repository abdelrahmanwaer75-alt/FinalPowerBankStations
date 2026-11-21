package com.powerbank.dto;

import jakarta.validation.constraints.*;

public record RegisterRequest(
        @NotBlank @Size(max = 120) String fullName,
        @NotBlank @Email @Size(max = 190) String email,
        @NotBlank @Pattern(regexp = "^(?=(?:\\D*\\d){7,20}$)\\+?[0-9 ()-]{7,20}$", message = "Invalid phone number") String phone,
        @NotBlank @Size(min = 8, max = 72, message = "Password must be 8 to 72 characters")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s]).+$",
                message = "Password must include upper and lower case letters, a number, and a special character") String password
) {}
