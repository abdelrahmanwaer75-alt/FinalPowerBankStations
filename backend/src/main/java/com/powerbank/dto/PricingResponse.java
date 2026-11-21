package com.powerbank.dto;

import java.math.BigDecimal;

public record PricingResponse(
        Integer id,
        String name,
        BigDecimal baseFee,
        BigDecimal pricePerMinute,
        BigDecimal dailyMaxFee,
        int freeMinutes,
        BigDecimal lateFee,
        BigDecimal reservationFee,
        boolean active
) {}
