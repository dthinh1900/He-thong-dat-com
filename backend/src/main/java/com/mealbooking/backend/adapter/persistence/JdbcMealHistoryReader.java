package com.mealbooking.backend.adapter.persistence;

import com.mealbooking.backend.application.port.out.MealHistoryReader;
import com.mealbooking.backend.application.model.TicketResponses.MealHistorySource;
import com.mealbooking.backend.domain.enums.TicketStatus;
import com.mealbooking.backend.domain.enums.TicketType;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JdbcMealHistoryReader implements MealHistoryReader {
    private final JdbcTemplate jdbc;

    public JdbcMealHistoryReader(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OwnedTicket> findOwnedTickets(Long ownerId, LocalDate from, LocalDate to) {
        return jdbc.query("""
            select t.id, t.meal_date, t.ticket_type, t.price, t.status,
                case when coalesce(r.user_id, p.requester_id, t.owner_id) <> t.owner_id then 'TRANSFER'
                     when t.purchase_request_id is not null then 'PURCHASE'
                     when t.registration_id is not null then 'REGISTRATION'
                     else 'OTHER' end as source
            from meal_tickets t
            left join meal_registrations r on r.id = t.registration_id
            left join ticket_purchases p on p.id = t.purchase_request_id
            where t.owner_id = ? and t.meal_date between ? and ?
            order by t.meal_date, t.id
            """, (rs, index) -> {
                var status = TicketStatus.valueOf(rs.getString("status"));
                return new OwnedTicket(rs.getLong("id"), rs.getObject("meal_date", LocalDate.class),
                    TicketType.valueOf(rs.getString("ticket_type")), rs.getBigDecimal("price"), status,
                    MealHistorySource.valueOf(rs.getString("source")));
            }, ownerId, from, to);
    }
}
