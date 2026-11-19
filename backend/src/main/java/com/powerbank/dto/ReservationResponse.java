package com.powerbank.dto;

import java.time.Instant;

public record ReservationResponse(
        Long id,
        String reference,
        String status,
        Long stationId,
        String stationName,
        String stationAddress,
        String powerBankSerial,
        int durationMinutes,
        Instant expiresAt
) {}
