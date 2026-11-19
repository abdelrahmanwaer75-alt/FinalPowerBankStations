package com.powerbank.admin;

import com.powerbank.exception.ApiException;
import com.powerbank.repository.AdminRoleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** Every admin action calls requireStaff first. The check is on the server, not only in the page. */
@Component
public class AdminAccess {

    private final AdminRoleRepository roles;

    public AdminAccess(AdminRoleRepository roles) {
        this.roles = roles;
    }

    public void requireStaff(long userId) {
        Number n = roles.countStaffRoles(userId);
        if (n == null || n.longValue() == 0) {
            throw new ApiException(HttpStatus.FORBIDDEN, "NOT_AUTHORIZED", "Staff access is required");
        }
    }
}
