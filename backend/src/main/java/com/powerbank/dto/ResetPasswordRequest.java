package com.powerbank.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank @Size(max = 190) String identifier,
        @NotBlank @Pattern(regexp = "^[0-9]{6}$", message = "Enter the six-digit code") String code,
        @NotBlank @Size(min = 8, max = 72, message = "Password must be 8 to 72 characters")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s]).+$",
                message = "Password must include upper and lower case letters, a number, and a special character") String newPassword
) {}
