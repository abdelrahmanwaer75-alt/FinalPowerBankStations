package com.powerbank.ds;

import com.powerbank.dto.StationResponse;
import powerbank.models.Station;
import powerbank.sorting.Sorting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Uses the course's own sorting class (powerbank.sorting.Sorting) to order stations by available units.
 * The Station model from the data-structures project uses int ids, so the adapter maps back to the API response.
 */
public final class StationRanking {

    private StationRanking() {}

    /** Returns the same stations, most available first. Ties keep their original order. */
    public static List<StationResponse> byAvailability(List<StationResponse> stations) {
        Station[] array = new Station[stations.size()];
        Map<Integer, StationResponse> byId = new HashMap<>();

        for (int i = 0; i < stations.size(); i++) {
            StationResponse s = stations.get(i);
            int id = Math.toIntExact(s.id());
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
