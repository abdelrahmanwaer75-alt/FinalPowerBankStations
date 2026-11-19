package com.powerbank.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record AddPowerBanksRequest(@Min(1) @Max(50) int count) {}
