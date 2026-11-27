package com.powerbank.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminAccountRequestCreate(
        @NotBlank @Size(max = 120) String fullName,
        @NotBlank @Email @Size(max = 190) String email,
        @Size(max = 20) @Pattern(regexp = "^(?=(?:\\D*\\d){7,20}$)\\+?[0-9 ()-]{7,20}$", message = "Invalid phone number") String phone,
        @NotBlank @Pattern(regexp = "ADMIN|STATION_MANAGER", message = "Only ADMIN or STATION_MANAGER can be requested") String role
) { }
