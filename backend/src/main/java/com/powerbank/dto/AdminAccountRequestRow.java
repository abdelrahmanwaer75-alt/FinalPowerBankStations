package com.powerbank.dto;

import java.time.Instant;

public record AdminAccountRequestRow(
        Long id,
        String fullName,
        String email,
        String phone,
        String requestedRole,
        String status,
        String requestedBy,
        Long createdUserId,
        String reviewNotes,
        Instant requestedAt,
        Instant reviewedAt
) { }
