package com.powerbank.repository;

import com.powerbank.entity.AdminAccountRequest;
import com.powerbank.entity.AdminAccountRequestStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AdminAccountRequestRepository extends JpaRepository<AdminAccountRequest, Long> {
    boolean existsByEmailIgnoreCaseAndStatus(String email, AdminAccountRequestStatus status);
    List<AdminAccountRequest> findAllByRequestedBy_IdOrderByRequestedAtDescIdDesc(Long requestedById);
    List<AdminAccountRequest> findAllByOrderByRequestedAtDescIdDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select request from AdminAccountRequest request where request.id = :id")
    Optional<AdminAccountRequest> findLockedById(@Param("id") Long id);
}
