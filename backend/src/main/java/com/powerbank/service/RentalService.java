package com.powerbank.service;

import com.powerbank.dto.RentalResponse;
import com.powerbank.entity.*;
import com.powerbank.exception.ApiException;
import com.powerbank.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static com.powerbank.service.ReservationService.conflict;
import static com.powerbank.service.ReservationService.ref;

@Service
@Transactional
public class RentalService {

    private final ReservationRepository reservations;
    private final RentalRepository rentals;
    private final PricingPlanRepository plans;
    private final RentalPricingSnapshotRepository snapshots;

    public RentalService(ReservationRepository reservations, RentalRepository rentals,
                         PricingPlanRepository plans, RentalPricingSnapshotRepository snapshots) {
        this.reservations = reservations;
        this.rentals = rentals;
        this.plans = plans;
        this.snapshots = snapshots;
    }

    public RentalResponse start(long userId, long reservationId) {
        Reservation r = reservations.findById(reservationId)
                .filter(x -> x.getUser().getId() == userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "RESERVATION_NOT_FOUND", "Reservation not found"));

        if (r.getStatus() == ReservationStatus.PENDING && r.getExpiresAt().isBefore(Instant.now())) {
            r.setStatus(ReservationStatus.EXPIRED);
            if (r.getPowerBank() != null && r.getPowerBank().getStatus() == PowerBankStatus.RESERVED) {
                r.getPowerBank().setStatus(PowerBankStatus.AVAILABLE);
            }
            throw conflict("RESERVATION_EXPIRED", "This reservation expired. Reserve again.");
        }
        if (r.getStatus() != ReservationStatus.PENDING) {
            throw conflict("RESERVATION_NOT_ACTIVE", "This reservation can no longer be started");
        }
        if (rentals.existsByUserIdAndStatus(userId, RentalStatus.ACTIVE)) {
            throw conflict("ACTIVE_RENTAL_EXISTS", "You already have an active rental");
        }

        PricingPlan plan = plans.findFirstByActiveTrueOrderByIdDesc()
                .orElseThrow(() -> new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "NO_PRICING_PLAN",
                        "No pricing plan is active. Ask an administrator."));

        // The reservation becomes "ACTIVE" (in use). The device moves from RESERVED to RENTED.
        r.setStatus(ReservationStatus.ACTIVE);
        PowerBank bank = r.getPowerBank();
        bank.setStatus(PowerBankStatus.RENTED);

        Instant now = Instant.now();
        Rental rental = new Rental(ref("RNT"), r.getUser(), bank, r.getStation(), r, plan, now);
        rentals.save(rental);
        snapshots.save(new RentalPricingSnapshot(rental, plan));

        return toResponse(rental, snapshotFor(rental));
    }

    @Transactional(readOnly = true)
    public List<RentalResponse> active(long userId) {
        return rentals.findByUserIdAndStatusOrderByStartTimeDesc(userId, RentalStatus.ACTIVE).stream()
                .map(r -> toResponse(r, snapshotFor(r)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RentalResponse> history(long userId) {
        return rentals.findByUserIdOrderByStartTimeDesc(userId).stream()
                .map(r -> toResponse(r, snapshotFor(r)))
                .toList();
    }

    @Transactional(readOnly = true)
    public RentalResponse get(long userId, long id) {
        Rental r = rentals.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "RENTAL_NOT_FOUND", "Rental not found"));
        return toResponse(r, snapshotFor(r));
    }

    RentalPricingSnapshot snapshotFor(Rental r) {
        return snapshots.findByRentalId(r.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "SNAPSHOT_MISSING",
                        "Pricing record is missing for this rental"));
    }

    RentalResponse toResponse(Rental r, RentalPricingSnapshot snap) {
        boolean finished = r.getStatus() == RentalStatus.COMPLETED;
        long minutes = finished
                ? r.getDurationMinutes()
                : minutesBetween(r.getStartTime(), Instant.now());

        BigDecimal cost = finished
                ? r.getFinalCost()
                : PricingCalculator.cost(snap.getBaseFee(), snap.getPricePerHour(),
                        snap.getDailyMaxFee(), snap.getFreeMinutes(), minutes);

        return new RentalResponse(
                r.getId(), r.getReference(), r.getStatus().name(),
                r.getPickupStation().getName(), r.getPowerBank().getSerialNumber(),
                snap.getPlanName(), snap.getPricePerHour(),
                r.getStartTime(), r.getEndTime(), minutes, cost, finished);
    }

    /** Whole minutes, rounded up, with a minimum of one. */
    static long minutesBetween(Instant from, Instant to) {
        long seconds = Math.max(0, Duration.between(from, to).getSeconds());
        return Math.max(1, (seconds + 59) / 60);
    }
}
