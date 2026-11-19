package com.powerbank.service;

import com.powerbank.dto.ChangePasswordRequest;
import com.powerbank.dto.CustomerProfileResponse;
import com.powerbank.dto.UpdateProfileRequest;
import com.powerbank.entity.UserAccount;
import com.powerbank.exception.ApiException;
import com.powerbank.repository.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CustomerService {

    private final UserAccountRepository users;
    private final PasswordEncoder encoder;

    public CustomerService(UserAccountRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @Transactional(readOnly = true)
    public CustomerProfileResponse me(long userId) {
        return toResponse(load(userId));
    }

    public CustomerProfileResponse update(long userId, UpdateProfileRequest req) {
        UserAccount u = load(userId);
        String phone = req.phone() == null || req.phone().isBlank() ? null : req.phone().trim();
        if (phone != null && !phone.equals(u.getPhone()) && users.existsByPhone(phone)) {
            throw new ApiException(HttpStatus.CONFLICT, "PHONE_TAKEN", "This phone number is already registered");
        }
        u.setFullName(req.fullName().trim());
        u.setPhone(phone);
        if (req.languagePreference() != null) u.setLanguagePreference(req.languagePreference());
        if (req.themePreference() != null) u.setThemePreference(req.themePreference());
        return toResponse(u);
    }

    /** Checks the current password first. Sessions are stateless, so existing tokens stay valid until they expire. */
    public void changePassword(long userId, ChangePasswordRequest req) {
        UserAccount u = load(userId);
        if (!encoder.matches(req.currentPassword(), u.getPasswordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "WRONG_PASSWORD", "The current password is incorrect");
        }
        if (req.currentPassword().equals(req.newPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "SAME_PASSWORD", "Choose a password you have not used before");
        }
        u.setPasswordHash(encoder.encode(req.newPassword()));
    }

    private UserAccount load(long userId) {
        return users.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "NOT_AUTHENTICATED", "Please sign in first"));
    }

    private CustomerProfileResponse toResponse(UserAccount u) {
        String lang = u.getLanguagePreference() == null ? "EN" : u.getLanguagePreference();
        String theme = u.getThemePreference() == null ? "LIGHT" : u.getThemePreference();
        return new CustomerProfileResponse(u.getId(), u.getFullName(), u.getEmail(), u.getPhone(),
                lang, theme, u.getCreatedAt());
    }
}
