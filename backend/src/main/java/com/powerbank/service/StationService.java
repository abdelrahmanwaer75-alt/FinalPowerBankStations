package com.powerbank.service;

import com.powerbank.dto.AvailabilityResponse;
import com.powerbank.dto.StationResponse;
import com.powerbank.entity.PowerBankStatus;
import com.powerbank.entity.Station;
import com.powerbank.entity.StationStatus;
import com.powerbank.exception.ApiException;
import com.powerbank.repository.PowerBankRepository;
import com.powerbank.repository.StationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.powerbank.service.ReturnService.DOCKED;

@Service
@Transactional(readOnly = true)
public class StationService {

    private final StationRepository stations;
    private final PowerBankRepository powerBanks;

    public StationService(StationRepository stations, PowerBankRepository powerBanks) {
        this.stations = stations;
        this.powerBanks = powerBanks;
    }

    /** Active stations. Availability comes from one grouped query, not one query per station. */
    public List<StationResponse> listActive() {
        Map<Long, Long> available = availableByStation();
        return stations.findByStatusOrderByNameAsc(StationStatus.ACTIVE).stream()
                .map(s -> toResponse(s, available.getOrDefault(s.getId(), 0L)))
                .toList();
    }

    public List<StationResponse> nearby(double lat, double lng, double radiusKm) {
        Map<Long, Long> available = availableByStation();
        return stations.findByStatusOrderByNameAsc(StationStatus.ACTIVE).stream()
                .filter(s -> distanceKm(lat, lng, s.getLatitude().doubleValue(), s.getLongitude().doubleValue()) <= radiusKm)
                .sorted(Comparator.comparingDouble(s ->
                        distanceKm(lat, lng, s.getLatitude().doubleValue(), s.getLongitude().doubleValue())))
                .map(s -> toResponse(s, available.getOrDefault(s.getId(), 0L)))
                .toList();
    }

    public StationResponse getActive(Long id) {
        Station s = activeOrNotFound(id);
        return toResponse(s, powerBanks.countByStationIdAndStatus(id, PowerBankStatus.AVAILABLE));
    }

    public AvailabilityResponse availability(Long id) {
        Station s = activeOrNotFound(id);
        long available = powerBanks.countByStationIdAndStatus(id, PowerBankStatus.AVAILABLE);
        long rented = powerBanks.countByStationIdAndStatus(id, PowerBankStatus.RENTED);
        long docked = powerBanks.countDockedAt(id, DOCKED);
        long slotsLeft = Math.max(0, s.getTotalCapacity() - docked);
        return new AvailabilityResponse(id, s.getTotalCapacity(), available, rented, slotsLeft);
    }

    private Station activeOrNotFound(Long id) {
        return stations.findById(id)
                .filter(st -> st.getStatus() == StationStatus.ACTIVE)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "STATION_NOT_FOUND", "Station not found"));
    }

    private Map<Long, Long> availableByStation() {
        Map<Long, Long> map = new HashMap<>();
        for (Object[] row : powerBanks.countAvailableGroupedByStation(PowerBankStatus.AVAILABLE)) {
            map.put((Long) row[0], (Long) row[1]);
        }
        return map;
    }

    private static double distanceKm(double lat1, double lng1, double lat2, double lng2) {
        double r = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * r * Math.asin(Math.sqrt(a));
    }

    private StationResponse toResponse(Station s, long available) {
        return new StationResponse(
                s.getId(), s.getName(), s.getAddress(), s.getCity(),
                s.getLatitude(), s.getLongitude(), s.getTotalCapacity(), available,
                s.getStatus().name(), s.getOpeningHours(), s.getContactPhone(), s.getDescription());
    }
}
