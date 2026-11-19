package com.powerbank.dto;

import jakarta.validation.constraints.NotNull;

/** durationMinutes is optional. The server uses 15 when it is missing. Allowed range: 5 to 120. */
public record CreateReservationRequest(@NotNull Long stationId, Integer durationMinutes) {}
