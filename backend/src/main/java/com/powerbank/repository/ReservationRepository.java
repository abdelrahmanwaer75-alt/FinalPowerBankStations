package com.powerbank.repository;

import com.powerbank.entity.Reservation;
import com.powerbank.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    Optional<Reservation> findByReference(String reference);
    boolean existsByUserIdAndStatusIn(Long userId, Collection<ReservationStatus> statuses);
    List<Reservation> findByUserIdAndStatusIn(Long userId, Collection<ReservationStatus> statuses);
        List<Reservation> findByStatusAndExpiresAtBefore(ReservationStatus status, Instant before);
    List<Reservation> findByUserIdAndStatusAndExpiresAtBefore(Long userId, ReservationStatus status, Instant before);
}
