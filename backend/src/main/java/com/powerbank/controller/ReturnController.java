package com.powerbank.controller;

import com.powerbank.dto.CreateReturnRequest;
import com.powerbank.dto.ImageResponse;
import com.powerbank.dto.ReturnResponse;
import com.powerbank.security.CurrentUser;
import com.powerbank.service.ReturnImageService;
import com.powerbank.service.ReturnService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/returns")
public class ReturnController {

    private final ReturnService returns;
    private final ReturnImageService images;

    public ReturnController(ReturnService returns, ReturnImageService images) {
        this.returns = returns;
        this.images = images;
    }

    @PostMapping
    ResponseEntity<ReturnResponse> create(Authentication auth, @Valid @RequestBody CreateReturnRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(returns.create(CurrentUser.id(auth), req.rentalId(), req.returnStationId()));
    }

    @GetMapping("/{id}")
    ReturnResponse get(Authentication auth, @PathVariable Long id) {
        return returns.get(CurrentUser.id(auth), id);
    }

    @GetMapping("/{id}/status")
    Map<String, String> status(Authentication auth, @PathVariable Long id) {
        return Map.of("status", returns.status(CurrentUser.id(auth), id));
    }

    @PostMapping(value = "/{id}/images", consumes = "multipart/form-data")
    ResponseEntity<List<ImageResponse>> upload(Authentication auth, @PathVariable Long id,
                                               @RequestParam("files") List<MultipartFile> files) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(images.upload(CurrentUser.id(auth), id, files));
    }
}
