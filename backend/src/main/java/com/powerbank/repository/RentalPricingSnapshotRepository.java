package com.powerbank.repository;

import com.powerbank.entity.RentalPricingSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RentalPricingSnapshotRepository extends JpaRepository<RentalPricingSnapshot, Long> {
    Optional<RentalPricingSnapshot> findByRentalId(Long rentalId);
}
