package com.powerbank.repository;

import com.powerbank.entity.PowerBankStatusEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PowerBankStatusEventRepository extends JpaRepository<PowerBankStatusEvent, Long> {
    List<PowerBankStatusEvent> findAllByOrderByChangedAtDescIdDesc();
    List<PowerBankStatusEvent> findByPowerBankIdOrderByChangedAtAscIdAsc(Long powerBankId);
}
