package com.mealbooking.backend.adapter.persistence;

import com.mealbooking.backend.application.port.out.TicketDirectory;
import com.mealbooking.backend.application.model.TicketResponses.*;
import com.mealbooking.backend.domain.enums.AuditAction;
import com.mealbooking.backend.domain.enums.TicketType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcTicketDirectory implements TicketDirectory {
    private final JdbcTemplate jdbc;

    public JdbcTicketDirectory(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<DepartmentChoice> departments() {
        return jdbc.query("select id, name from departments order by name, id",
            (rs, index) -> new DepartmentChoice(rs.getLong("id"), rs.getString("name")));
    }

    public List<RosterRow> rosterRows(LocalDate date, Long departmentId) {
        return jdbc.query("""
            select u.id, u.employee_code, u.full_name, d.id department_id, d.name department_name,
                t.ticket_type, count(*) quantity
            from meal_tickets t join users u on u.id = t.owner_id join departments d on d.id = u.department_id
            where t.meal_date = ? and t.status in ('AVAILABLE', 'LISTED_FOR_EXCHANGE', 'USED')
                and (? = -1 or d.id = ?)
            group by u.id, d.id, t.ticket_type
            order by u.full_name, u.id, t.ticket_type
            """, (rs, index) -> new RosterRow(rs.getLong("id"), rs.getString("employee_code"),
                rs.getString("full_name"), rs.getLong("department_id"), rs.getString("department_name"),
                TicketType.valueOf(rs.getString("ticket_type")), rs.getLong("quantity")),
            date, departmentId == null ? -1L : departmentId, departmentId == null ? -1L : departmentId);
    }

    public List<ExchangeAuditRow> exchangeAudit() {
        return jdbc.query("""
            select a.id, a.action, a.actor_id, u.full_name, a.description, a.created_at, a.ticket_id, a.transaction_id
            from audit_logs a join users u on u.id = a.actor_id
            where a.action in ('CREATE_EXCHANGE', 'COMPLETE_EXCHANGE', 'CANCEL_EXCHANGE')
            order by a.created_at desc, a.id desc
            """, (rs, index) -> new ExchangeAuditRow(rs.getLong("id"), AuditAction.valueOf(rs.getString("action")),
                rs.getLong("actor_id"), rs.getString("full_name"), rs.getString("description"),
                rs.getObject("created_at", LocalDateTime.class), rs.getObject("ticket_id", Long.class),
                rs.getObject("transaction_id", Long.class)));
    }
}
