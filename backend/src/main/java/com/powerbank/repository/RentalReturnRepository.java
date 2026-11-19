package com.powerbank.repository;

import com.powerbank.entity.RentalReturn;
import com.powerbank.entity.ReturnStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RentalReturnRepository extends JpaRepository<RentalReturn, Long> {
    Optional<RentalReturn> findByIdAndRentalUserId(Long id, Long userId);
    List<RentalReturn> findByStatusOrderByReturnedAtAsc(ReturnStatus status);
}
