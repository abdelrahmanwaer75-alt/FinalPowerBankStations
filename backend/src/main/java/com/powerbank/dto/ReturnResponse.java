package com.powerbank.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ReturnResponse(
        Long id,
        String reference,
        Long rentalId,
        String status,
        String returnStationName,
        Instant returnedAt,
        Integer durationMinutes,
        BigDecimal finalCost,
        List<ImageResponse> images
) {}
