package com.powerbank.repository;

import com.powerbank.entity.WaitingListEntry;
import com.powerbank.entity.WaitingListStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface WaitingListRepository extends JpaRepository<WaitingListEntry, Long> {
    Optional<WaitingListEntry> findByUserIdAndStationId(Long userId, Long stationId);
    List<WaitingListEntry> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<WaitingListEntry> findByStationIdAndStatusOrderByCreatedAtAsc(Long stationId, WaitingListStatus status);
    List<WaitingListEntry> findByStatusOrderByCreatedAtAsc(WaitingListStatus status);
    List<WaitingListEntry> findByStationIdAndStatusInOrderByCreatedAtAsc(
            Long stationId, Collection<WaitingListStatus> statuses);
}
