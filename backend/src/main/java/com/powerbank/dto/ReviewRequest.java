package com.powerbank.dto;

import jakarta.validation.constraints.Size;

public record ReviewRequest(@Size(max = 500) String notes) {}
