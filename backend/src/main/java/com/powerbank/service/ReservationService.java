package com.powerbank.service;

import com.powerbank.dto.ReservationResponse;
import com.powerbank.entity.*;
import com.powerbank.exception.ApiException;
import com.powerbank.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ReservationService {

    public static final int HOLD_MINUTES = 15;
    static final int MIN_MINUTES = 5;
    static final int MAX_MINUTES = 120;
    private final ReservationRepository reservations;
    private final StationRepository stations;
    private final PowerBankRepository powerBanks;
    private final UserAccountRepository users;
    private final WaitingListService waitingList;

    public ReservationService(ReservationRepository reservations, StationRepository stations,
                              PowerBankRepository powerBanks,
                              UserAccountRepository users, WaitingListService waitingList) {
        this.reservations = reservations;
        this.stations = stations;
        this.powerBanks = powerBanks;
        this.users = users;
        this.waitingList = waitingList;
    }

    public ReservationResponse create(long userId, long stationId, int durationMinutes) {
        if (durationMinutes < MIN_MINUTES || durationMinutes > MAX_MINUTES) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_DURATION",
                    "Choose between " + MIN_MINUTES + " and " + MAX_MINUTES + " minutes");
        }
        expireDueFor(userId);

        UserAccount user = users.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "NOT_AUTHENTICATED", "Please sign in first"));

        Station station = stations.findById(stationId)
                .filter(s -> s.getStatus() == StationStatus.ACTIVE)
                .orElseThrow(() -> conflict("STATION_UNAVAILABLE", "This station is not operating right now"));

        // The row is locked here, so two customers cannot take the same device.
        PowerBank bank = powerBanks
                .findFirstByStationIdAndStatusOrderBySerialNumberAsc(stationId, PowerBankStatus.AVAILABLE)
                .orElseThrow(() -> conflict("NO_POWER_BANK_AVAILABLE", "No PowerBank is available at this station"));

        bank.setStatus(PowerBankStatus.RESERVED);

        Instant now = Instant.now();
        Reservation reservation = new Reservation(
                ref("RSV"), user, station, bank, durationMinutes, now.plus(Duration.ofMinutes(durationMinutes)));
        reservations.save(reservation);
        waitingList.fulfill(userId, stationId);
        waitingList.reconcileAvailableStock(stationId);
        return toResponse(reservation);
    }

    /** The customer's current hold, if any. Expired holds are released first. */
    public java.util.List<ReservationResponse> activeHolds(long userId) {
        expireDueFor(userId);
        return reservations.findByUserIdAndStatusIn(userId, java.util.List.of(ReservationStatus.PENDING))
                .stream().map(this::toResponse).toList();
    }

    public ReservationResponse get(long userId, long id) {
        Reservation r = ownedOrNotFound(userId, id);
        expireIfDue(r);
        return toResponse(r);
    }

    public ReservationResponse cancel(long userId, long id) {
        Reservation r = ownedOrNotFound(userId, id);
        if (r.getStatus() != ReservationStatus.PENDING && r.getStatus() != ReservationStatus.CONFIRMED) {
            throw conflict("RESERVATION_NOT_CANCELLABLE", "Only a held reservation can be cancelled");
        }
        r.setStatus(ReservationStatus.CANCELLED);
        r.setCancelledAt(Instant.now());
        releaseBank(r);
        return toResponse(r);
    }

    /** Runs every minute. Holds that passed their expiry go back to the pool. */
    @Scheduled(fixedDelayString = "PT1M")
    public void expireAllDue() {
        for (Reservation r : reservations.findByStatusAndExpiresAtBefore(ReservationStatus.PENDING, Instant.now())) {
            expire(r);
        }
    }

    private void expireDueFor(long userId) {
        for (Reservation r : reservations.findByUserIdAndStatusAndExpiresAtBefore(
                userId, ReservationStatus.PENDING, Instant.now())) {
            expire(r);
        }
    }

    private void expireIfDue(Reservation r) {
        if (r.getStatus() == ReservationStatus.PENDING && r.getExpiresAt().isBefore(Instant.now())) {
            expire(r);
        }
    }

    private void expire(Reservation r) {
        r.setStatus(ReservationStatus.EXPIRED);
        releaseBank(r);
    }

    private void releaseBank(Reservation r) {
        PowerBank bank = r.getPowerBank();
        if (bank != null && bank.getStatus() == PowerBankStatus.RESERVED) {
            bank.setStatus(PowerBankStatus.AVAILABLE);
            if (bank.getStation() != null) waitingList.promoteForAvailableStock(bank.getStation().getId(), 1);
        }
    }

    private Reservation ownedOrNotFound(long userId, long id) {
        // Returns 404 for other people's reservations too, so ids cannot be probed.
        return reservations.findById(id)
                .filter(r -> r.getUser().getId() == userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "RESERVATION_NOT_FOUND", "Reservation not found"));
    }

    private ReservationResponse toResponse(Reservation r) {
        return new ReservationResponse(
                r.getId(), r.getReference(), r.getStatus().name(),
                r.getStation().getId(), r.getStation().getName(), r.getStation().getAddress(),
                r.getPowerBank() == null ? null : r.getPowerBank().getSerialNumber(),
                r.getDurationMinutes(), r.getExpiresAt());
    }

    static ApiException conflict(String code, String message) {
        return new ApiException(HttpStatus.CONFLICT, code, message);
    }

    static String ref(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
    }
}
