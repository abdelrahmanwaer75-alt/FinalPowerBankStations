package com.powerbank.controller;

import com.powerbank.dto.RentalResponse;
import com.powerbank.dto.StartRentalRequest;
import com.powerbank.security.CurrentUser;
import com.powerbank.service.RentalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rentals")
public class RentalController {

    private final RentalService rentals;

    public RentalController(RentalService rentals) {
        this.rentals = rentals;
    }

    @PostMapping("/start")
    ResponseEntity<RentalResponse> start(Authentication auth, @Valid @RequestBody StartRentalRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rentals.start(CurrentUser.id(auth), req.reservationId()));
    }

    @GetMapping
    List<RentalResponse> history(Authentication auth) {
        return rentals.history(CurrentUser.id(auth));
    }

    @GetMapping("/active")
    List<RentalResponse> active(Authentication auth) {
        return rentals.active(CurrentUser.id(auth));
    }

    @GetMapping("/{id}")
    RentalResponse get(Authentication auth, @PathVariable Long id) {
        return rentals.get(CurrentUser.id(auth), id);
    }
}
