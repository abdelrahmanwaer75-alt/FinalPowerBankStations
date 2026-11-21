package com.powerbank.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record PricingUpsertRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull @DecimalMin("0") @Digits(integer = 8, fraction = 2) BigDecimal baseFee,
        @NotNull @DecimalMin("0") @Digits(integer = 4, fraction = 6) BigDecimal pricePerMinute,
        @DecimalMin("0") @Digits(integer = 8, fraction = 2) BigDecimal dailyMaxFee,
        @Min(0) @Max(1440) int freeMinutes,
        @NotNull @DecimalMin("0") @Digits(integer = 8, fraction = 2) BigDecimal lateFee,
        @NotNull @DecimalMin("0") @Digits(integer = 8, fraction = 2) BigDecimal reservationFee
) {}
