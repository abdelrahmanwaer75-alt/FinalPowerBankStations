package com.powerbank.repository;

import com.powerbank.entity.Station;
import com.powerbank.entity.StationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StationRepository extends JpaRepository<Station, Long> {
    List<Station> findByStatusOrderByNameAsc(StationStatus status);
}
