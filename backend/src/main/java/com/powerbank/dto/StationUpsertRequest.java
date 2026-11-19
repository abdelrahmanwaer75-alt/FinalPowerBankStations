package com.powerbank.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record StationUpsertRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 255) String address,
        @NotBlank @Size(max = 100) String city,
        @NotNull @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
        @NotNull @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude,
        @Min(1) @Max(500) int totalCapacity,
        @Size(max = 120) String openingHours,
        @Size(max = 20) String contactPhone,
        @Size(max = 2000) String description,
        @NotNull String status
) {}
