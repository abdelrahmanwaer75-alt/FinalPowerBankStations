package com.powerbank.dto;

import java.time.Instant;

public record PowerBankStatusEventResponse(
        Long id,
        Long powerBankId,
        String oldStatus,
        String newStatus,
        Instant changedAt) {}
