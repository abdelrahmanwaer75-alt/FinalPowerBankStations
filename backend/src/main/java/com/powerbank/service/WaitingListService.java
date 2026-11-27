package com.powerbank.service;

import com.powerbank.dto.WaitingListResponse;
import com.powerbank.entity.*;
import com.powerbank.exception.ApiException;
import com.powerbank.repository.*;
import datastructure.bst.PowerBankBST;
import datastructure.models.WaitingRequest;
import datastructure.queue.GlobalWaitingQueue;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static com.powerbank.service.ReservationService.conflict;

@Service
@Transactional
public class WaitingListService {

    private static final Set<WaitingListStatus> OPEN = EnumSet.of(
            WaitingListStatus.WAITING, WaitingListStatus.NOTIFIED);

    private final WaitingListRepository entries;
    private final StationRepository stations;
    private final PowerBankRepository powerBanks;
    private final UserAccountRepository users;

    public WaitingListService(WaitingListRepository entries, StationRepository stations,
                              PowerBankRepository powerBanks, UserAccountRepository users) {
        this.entries = entries;
        this.stations = stations;
        this.powerBanks = powerBanks;
        this.users = users;
    }

    public WaitingListResponse join(long userId, long stationId) {
        Station station = stations.findById(stationId)
                .filter(s -> s.getStatus() == StationStatus.ACTIVE)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "STATION_UNAVAILABLE",
                        "This station is not operating right now"));

        if (powerBanks.countByStationIdAndStatus(stationId, PowerBankStatus.AVAILABLE) > 0) {
            throw conflict("POWERBANKS_AVAILABLE_NOW", "A PowerBank is available at this station. Reserve it now instead of joining the waitlist.");
        }

        Instant now = Instant.now();
        WaitingListEntry entry = entries.findByUserIdAndStationId(userId, stationId).orElse(null);
        if (entry == null) {
            UserAccount user = users.findById(userId)
                    .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "NOT_AUTHENTICATED", "Please sign in first"));
            entry = new WaitingListEntry(user, station, now);
        } else if (!OPEN.contains(entry.getStatus())) {
            entry.rejoin(now);
        }

        entry = entries.save(entry);
        return toResponse(entry);
    }

    @Transactional(readOnly = true)
    public List<WaitingListResponse> mine(long userId) {
        return entries.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(entry -> OPEN.contains(entry.getStatus()))
                .map(this::toResponse)
                .toList();
    }

    public WaitingListResponse leave(long userId, long entryId) {
        WaitingListEntry entry = entries.findById(entryId)
                .filter(item -> item.getUser().getId() == userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "WAITLIST_ENTRY_NOT_FOUND", "Waitlist entry not found"));
        if (!OPEN.contains(entry.getStatus())) {
            throw conflict("WAITLIST_ENTRY_CLOSED", "This waitlist entry is already closed");
        }
        entry.setStatus(WaitingListStatus.CANCELLED);
        return toResponse(entry);
    }

    /** Promotes the oldest waiting customers when new stock reaches a station. */
    public void promoteForAvailableStock(long stationId, int newUnits) {
        if (newUnits <= 0) return;
        if (stations.findById(stationId).filter(s -> s.getStatus() == StationStatus.ACTIVE).isEmpty()) return;
        List<WaitingListEntry> notified = entries
                .findByStationIdAndStatusOrderByCreatedAtAsc(stationId, WaitingListStatus.NOTIFIED);
        List<com.powerbank.entity.PowerBank> available = powerBanks
                .findByStationIdAndStatusOrderByIdAsc(stationId, PowerBankStatus.AVAILABLE);
        int unnotifiedSlots = Math.max(0, available.size() - notified.size());
        int promotionLimit = Math.min(unnotifiedSlots, newUnits);
        if (promotionLimit == 0) return;

        // Build the single project-wide FIFO queue, then scan it for the oldest request
        // eligible for each available device at this station.
        GlobalWaitingQueue queue = new GlobalWaitingQueue();
        java.util.Map<Long, WaitingListEntry> entriesById = new java.util.HashMap<>();
        for (WaitingListEntry entry : entries.findByStatusOrderByCreatedAtAsc(WaitingListStatus.WAITING)) {
            long id = entry.getId();
            queue.enqueue(new WaitingRequest(id, entry.getUser().getId(), entry.getStation().getId(),
                    entry.getCreatedAt().toString(), WaitingListStatus.WAITING.name()));
            entriesById.put(id, entry);
        }

        PowerBankBST inventory = new PowerBankBST();
        for (com.powerbank.entity.PowerBank bank : available) {
            inventory.insert(new datastructure.models.PowerBank(bank.getId(), bank.getStatus().name(), stationId));
        }
        int promoted = 0;
        for (datastructure.models.PowerBank bank : inventory.inOrderTraversal()) {
            if (promoted >= promotionLimit) break;
            WaitingRequest next = queue.findNextEligible(bank);
            if (next != null && queue.removeRequest(next.getRequestId())) {
                WaitingListEntry entry = entriesById.get(next.getRequestId());
                if (entry != null) {
                    entry.setStatus(WaitingListStatus.NOTIFIED);
                    promoted++;
                }
            }
        }
    }

    /** Keeps notifications aligned with stock if a customer outside the queue takes a released unit. */
    public void reconcileAvailableStock(long stationId) {
        long available = powerBanks.countByStationIdAndStatus(stationId, PowerBankStatus.AVAILABLE);
        List<WaitingListEntry> notified = entries
                .findByStationIdAndStatusOrderByCreatedAtAsc(stationId, WaitingListStatus.NOTIFIED);
        int retained = (int) Math.min(available, notified.size());
        for (int i = retained; i < notified.size(); i++) {
            notified.get(i).setStatus(WaitingListStatus.WAITING);
        }
    }

    /** A customer who successfully reserves a device leaves their waitlist entry. */
    public void fulfill(long userId, long stationId) {
        entries.findByUserIdAndStationId(userId, stationId)
                .filter(entry -> OPEN.contains(entry.getStatus()))
                .ifPresent(entry -> entry.setStatus(WaitingListStatus.FULFILLED));
    }

    private WaitingListResponse toResponse(WaitingListEntry entry) {
        Integer position = null;
        if (entry.getStatus() == WaitingListStatus.WAITING) {
            List<WaitingListEntry> queue = entries.findByStationIdAndStatusInOrderByCreatedAtAsc(
                    entry.getStation().getId(), OPEN);
            for (int i = 0; i < queue.size(); i++) {
                if (queue.get(i).getId().equals(entry.getId())) { position = i + 1; break; }
            }
        }
        return new WaitingListResponse(entry.getId(), entry.getStation().getId(),
                entry.getStation().getName(), entry.getStation().getAddress(),
                entry.getStatus().name(), position, entry.getCreatedAt());
    }
}
