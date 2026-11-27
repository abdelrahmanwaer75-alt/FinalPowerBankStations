package com.powerbank.service;

import com.powerbank.dto.RentalResponse;
import com.powerbank.entity.*;
import com.powerbank.exception.ApiException;
import com.powerbank.repository.*;
import datastructure.linkedlist.ActiveRentalList;
import datastructure.linkedlist.RentalHistoryList;
import datastructure.models.RentalRecord;
import datastructure.sorting.Sorting;
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
        List<Rental> rows = rentals.findByUserIdAndStatusOrderByStartTimeDesc(userId, RentalStatus.ACTIVE);
        java.util.Map<Long, Rental> byId = new java.util.HashMap<>();
        ActiveRentalList active = new ActiveRentalList();
        for (Rental row : rows) {
            byId.put(row.getId(), row);
            active.insert(toDataStructureRecord(row));
        }
        return java.util.Arrays.stream(active.toArray())
                .map(record -> byId.get(record.getId()))
                .filter(java.util.Objects::nonNull)
                .map(r -> toResponse(r, snapshotFor(r)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RentalResponse> history(long userId) {
        List<Rental> rows = rentals.findByUserIdOrderByStartTimeDesc(userId);
        java.util.Map<Long, Rental> byId = new java.util.HashMap<>();
        ActiveRentalList active = new ActiveRentalList();
        RentalHistoryList completed = new RentalHistoryList();
        for (Rental row : rows) {
            byId.put(row.getId(), row);
            RentalRecord record = toDataStructureRecord(row);
            if (row.getStatus() == RentalStatus.ACTIVE) active.insert(record);
            else completed.insert(record);
        }
        java.util.List<RentalRecord> ordered = new java.util.ArrayList<>();
        ordered.addAll(java.util.Arrays.asList(active.toArray()));
        ordered.addAll(java.util.Arrays.asList(completed.toArray()));
        RentalRecord[] chronological = ordered.toArray(RentalRecord[]::new);
        Sorting.sortRentalsByDate(chronological);
        ordered.clear();
        for (int i = chronological.length - 1; i >= 0; i--) ordered.add(chronological[i]);
        return ordered.stream().map(record -> byId.get(record.getId()))
                .filter(java.util.Objects::nonNull)
                .map(r -> toResponse(r, snapshotFor(r)))
                .toList();
    }

    private RentalRecord toDataStructureRecord(Rental rental) {
        return new RentalRecord(rental.getId(), rental.getUser().getId(), rental.getPowerBank().getId(),
                rental.getPickupStation().getId(), 0,
                rental.getStartTime() == null ? null : rental.getStartTime().toString(),
                rental.getEndTime() == null ? null : rental.getEndTime().toString(),
                rental.getFinalCost() == null ? 0 : rental.getFinalCost().doubleValue());
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
                : PricingCalculator.cost(snap.getBaseFee(), snap.getPricePerMinute(),
                        snap.getDailyMaxFee(), snap.getFreeMinutes(), minutes);

        return new RentalResponse(
                r.getId(), r.getReference(), r.getStatus().name(),
                r.getPickupStation().getName(), r.getPowerBank().getSerialNumber(),
                snap.getPlanName(), snap.getPricePerMinute(),
                r.getStartTime(), r.getEndTime(), minutes, cost, finished);
    }

    /** Whole minutes, rounded up, with a minimum of one. */
    static long minutesBetween(Instant from, Instant to) {
        long seconds = Math.max(0, Duration.between(from, to).getSeconds());
        return Math.max(1, (seconds + 59) / 60);
    }
}
