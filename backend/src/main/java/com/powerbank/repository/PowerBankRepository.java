package com.powerbank.repository;

import com.powerbank.entity.PowerBank;
import com.powerbank.entity.PowerBankStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PowerBankRepository extends JpaRepository<PowerBank, Long> {

    long countByStationIdAndStatus(Long stationId, PowerBankStatus status);

    /** Locks the row so two customers cannot reserve the same device at the same time. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PowerBank> findFirstByStationIdAndStatusOrderBySerialNumberAsc(Long stationId, PowerBankStatus status);

    /** Locks available rows before the inventory BST selects a unit. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<PowerBank> findByStationIdAndStatusOrderByIdAsc(Long stationId, PowerBankStatus status);

    @Query("select p.station.id, count(p) from PowerBank p where p.status = :status group by p.station.id")
    List<Object[]> countAvailableGroupedByStation(@Param("status") PowerBankStatus status);

    /** Devices physically docked at a station. They count against the station's capacity. */
    @Query("select count(p) from PowerBank p where p.station.id = :stationId and p.status in :statuses")
    long countDockedAt(@Param("stationId") Long stationId, @Param("statuses") Collection<PowerBankStatus> statuses);
}
