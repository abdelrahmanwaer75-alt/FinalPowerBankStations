package com.powerbank.controller;

import com.powerbank.dto.CreateReservationRequest;
import com.powerbank.dto.ReservationResponse;
import com.powerbank.security.CurrentUser;
import com.powerbank.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservations;

    public ReservationController(ReservationService reservations) {
        this.reservations = reservations;
    }

    @PostMapping
    ResponseEntity<ReservationResponse> create(Authentication auth, @Valid @RequestBody CreateReservationRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reservations.create(CurrentUser.id(auth), req.stationId(),
                        req.durationMinutes() == null ? ReservationService.HOLD_MINUTES : req.durationMinutes()));
    }

    @GetMapping("/active")
    java.util.List<ReservationResponse> active(Authentication auth) {
        return reservations.activeHolds(CurrentUser.id(auth));
    }

    @GetMapping("/{id}")
    ReservationResponse get(Authentication auth, @PathVariable Long id) {
        return reservations.get(CurrentUser.id(auth), id);
    }

    /** Cancels a held reservation and returns the PowerBank to the pool. */
    @DeleteMapping("/{id}")
    ReservationResponse cancel(Authentication auth, @PathVariable Long id) {
        return reservations.cancel(CurrentUser.id(auth), id);
    }
}
