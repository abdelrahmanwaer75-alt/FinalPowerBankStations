package com.powerbank.admin;

import com.powerbank.dto.AddPowerBanksRequest;
import com.powerbank.dto.StationResponse;
import com.powerbank.dto.StationUpsertRequest;
import com.powerbank.entity.*;
import com.powerbank.exception.ApiException;
import com.powerbank.repository.PowerBankRepository;
import com.powerbank.repository.StationRepository;
import com.powerbank.service.WaitingListService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class AdminStationService {

    private final StationRepository stations;
    private final PowerBankRepository powerBanks;
    private final WaitingListService waitingList;

    public AdminStationService(StationRepository stations, PowerBankRepository powerBanks,
                               WaitingListService waitingList) {
        this.stations = stations;
        this.powerBanks = powerBanks;
        this.waitingList = waitingList;
    }

    @Transactional(readOnly = true)
    public List<StationResponse> all() {
        return stations.findAll().stream()
                .map(s -> toResponse(s, powerBanks.countByStationIdAndStatus(s.getId(), PowerBankStatus.AVAILABLE)))
                .toList();
    }

    public StationResponse create(StationUpsertRequest req) {
        Station s = new Station(req.name().trim(), req.address().trim(), req.city().trim(),
                req.latitude(), req.longitude(), req.totalCapacity(),
                req.openingHours(), req.contactPhone(), req.description());
        s.setStatus(parseStatus(req.status()));
        stations.save(s);
        return toResponse(s, 0);
    }

    public StationResponse update(long id, StationUpsertRequest req) {
        Station s = stations.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "STATION_NOT_FOUND", "Station not found"));
        long docked = powerBanks.countByStationIdAndStatus(id, PowerBankStatus.AVAILABLE)
                + powerBanks.countByStationIdAndStatus(id, PowerBankStatus.RESERVED)
                + powerBanks.countByStationIdAndStatus(id, PowerBankStatus.RETURNED_PENDING_REVIEW);
        if (req.totalCapacity() < docked) {
            throw new ApiException(HttpStatus.CONFLICT, "CAPACITY_BELOW_DOCKED",
                    "Capacity cannot be lower than the " + docked + " devices docked here");
        }
        s.apply(req.name().trim(), req.address().trim(), req.city().trim(),
                req.latitude(), req.longitude(), req.totalCapacity(),
                req.openingHours(), req.contactPhone(), req.description());
        s.setStatus(parseStatus(req.status()));
        if (s.getStatus() == StationStatus.ACTIVE) {
            waitingList.promoteForAvailableStock(id,
                    (int) powerBanks.countByStationIdAndStatus(id, PowerBankStatus.AVAILABLE));
        }
        return toResponse(s, powerBanks.countByStationIdAndStatus(id, PowerBankStatus.AVAILABLE));
    }

    /** Adds devices to a station. They start AVAILABLE and count against capacity. */
    public StationResponse addPowerBanks(long stationId, AddPowerBanksRequest req) {
        Station s = stations.findById(stationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "STATION_NOT_FOUND", "Station not found"));
        for (int i = 0; i < req.count(); i++) {
            PowerBank bank = new PowerBank(
                    "PB-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(), "PowerBank 10000mAh");
            bank.setStation(s);
            powerBanks.save(bank);
        }
        waitingList.promoteForAvailableStock(stationId, req.count());
        return toResponse(s, powerBanks.countByStationIdAndStatus(stationId, PowerBankStatus.AVAILABLE));
    }

    private static StationStatus parseStatus(String value) {
        try {
            return StationStatus.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_STATUS", "Status must be ACTIVE or INACTIVE");
        }
    }

    private StationResponse toResponse(Station s, long available) {
        return new StationResponse(s.getId(), s.getName(), s.getAddress(), s.getCity(),
                s.getLatitude(), s.getLongitude(), s.getTotalCapacity(), available,
                s.getStatus().name(), s.getOpeningHours(), s.getContactPhone(), s.getDescription());
    }
}
