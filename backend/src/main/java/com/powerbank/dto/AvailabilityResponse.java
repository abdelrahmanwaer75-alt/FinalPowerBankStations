package com.powerbank.dto;

public record AvailabilityResponse(Long stationId, int totalCapacity, long available, long rented, long returnSlotsLeft) {}
