package com.powerbank.repository;

import com.powerbank.entity.Rental;
import com.powerbank.entity.RentalStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RentalRepository extends JpaRepository<Rental, Long> {
    boolean existsByUserIdAndStatus(Long userId, RentalStatus status);
    List<Rental> findByUserIdAndStatusOrderByStartTimeDesc(Long userId, RentalStatus status);
    List<Rental> findByUserIdOrderByStartTimeDesc(Long userId);
    Optional<Rental> findByIdAndUserId(Long id, Long userId);
    Optional<Rental> findByReservationId(Long reservationId);
}
