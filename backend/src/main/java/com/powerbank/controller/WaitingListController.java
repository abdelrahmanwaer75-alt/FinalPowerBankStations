package com.powerbank.controller;

import com.powerbank.dto.JoinWaitlistRequest;
import com.powerbank.dto.WaitingListResponse;
import com.powerbank.security.CurrentUser;
import com.powerbank.service.WaitingListService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/waitlist")
public class WaitingListController {

    private final WaitingListService waitingList;

    public WaitingListController(WaitingListService waitingList) {
        this.waitingList = waitingList;
    }

    @PostMapping
    ResponseEntity<WaitingListResponse> join(Authentication auth, @Valid @RequestBody JoinWaitlistRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(waitingList.join(CurrentUser.id(auth), req.stationId()));
    }

    @GetMapping
    List<WaitingListResponse> mine(Authentication auth) {
        return waitingList.mine(CurrentUser.id(auth));
    }

    @DeleteMapping("/{id}")
    WaitingListResponse leave(Authentication auth, @PathVariable Long id) {
        return waitingList.leave(CurrentUser.id(auth), id);
    }
}
