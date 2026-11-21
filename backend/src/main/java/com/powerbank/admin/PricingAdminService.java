package com.powerbank.admin;

import com.powerbank.dto.PricingResponse;
import com.powerbank.dto.PricingUpsertRequest;
import com.powerbank.entity.PricingPlan;
import com.powerbank.exception.ApiException;
import com.powerbank.repository.PricingPlanRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Pricing plans. Old plans are kept, so past rentals still show the price they were charged. */
@Service
@Transactional
public class PricingAdminService {

    private final PricingPlanRepository plans;

    public PricingAdminService(PricingPlanRepository plans) {
        this.plans = plans;
    }

    @Transactional(readOnly = true)
    public List<PricingResponse> list() {
        return plans.findAll().stream().map(this::toResponse).toList();
    }

    public PricingResponse create(PricingUpsertRequest req) {
        if (req.dailyMaxFee() != null && req.dailyMaxFee().compareTo(req.pricePerMinute()) < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PRICING",
                    "The daily maximum cannot be lower than the price per minute");
        }
        PricingPlan plan = new PricingPlan(req.name().trim(), req.baseFee(), req.pricePerMinute(),
                req.dailyMaxFee(), req.freeMinutes(), req.lateFee(), req.reservationFee());
        plans.save(plan);
        return toResponse(plan);
    }

    /** Makes one plan active and deactivates the rest, so exactly one plan is used for new rentals. */
    public PricingResponse activate(int id) {
        PricingPlan target = plans.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PLAN_NOT_FOUND", "Pricing plan not found"));
        for (PricingPlan p : plans.findAll()) {
            p.setActive(p.getId().equals(id));
        }
        return toResponse(target);
    }

    private PricingResponse toResponse(PricingPlan p) {
        return new PricingResponse(p.getId(), p.getName(), p.getBaseFee(), p.getPricePerMinute(), p.getDailyMaxFee(),
                p.getFreeMinutes(), p.getLateFee(), p.getReservationFee(), p.isActive());
    }
}
