package com.powerbank.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank String currentPassword,
        @Size(min = 8, max = 72, message = "New password must be 8 to 72 characters") String newPassword
) {}
