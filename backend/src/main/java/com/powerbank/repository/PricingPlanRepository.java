package com.powerbank.repository;

import com.powerbank.entity.PricingPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PricingPlanRepository extends JpaRepository<PricingPlan, Integer> {
    Optional<PricingPlan> findFirstByActiveTrueOrderByIdDesc();
}
