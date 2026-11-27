package com.powerbank.admin;

import com.powerbank.dto.AdminAccountRequestCreate;
import com.powerbank.dto.AdminAccountRequestRow;
import com.powerbank.dto.AdminAccountRequestsResponse;
import com.powerbank.entity.AccountStatus;
import com.powerbank.entity.AdminAccountRequest;
import com.powerbank.entity.AdminAccountRequestStatus;
import com.powerbank.entity.UserAccount;
import com.powerbank.exception.ApiException;
import com.powerbank.repository.AdminAccountRequestRepository;
import com.powerbank.repository.UserAccountRepository;
import com.powerbank.service.AuthService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AdminAccountService {

    private final AdminAccountRequestRepository requests;
    private final UserAccountRepository users;
    private final AdminAccess access;
    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;
    private final AuthService auth;

    public AdminAccountService(AdminAccountRequestRepository requests, UserAccountRepository users,
                               AdminAccess access, JdbcTemplate jdbc, PasswordEncoder passwordEncoder,
                               AuthService auth) {
        this.requests = requests;
        this.users = users;
        this.access = access;
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
        this.auth = auth;
    }

    @Transactional(readOnly = true)
    public AdminAccountRequestsResponse list(long actorId) {
        boolean canApprove = access.isSuperAdmin(actorId);
        List<AdminAccountRequest> entries = canApprove
                ? requests.findAllByOrderByRequestedAtDescIdDesc()
                : requests.findAllByRequestedBy_IdOrderByRequestedAtDescIdDesc(actorId);
        return new AdminAccountRequestsResponse(canApprove, entries.stream().map(this::toRow).toList());
    }

    @Transactional
    public AdminAccountRequestRow create(long actorId, AdminAccountRequestCreate request) {
        String email = request.email().trim().toLowerCase();
        String phone = request.phone() == null || request.phone().isBlank() ? null : request.phone().trim();
        String role = request.role().trim().toUpperCase();

        if (!role.equals("ADMIN") && !role.equals("STATION_MANAGER")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_STAFF_ROLE",
                    "New staff requests may only be ADMIN or STATION_MANAGER.");
        }
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_TAKEN", "An account already uses this email.");
        }
        if (phone != null && users.existsByPhone(phone)) {
            throw new ApiException(HttpStatus.CONFLICT, "PHONE_TAKEN", "An account already uses this phone number.");
        }
        if (requests.existsByEmailIgnoreCaseAndStatus(email, AdminAccountRequestStatus.PENDING)) {
            throw new ApiException(HttpStatus.CONFLICT, "ADMIN_REQUEST_EXISTS",
                    "There is already a pending staff account request for this email.");
        }

        UserAccount requester = requireUser(actorId);
        try {
            AdminAccountRequest created = requests.saveAndFlush(new AdminAccountRequest(
                    request.fullName().trim(), email, phone, role, requester));
            return toRow(created);
        } catch (DataIntegrityViolationException error) {
            throw new ApiException(HttpStatus.CONFLICT, "ADMIN_REQUEST_EXISTS",
                    "A pending staff account request already uses this email.");
        }
    }

    @Transactional
    public AdminAccountRequestRow approve(long requestId, long reviewerId, String notes) {
        AdminAccountRequest request = requests.findLockedById(requestId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ADMIN_REQUEST_NOT_FOUND",
                        "Staff account request not found."));
        if (request.getStatus() != AdminAccountRequestStatus.PENDING) {
            throw new ApiException(HttpStatus.CONFLICT, "ADMIN_REQUEST_ALREADY_REVIEWED",
                    "This staff account request has already been reviewed.");
        }
        if (users.existsByEmailIgnoreCase(request.getEmail())) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_TAKEN",
                    "An account with this email was created while the request was pending.");
        }
        if (request.getPhone() != null && users.existsByPhone(request.getPhone())) {
            throw new ApiException(HttpStatus.CONFLICT, "PHONE_TAKEN",
                    "An account with this phone number was created while the request was pending.");
        }

        UserAccount reviewer = requireUser(reviewerId);
        String unusableUntilPasswordReset = UUID.randomUUID().toString() + "-Aa1!";
        UserAccount account = new UserAccount(request.getFullName(), request.getEmail(), request.getPhone(),
                passwordEncoder.encode(unusableUntilPasswordReset));
        account.setAccountStatus(AccountStatus.ACTIVE);
        account.setEmailVerifiedAt(Instant.now());
        account = users.saveAndFlush(account);

        int linked = jdbc.update("""
                INSERT INTO user_roles (user_id, role_id, assigned_by)
                SELECT ?, id, ? FROM roles WHERE name = ? AND is_enabled = TRUE
                ON DUPLICATE KEY UPDATE assigned_by = VALUES(assigned_by), assigned_at = CURRENT_TIMESTAMP
                """, account.getId(), reviewerId, request.getRequestedRole());
        if (linked == 0) {
            throw new ApiException(HttpStatus.CONFLICT, "STAFF_ROLE_UNAVAILABLE",
                    "The requested staff role is not enabled in the database.");
        }

        request.approve(reviewer, account, notes == null || notes.isBlank() ? null : notes.trim());
        auth.issuePasswordSetupCode(account.getId());
        return toRow(request);
    }

    @Transactional
    public AdminAccountRequestRow reject(long requestId, long reviewerId, String notes) {
        AdminAccountRequest request = requests.findLockedById(requestId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ADMIN_REQUEST_NOT_FOUND",
                        "Staff account request not found."));
        if (request.getStatus() != AdminAccountRequestStatus.PENDING) {
            throw new ApiException(HttpStatus.CONFLICT, "ADMIN_REQUEST_ALREADY_REVIEWED",
                    "This staff account request has already been reviewed.");
        }
        request.reject(requireUser(reviewerId), notes == null || notes.isBlank() ? null : notes.trim());
        return toRow(request);
    }

    private UserAccount requireUser(long userId) {
        return users.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "Account not found."));
    }

    private AdminAccountRequestRow toRow(AdminAccountRequest request) {
        return new AdminAccountRequestRow(request.getId(), request.getFullName(), request.getEmail(),
                request.getPhone(), request.getRequestedRole(), request.getStatus().name(),
                request.getRequestedBy().getFullName() + " · " + request.getRequestedBy().getEmail(),
                request.getCreatedUser() == null ? null : request.getCreatedUser().getId(),
                request.getReviewNotes(), request.getRequestedAt(), request.getReviewedAt());
    }
}
