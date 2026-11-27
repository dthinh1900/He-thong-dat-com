package com.mealbooking.backend.adapter.persistence;

import com.mealbooking.backend.application.port.out.AdminMealData;
import com.mealbooking.backend.application.model.AdminResponses.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcAdminMealData implements AdminMealData {
    private final JdbcTemplate jdbc;

    public JdbcAdminMealData(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public Overview overview(LocalDate today, LocalDate tomorrow, LocalDate weekStart) {
        return new Overview(today,tomorrow,registeredCount(today),registeredCount(tomorrow),
            jdbc.queryForObject("select count(*) from users",Long.class),
            jdbc.queryForObject("select count(*) from departments",Long.class),
            jdbc.queryForObject("select count(*) from ticket_purchases where status='PENDING'",Long.class),
            !Boolean.TRUE.equals(jdbc.queryForObject("""
                select exists (select 1 from menu_images m join menu_image_content c on c.menu_id = m.id
                    where m.week_start_date = ?)
                """, Boolean.class, weekStart)));
    }

    private long registeredCount(LocalDate date) {
        return jdbc.queryForObject("""
            select count(distinct owner_id) from meal_tickets
            where meal_date=? and status in ('AVAILABLE','LISTED_FOR_EXCHANGE','USED')
            """,Long.class,date);
    }

    public PortionCounts portionCounts(LocalDate date) {
        return jdbc.queryForObject("""
            select count(*) filter (where ticket_type = 'NORMAL') as normal,
                count(*) filter (where ticket_type = 'SPECIAL') as special
            from meal_tickets
            where meal_date = ? and status in ('AVAILABLE', 'LISTED_FOR_EXCHANGE', 'USED')
            """, (rs, index) -> new PortionCounts(rs.getLong("normal"), rs.getLong("special")), date);
    }

    public KitchenReport lastKitchenReport(LocalDate date) {
        return jdbc.query("""
            select normal_count, special_count, reported_at
            from kitchen_meal_reports where meal_date = ? order by id desc limit 1
            """, (rs, index) -> new KitchenReport(new PortionCounts(rs.getLong("normal_count"),
                rs.getLong("special_count")), rs.getObject("reported_at", LocalDateTime.class)), date)
            .stream().findFirst().orElse(null);
    }

    public void saveKitchenReport(LocalDate date, PortionCounts counts, Long actorId, LocalDateTime reportedAt) {
        jdbc.update("""
            insert into kitchen_meal_reports (meal_date, normal_count, special_count, reported_by, reported_at)
            values (?, ?, ?, ?, ?)
            """, date, counts.normal(), counts.special(), actorId, reportedAt);
    }

    public List<RegistrationState> registrationStates(LocalDate date, Long departmentId) {
        return jdbc.query("""
            select u.id,u.employee_code,u.full_name,u.department_id,u.active,t.id ticket_id,t.status,t.owner_id
            from users u left join meal_registrations r on r.user_id=u.id and r.month=? and r.year=?
            left join meal_tickets t on t.registration_id=r.id and t.meal_date=?
            where (? = -1 or u.department_id=?) order by u.full_name,u.id
            """, (rs, index) -> new RegistrationState(rs.getLong("id"), rs.getString("employee_code"),
                rs.getString("full_name"), rs.getLong("department_id"), rs.getBoolean("active"),
                rs.getObject("ticket_id", Long.class), rs.getString("status"), rs.getLong("owner_id")),date.getMonthValue(),date.getYear(),date,departmentId==null?-1L:departmentId,departmentId==null?-1L:departmentId);
    }

    public void lockPricesForWrite() { jdbc.execute("lock table meal_prices in share row exclusive mode"); }
}
