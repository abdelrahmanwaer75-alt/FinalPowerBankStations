package com.powerbank.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Reads the role links from the schema (user_roles and roles). */
public interface AdminRoleRepository extends Repository<com.powerbank.entity.UserAccount, Long> {
    @Query(value = "select count(*) from user_roles ur join roles r on r.id = ur.role_id " +
                   "where ur.user_id = :userId and r.is_enabled = 1 " +
                   "and r.name in ('SUPER_ADMIN','ADMIN','STATION_MANAGER')", nativeQuery = true)
    Number countStaffRoles(@Param("userId") long userId);
}
