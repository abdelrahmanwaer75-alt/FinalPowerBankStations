package com.powerbank.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Cost = base fee + price per billable minute after the free period, capped at the daily maximum.
 * Late fees are not applied yet: there is no overdue rule in the spec's current scope.
 */
public final class PricingCalculator {
    private PricingCalculator() {}

    public static BigDecimal cost(BigDecimal baseFee, BigDecimal pricePerMinute, BigDecimal dailyMaxFee,
                                  int freeMinutes, long minutes) {
        long billable = Math.max(0, minutes - freeMinutes);
        BigDecimal cost = baseFee.add(pricePerMinute.multiply(BigDecimal.valueOf(billable)));
        if (dailyMaxFee != null && cost.compareTo(dailyMaxFee) > 0) {
            cost = dailyMaxFee;
        }
        return cost.setScale(2, RoundingMode.HALF_UP);
    }
}
