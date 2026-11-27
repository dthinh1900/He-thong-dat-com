package com.mealbooking.backend.application.port.out;

import com.mealbooking.backend.domain.model.AuditLog;

public interface AuditLogStore {
    <S extends AuditLog> S save(S event);
}
