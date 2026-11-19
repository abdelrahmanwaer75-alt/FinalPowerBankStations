package com.powerbank.controller;

import com.powerbank.dto.ChangePasswordRequest;
import com.powerbank.dto.CustomerProfileResponse;
import com.powerbank.dto.UpdateProfileRequest;
import com.powerbank.security.CurrentUser;
import com.powerbank.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers/me")
public class CustomerController {

    private final CustomerService customers;

    public CustomerController(CustomerService customers) {
        this.customers = customers;
    }

    @GetMapping
    CustomerProfileResponse me(Authentication auth) {
        return customers.me(CurrentUser.id(auth));
    }

    @PutMapping
    CustomerProfileResponse update(Authentication auth, @Valid @RequestBody UpdateProfileRequest req) {
        return customers.update(CurrentUser.id(auth), req);
    }

    @PostMapping("/password")
    void changePassword(Authentication auth, @Valid @RequestBody ChangePasswordRequest req) {
        customers.changePassword(CurrentUser.id(auth), req);
    }
}
