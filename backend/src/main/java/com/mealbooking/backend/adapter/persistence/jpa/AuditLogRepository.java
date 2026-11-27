package com.mealbooking.backend.adapter.persistence.jpa;

import com.mealbooking.backend.application.port.out.AuditLogStore;
import com.mealbooking.backend.domain.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, AuditLogStore { }
