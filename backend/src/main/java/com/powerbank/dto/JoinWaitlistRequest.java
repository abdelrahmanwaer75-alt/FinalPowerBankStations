package com.powerbank.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record JoinWaitlistRequest(@NotNull @Positive Long stationId) {}
