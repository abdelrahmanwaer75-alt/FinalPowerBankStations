package com.powerbank.controller;

import com.powerbank.admin.AdminAccess;
import com.powerbank.admin.AdminAccountService;
import com.powerbank.admin.AdminStationService;
import com.powerbank.admin.AuditService;
import com.powerbank.admin.PricingAdminService;
import com.powerbank.admin.PowerBankStatusHistoryService;
import com.powerbank.admin.ReturnReviewService;
import com.powerbank.dto.*;
import com.powerbank.entity.ReturnStatus;
import com.powerbank.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Staff endpoints. Each method checks the caller's role on the server, then records the action in audit_logs. */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminAccess access;
    private final ReturnReviewService reviews;
    private final AdminStationService stations;
    private final PricingAdminService pricing;
    private final AuditService audit;
    private final AdminAccountService adminAccounts;
    private final PowerBankStatusHistoryService statusHistory;

    public AdminController(AdminAccess access, ReturnReviewService reviews, AdminStationService stations,
                           PricingAdminService pricing, AuditService audit, AdminAccountService adminAccounts,
                           PowerBankStatusHistoryService statusHistory) {
        this.access = access;
        this.reviews = reviews;
        this.stations = stations;
        this.pricing = pricing;
        this.audit = audit;
        this.adminAccounts = adminAccounts;
        this.statusHistory = statusHistory;
    }

    private long staff(Authentication auth) {
        long id = CurrentUser.id(auth);
        access.requireStaff(id);
        return id;
    }

    private long superAdmin(Authentication auth) {
        long id = CurrentUser.id(auth);
        access.requireSuperAdmin(id);
        return id;
    }

    /* ---- staff account requests ---- */

    @GetMapping("/account-requests")
    AdminAccountRequestsResponse accountRequests(Authentication auth) {
        return adminAccounts.list(staff(auth));
    }

    @PostMapping("/account-requests")
    ResponseEntity<AdminAccountRequestRow> createAccountRequest(Authentication auth,
            @Valid @RequestBody AdminAccountRequestCreate request) {
        long actor = staff(auth);
        AdminAccountRequestRow created = adminAccounts.create(actor, request);
        audit.success(actor, "STAFF_ACCOUNT_REQUESTED", "admin_account_request", created.id(), null);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/account-requests/{id}/approve")
    AdminAccountRequestRow approveAccountRequest(Authentication auth, @PathVariable Long id,
            @Valid @RequestBody(required = false) ReviewRequest request) {
        long actor = superAdmin(auth);
        AdminAccountRequestRow approved = adminAccounts.approve(id, actor, request == null ? null : request.notes());
        audit.success(actor, "STAFF_ACCOUNT_APPROVED", "admin_account_request", id, null);
        return approved;
    }

    @PostMapping("/account-requests/{id}/reject")
    AdminAccountRequestRow rejectAccountRequest(Authentication auth, @PathVariable Long id,
            @Valid @RequestBody(required = false) ReviewRequest request) {
        long actor = superAdmin(auth);
        AdminAccountRequestRow rejected = adminAccounts.reject(id, actor, request == null ? null : request.notes());
        audit.success(actor, "STAFF_ACCOUNT_REJECTED", "admin_account_request", id, null);
        return rejected;
    }

    /* ---- returns ---- */

    @GetMapping("/returns")
    List<AdminReturnRow> returnQueue(Authentication auth,
                                     @RequestParam(defaultValue = "PENDING_REVIEW") ReturnStatus status) {
        staff(auth);
        return reviews.queue(status);
    }

    @PostMapping("/returns/{id}/approve")
    ResponseEntity<Void> approve(Authentication auth, @PathVariable Long id,
                                 @Valid @RequestBody(required = false) ReviewRequest req) {
        long actor = staff(auth);
        reviews.approve(actor, id, req == null ? null : req.notes());
        audit.success(actor, "RETURN_APPROVED", "return", id, null);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/returns/{id}/reject")
    ResponseEntity<Void> reject(Authentication auth, @PathVariable Long id, @Valid @RequestBody ReviewRequest req) {
        long actor = staff(auth);
        reviews.reject(actor, id, req.notes());
        audit.success(actor, "RETURN_REJECTED", "return", id, "{\"reason\":\"" + safe(req.notes()) + "\"}");
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/returns/{id}/manual-review")
    ResponseEntity<Void> manualReview(Authentication auth, @PathVariable Long id,
                                      @Valid @RequestBody(required = false) ReviewRequest req) {
        long actor = staff(auth);
        reviews.requestManualReview(actor, id, req == null ? null : req.notes());
        audit.success(actor, "RETURN_MANUAL_REVIEW", "return", id, null);
        return ResponseEntity.noContent().build();
    }

    /* ---- stations and devices ---- */

    @GetMapping("/stations")
    List<StationResponse> allStations(Authentication auth) {
        staff(auth);
        return stations.all();
    }

    @PostMapping("/stations")
    ResponseEntity<StationResponse> createStation(Authentication auth, @Valid @RequestBody StationUpsertRequest req) {
        long actor = staff(auth);
        StationResponse created = stations.create(req);
        audit.success(actor, "STATION_CREATED", "station", created.id(), null);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/stations/{id}")
    StationResponse updateStation(Authentication auth, @PathVariable Long id,
                                  @Valid @RequestBody StationUpsertRequest req) {
        long actor = staff(auth);
        StationResponse updated = stations.update(id, req);
        audit.success(actor, "STATION_UPDATED", "station", id, null);
        return updated;
    }

    @PostMapping("/stations/{id}/powerbanks")
    StationResponse addPowerBanks(Authentication auth, @PathVariable Long id,
                                  @Valid @RequestBody AddPowerBanksRequest req) {
        long actor = staff(auth);
        StationResponse updated = stations.addPowerBanks(id, req);
        audit.success(actor, "POWERBANKS_ADDED", "station", id, "{\"count\":" + req.count() + "}");
        return updated;
    }

    @GetMapping("/powerbank-status-history")
    List<PowerBankStatusEventResponse> powerBankStatusHistory(Authentication auth,
            @RequestParam(required = false) Long powerBankId) {
        staff(auth);
        return statusHistory.list(powerBankId);
    }

    /* ---- pricing ---- */

    @GetMapping("/pricing")
    List<PricingResponse> plans(Authentication auth) {
        staff(auth);
        return pricing.list();
    }

    @PostMapping("/pricing")
    ResponseEntity<PricingResponse> createPlan(Authentication auth, @Valid @RequestBody PricingUpsertRequest req) {
        long actor = staff(auth);
        PricingResponse created = pricing.create(req);
        audit.success(actor, "PRICING_CREATED", "pricing_plan", req.name(), null);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/pricing/{id}/activate")
    PricingResponse activatePlan(Authentication auth, @PathVariable Integer id) {
        long actor = staff(auth);
        PricingResponse active = pricing.activate(id);
        audit.success(actor, "PRICING_ACTIVATED", "pricing_plan", id, null);
        return active;
    }

    private static String safe(String text) {
        return text == null ? "" : text.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
