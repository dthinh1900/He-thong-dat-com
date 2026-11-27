package com.mealbooking.backend.application.usecase;

import com.mealbooking.backend.application.port.out.TicketDirectory;
import com.mealbooking.backend.application.port.out.UserStore;
import com.mealbooking.backend.application.port.out.MealTicketStore;
import com.mealbooking.backend.application.port.in.TicketQueries;
import com.mealbooking.backend.application.model.TicketResponses.*;
import com.mealbooking.backend.domain.enums.*;
import com.mealbooking.backend.domain.error.BusinessException;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;

public class TicketLookupService implements TicketQueries {
    private final TicketDirectory directory;
    private final MealTicketStore tickets;
    private final UserStore users;
    private final RegistrationPolicy policy;

    public TicketLookupService(TicketDirectory directory, MealTicketStore tickets, UserStore users,
            RegistrationPolicy policy) {
        this.directory = directory;
        this.tickets = tickets;
        this.users = users;
        this.policy = policy;
    }

    @Override
    public List<DepartmentChoice> departments() { return directory.departments(); }

    @Override
    public Roster roster(LocalDate date, Long departmentId) {
        var rows = directory.rosterRows(date, departmentId);

        return new Roster(date, rows.stream().map(RosterRow::userId).distinct().count(),
            rows.stream().mapToLong(RosterRow::quantity).sum(), rows);
    }

    @Override
    public EmployeeHistory employeeHistory(Long id, int month, int year) {
        if (month < 1 || month > 12 || year < 2000 || year > 2200) {
            throw BusinessException.badRequest("Tháng hoặc năm không hợp lệ.");
        }
        if (!users.existsById(id)) throw BusinessException.notFound("Không tìm thấy nhân viên.");
        var range = YearMonth.of(year, month);
        var rows = tickets.findByOwnerIdAndMealDateBetweenOrderByMealDate(id, range.atDay(1), range.atEndOfMonth());
        var billable = rows.stream().filter(t -> t.getStatus().isBillable()).toList();
        var total = billable.stream().map(t -> t.getPrice()).reduce(BigDecimal.ZERO, BigDecimal::add);
        var settings = policy.settings();
        var history = rows.stream().map(ticket -> {
            boolean removable = ticket.getStatus() == TicketStatus.AVAILABLE
                && policy.adminCancellationLock(ticket.getMealDate()) == null;
            return new EmployeeTicket(ticket.getId(), ticket.getMealDate(), ticket.getTicketType(), ticket.getPrice(),
                ticket.getStatus(), removable, removable && policy.canWithdraw(ticket, settings));
        }).toList();
        return new EmployeeHistory(id, month, year, billable.size(), total, history);
    }

    @Override
    public List<ExchangeAuditRow> exchangeAudit() { return directory.exchangeAudit(); }
}
