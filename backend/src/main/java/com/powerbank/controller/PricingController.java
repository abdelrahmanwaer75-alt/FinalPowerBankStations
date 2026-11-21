package com.powerbank.controller;

import com.powerbank.dto.PricingResponse;
import com.powerbank.entity.PricingPlan;
import com.powerbank.exception.ApiException;
import com.powerbank.repository.PricingPlanRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public: the rental price shown to customers before they reserve. */
@RestController
@RequestMapping("/api/pricing")
public class PricingController {

    private final PricingPlanRepository plans;

    public PricingController(PricingPlanRepository plans) {
        this.plans = plans;
    }

    @GetMapping("/active")
    PricingResponse active() {
        PricingPlan p = plans.findFirstByActiveTrueOrderByIdDesc()
                .orElseThrow(() -> new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "NO_PRICING_PLAN",
                        "No pricing plan is active"));
        return new PricingResponse(p.getId(), p.getName(), p.getBaseFee(), p.getPricePerMinute(), p.getDailyMaxFee(),
                p.getFreeMinutes(), p.getLateFee(), p.getReservationFee(), p.isActive());
    }
}
