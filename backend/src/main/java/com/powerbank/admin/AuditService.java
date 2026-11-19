package com.powerbank.admin;

import com.powerbank.entity.AuditLog;
import com.powerbank.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Writes in its own transaction, so a log entry is kept even if the action itself is rolled back. */
@Service
public class AuditService {

    private final AuditLogRepository logs;

    public AuditService(AuditLogRepository logs) {
        this.logs = logs;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void success(long actorId, String action, String entityType, Object entityId, String details) {
        logs.save(new AuditLog(actorId, action, entityType,
                entityId == null ? null : String.valueOf(entityId),
                AuditLog.AuditResult.SUCCESS, null, details));
    }
}
