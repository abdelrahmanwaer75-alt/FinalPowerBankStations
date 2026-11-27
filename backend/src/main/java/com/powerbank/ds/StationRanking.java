package com.powerbank.ds;

import com.powerbank.dto.StationResponse;
import datastructure.models.Station;
import datastructure.sorting.Sorting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Uses the course's own sorting class (datastructure.sorting.Sorting) to order stations by available units.
 * The Data Structures Station model uses long IDs, matching the database's BIGINT keys.
 */
public final class StationRanking {

    private StationRanking() {}

    /** Returns the same stations, most available first. Ties keep their original order. */
    public static List<StationResponse> byAvailability(List<StationResponse> stations) {
        Station[] array = new Station[stations.size()];
        Map<Long, StationResponse> byId = new HashMap<>();

        for (int i = 0; i < stations.size(); i++) {
            StationResponse s = stations.get(i);
            long id = s.id();
            byId.put(id, s);
            array[i] = new Station(id, s.name(), s.totalCapacity(),
                    (int) s.availableCount(), "ACTIVE".equals(s.status()), 0.0);
        }

        Sorting.sortStationsByAvailability(array);

        List<StationResponse> ranked = new ArrayList<>(array.length);
        for (Station st : array) {
            ranked.add(byId.get(st.getId()));
        }
        return ranked;
    }
}
