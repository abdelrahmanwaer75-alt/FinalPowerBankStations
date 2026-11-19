package com.powerbank.dto;

import java.time.Instant;

public record AdminReturnRow(
        Long id, String reference, String status, Long rentalId, String powerBankSerial,
        String returnStation, Instant returnedAt, Integer durationMinutes, String finalCost, int photoCount
) {}
