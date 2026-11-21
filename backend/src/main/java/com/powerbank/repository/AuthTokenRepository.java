package com.powerbank.repository;

import com.powerbank.entity.AuthToken;
import com.powerbank.entity.AuthTokenPurpose;
import com.powerbank.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthTokenRepository extends JpaRepository<AuthToken, Long> {
    Optional<AuthToken> findFirstByUserAndPurposeAndTokenHashAndUsedAtIsNullOrderByExpiresAtDesc(
            UserAccount user, AuthTokenPurpose purpose, String tokenHash);

    Optional<AuthToken> findFirstByUserAndPurposeOrderByCreatedAtDesc(
            UserAccount user, AuthTokenPurpose purpose);
}
