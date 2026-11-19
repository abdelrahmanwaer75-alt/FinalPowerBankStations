package com.powerbank.dto;

import java.time.Instant;

public record CustomerProfileResponse(
        Long id, String fullName, String email, String phone,
        String languagePreference, String themePreference, Instant createdAt
) {}
