package com.powerbank.admin;

import com.powerbank.dto.PowerBankStatusEventResponse;
import com.powerbank.entity.PowerBankStatusEvent;
import com.powerbank.repository.PowerBankStatusEventRepository;
import datastructure.linkedlist.StatusHistoryList;
import datastructure.models.StatusHistory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class PowerBankStatusHistoryService {
    private final PowerBankStatusEventRepository events;

    public PowerBankStatusHistoryService(PowerBankStatusEventRepository events) {
        this.events = events;
    }

    public List<PowerBankStatusEventResponse> list(Long powerBankId) {
        List<PowerBankStatusEvent> rows = powerBankId == null
                ? events.findAllByOrderByChangedAtDescIdDesc()
                : events.findByPowerBankIdOrderByChangedAtAscIdAsc(powerBankId);
        StatusHistoryList history = new StatusHistoryList();
        for (PowerBankStatusEvent row : rows) {
            history.insert(new StatusHistory(row.getId(), row.getPowerBankId(), row.getOldStatus(),
                    row.getNewStatus(), row.getChangedAt().toString()));
        }
        StatusHistory[] selected = powerBankId == null ? history.toArray() : history.searchByPowerBankId(powerBankId);
        return java.util.Arrays.stream(selected).map(item -> new PowerBankStatusEventResponse(
                item.getEventId(), item.getPowerBankId(), item.getOldStatus(), item.getNewStatus(),
                java.time.Instant.parse(item.getChangeDate()))).toList();
    }
}
