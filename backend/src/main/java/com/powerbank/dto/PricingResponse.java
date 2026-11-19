package com.powerbank.dto;

import java.math.BigDecimal;

public record PricingResponse(
        String name,
        BigDecimal baseFee,
        BigDecimal pricePerHour,
        BigDecimal dailyMaxFee,
        int freeMinutes,
        BigDecimal lateFee,
        BigDecimal reservationFee
) {}
