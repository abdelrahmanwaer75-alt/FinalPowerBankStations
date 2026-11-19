package com.powerbank.dto;

import jakarta.validation.constraints.NotNull;

public record CreateReturnRequest(@NotNull Long rentalId, @NotNull Long returnStationId) {}
