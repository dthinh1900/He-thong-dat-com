package com.mealbooking.backend.application.port.in;

import com.mealbooking.backend.application.model.TicketResponses.DepartmentChoice;
import com.mealbooking.backend.application.model.TicketResponses.EmployeeHistory;
import com.mealbooking.backend.application.model.TicketResponses.ExchangeAuditRow;
import com.mealbooking.backend.application.model.TicketResponses.Roster;
import java.time.LocalDate;
import java.util.List;

public interface TicketQueries {
    List<DepartmentChoice> departments();

    Roster roster(LocalDate date, Long departmentId);

    EmployeeHistory employeeHistory(Long id, int month, int year);

    List<ExchangeAuditRow> exchangeAudit();
}
