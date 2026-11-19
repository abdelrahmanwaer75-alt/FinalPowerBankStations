package com.powerbank.dto;

import jakarta.validation.constraints.*;

public record UpdateProfileRequest(
        @NotBlank @Size(max = 120) String fullName,
        @Pattern(regexp = "^$|^\\+?[0-9 ()-]{7,20}$", message = "Invalid phone number") String phone,
        @Pattern(regexp = "^(AR|EN)$", message = "Language must be AR or EN") String languagePreference,
        @Pattern(regexp = "^(LIGHT|DARK)$", message = "Theme must be LIGHT or DARK") String themePreference
) {}
