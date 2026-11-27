package com.powerbank.service;

import com.powerbank.dto.WaitingListResponse;
import com.powerbank.entity.*;
import com.powerbank.exception.ApiException;
import com.powerbank.repository.*;
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
        long available = powerBanks.countByStationIdAndStatus(stationId, PowerBankStatus.AVAILABLE);
        List<WaitingListEntry> notified = entries
                .findByStationIdAndStatusOrderByCreatedAtAsc(stationId, WaitingListStatus.NOTIFIED);
        int unnotifiedSlots = Math.max(0, (int) Math.min(Integer.MAX_VALUE, available) - notified.size());
        int promotionLimit = Math.min(unnotifiedSlots, newUnits);
        List<WaitingListEntry> waiting = entries
                .findByStationIdAndStatusOrderByCreatedAtAsc(stationId, WaitingListStatus.WAITING);
        for (int i = 0; i < Math.min(promotionLimit, waiting.size()); i++) {
            waiting.get(i).setStatus(WaitingListStatus.NOTIFIED);
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
