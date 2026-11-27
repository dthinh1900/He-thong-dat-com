package com.mealbooking.backend.application.port.out;

import com.mealbooking.backend.application.model.TicketResponses.DepartmentChoice;
import com.mealbooking.backend.application.model.TicketResponses.ExchangeAuditRow;
import com.mealbooking.backend.application.model.TicketResponses.RosterRow;
import java.time.LocalDate;
import java.util.List;

public interface TicketDirectory {
    List<DepartmentChoice> departments();

    List<RosterRow> rosterRows(LocalDate date, Long departmentId);

    List<ExchangeAuditRow> exchangeAudit();
}
