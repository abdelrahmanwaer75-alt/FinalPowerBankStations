package com.powerbank.dto;

import jakarta.validation.constraints.NotNull;

public record StartRentalRequest(@NotNull Long reservationId) {}
