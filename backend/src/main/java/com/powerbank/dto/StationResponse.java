package com.powerbank.dto;

import java.math.BigDecimal;

public record StationResponse(
        Long id,
        String name,
        String address,
        String city,
        BigDecimal latitude,
        BigDecimal longitude,
        int totalCapacity,
        long availableCount,
        String status,
        String openingHours,
        String contactPhone,
        String description
) {}
