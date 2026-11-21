package com.powerbank.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record IdentifierRequest(@NotBlank @Size(max = 190) String identifier) {}
