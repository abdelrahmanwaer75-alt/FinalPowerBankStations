package com.powerbank.controller;

import com.powerbank.dto.AvailabilityResponse;
import com.powerbank.dto.StationResponse;
import com.powerbank.service.StationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stations")
public class StationController {

    private final StationService stations;

    public StationController(StationService stations) {
        this.stations = stations;
    }

    @GetMapping
    List<StationResponse> list() {
        return stations.listActive();
    }

    @GetMapping("/nearby")
    List<StationResponse> nearby(@RequestParam double lat, @RequestParam double lng,
                                 @RequestParam(defaultValue = "5") double radiusKm) {
        return stations.nearby(lat, lng, radiusKm);
    }

    @GetMapping("/{id}")
    StationResponse get(@PathVariable Long id) {
        return stations.getActive(id);
    }

    @GetMapping("/{id}/availability")
    AvailabilityResponse availability(@PathVariable Long id) {
        return stations.availability(id);
    }
}
