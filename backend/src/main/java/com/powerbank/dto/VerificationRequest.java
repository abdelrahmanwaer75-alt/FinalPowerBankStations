package com.powerbank.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record VerificationRequest(
        @NotBlank @Size(max = 190) String identifier,
        @NotBlank @Pattern(regexp = "^[0-9]{6}$", message = "Enter the six-digit code") String code
) {}
