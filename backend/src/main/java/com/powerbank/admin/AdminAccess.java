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
        if (!hasStaffRole(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "NOT_AUTHORIZED", "Staff access is required");
        }
    }

    public boolean isSuperAdmin(long userId) {
        Number count = roles.countSuperAdminRoles(userId);
        return count != null && count.longValue() > 0;
    }

    public void requireSuperAdmin(long userId) {
        if (!isSuperAdmin(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "SUPER_ADMIN_REQUIRED",
                    "Only the Super Admin can approve staff account requests");
        }
    }

    private boolean hasStaffRole(long userId) {
        Number count = roles.countStaffRoles(userId);
        return count != null && count.longValue() > 0;
    }
}
