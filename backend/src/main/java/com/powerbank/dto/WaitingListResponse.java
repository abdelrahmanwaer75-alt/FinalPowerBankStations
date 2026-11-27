package com.powerbank.dto;

import java.time.Instant;

public record WaitingListResponse(
        Long id,
        Long stationId,
        String stationName,
        String stationAddress,
        String status,
        Integer queuePosition,
        Instant joinedAt
) {}
