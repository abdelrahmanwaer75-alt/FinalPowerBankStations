package com.powerbank.security;

import com.powerbank.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

/** The authenticated user id, set by JwtAuthFilter as the principal. */
public final class CurrentUser {
    private CurrentUser() {}

    public static long id(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof Long id)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "NOT_AUTHENTICATED", "Please sign in first");
        }
        return id;
    }
}
