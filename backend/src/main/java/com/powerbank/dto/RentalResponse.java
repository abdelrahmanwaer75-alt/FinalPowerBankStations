package com.powerbank.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record RentalResponse(
        Long id,
        String reference,
        String status,
        String pickupStationName,
        String powerBankSerial,
        String planName,
        BigDecimal pricePerMinute,
        Instant startTime,
        Instant endTime,
        long elapsedMinutes,
        BigDecimal cost,
        boolean costIsFinal
) {}
