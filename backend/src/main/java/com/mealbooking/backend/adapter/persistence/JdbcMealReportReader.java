package com.mealbooking.backend.adapter.persistence;

import com.mealbooking.backend.application.port.out.MealReportReader;
import com.mealbooking.backend.application.model.AdminResponses.*;
import com.mealbooking.backend.application.model.AdminReportDetail;
import com.mealbooking.backend.domain.enums.TicketType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcMealReportReader implements MealReportReader {
    private final JdbcTemplate jdbc;

    public JdbcMealReportReader(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public AdminReportDetail detail(LocalDate from, LocalDate to) {
        var departments = jdbc.query("select id,name from departments order by name,id",
            (rs, n) -> new AdminReportDetail.Department(rs.getLong("id"), rs.getString("name")));
        var employees = jdbc.query("select id,employee_code,full_name,department_id from users order by full_name,id",
            (rs, n) -> new AdminReportDetail.Employee(rs.getLong("id"), rs.getString("employee_code"),
                rs.getString("full_name"), rs.getLong("department_id")));
        var tickets = jdbc.query("""
            select t.id,t.ticket_code,t.owner_id,t.meal_date,t.ticket_type,t.price,t.purchase_request_id,x.id transfer_id
            from meal_tickets t
            left join lateral (
                select id from ticket_exchange_transactions
                where ticket_id=t.id and buyer_id=t.owner_id
                order by completed_at desc,id desc limit 1
            ) x on true
            where t.meal_date between ? and ?
              and t.status in ('AVAILABLE','LISTED_FOR_EXCHANGE','USED')
            order by t.meal_date,t.id
            """, (rs, n) -> new AdminReportDetail.Ticket(rs.getLong("id"), rs.getString("ticket_code"), rs.getLong("owner_id"),
                rs.getObject("meal_date", LocalDate.class), TicketType.valueOf(rs.getString("ticket_type")),
                rs.getBigDecimal("price"), rs.getObject("purchase_request_id", Long.class),
                rs.getObject("transfer_id", Long.class)), from, to);
        var transfers = jdbc.query("""
            select x.id,x.seller_id,x.buyer_id,x.completed_at,t.meal_date,
                coalesce(sale.ticket_type,t.ticket_type) ticket_type,coalesce(x.amount,t.price) amount
            from ticket_exchange_transactions x
            join meal_tickets t on t.id=x.ticket_id
            left join ticket_exchanges sale on sale.id=x.sell_request_id
            where t.meal_date between ? and ?
            order by x.completed_at desc,x.id desc
            """, (rs, n) -> new AdminReportDetail.Transfer(rs.getLong("id"), rs.getLong("seller_id"),
                rs.getLong("buyer_id"), rs.getObject("meal_date", LocalDate.class),
                TicketType.valueOf(rs.getString("ticket_type")), rs.getBigDecimal("amount"), 1,
                rs.getObject("completed_at", LocalDateTime.class)), from, to);
        return new AdminReportDetail(from, to, departments, employees, tickets, transfers);
    }

    public Report summarize(LocalDate start, LocalDate end) {
        // Include locked employees with historical tickets; sum snapshot prices.
        record Row(Long departmentId,EmployeeTotal employee) {}
        List<Row> rows=jdbc.query("""
            select d.id department_id,u.id,u.employee_code,u.full_name,
                   count(t.id) tickets,coalesce(sum(t.price),0) amount
            from departments d join users u on u.department_id=d.id
            left join meal_tickets t on t.owner_id=u.id and t.meal_date>=? and t.meal_date<?
                and t.status in ('AVAILABLE','LISTED_FOR_EXCHANGE','USED')
            group by d.id,d.name,u.id,u.employee_code,u.full_name,u.active
            having u.active or count(t.id)>0
            order by d.name,d.id,u.full_name,u.id
            """,(r,n)->new Row(r.getLong("department_id"),new EmployeeTotal(r.getLong("id"),r.getString("employee_code"),
                    r.getString("full_name"),r.getLong("tickets"),r.getBigDecimal("amount"))),start,end);
        Map<Long,List<EmployeeTotal>> groups=new LinkedHashMap<>(); Map<Long,String> names=new LinkedHashMap<>();
        jdbc.query("select id,name from departments order by name,id",(org.springframework.jdbc.core.RowCallbackHandler) r->{
            long id=r.getLong(1); names.put(id,r.getString(2)); groups.put(id,new ArrayList<>());
        });
        for(Row row:rows) groups.get(row.departmentId()).add(row.employee());
        List<DepartmentTotal> departments=groups.entrySet().stream().map(e->new DepartmentTotal(e.getKey(),names.get(e.getKey()),
            e.getValue().stream().mapToLong(EmployeeTotal::tickets).sum(),
            e.getValue().stream().map(EmployeeTotal::totalAmount).reduce(BigDecimal.ZERO,BigDecimal::add),e.getValue())).toList();
        Map<LocalDate,DailyTotal> byDate=new HashMap<>();
        jdbc.query("""
            select meal_date,count(*) tickets,sum(price) amount from meal_tickets
            where meal_date>=? and meal_date<? and status in ('AVAILABLE','LISTED_FOR_EXCHANGE','USED')
            group by meal_date
            """,(org.springframework.jdbc.core.RowCallbackHandler) r->{
                LocalDate date=r.getObject(1,LocalDate.class); byDate.put(date,new DailyTotal(date,r.getLong(2),r.getBigDecimal(3)));
            },start,end);
        List<DailyTotal> days=start.datesUntil(end).map(d->byDate.getOrDefault(d,new DailyTotal(d,0,BigDecimal.ZERO))).toList();
        return new Report(start.getMonthValue(),start.getYear(),departments.stream().mapToLong(DepartmentTotal::tickets).sum(),
            departments.stream().map(DepartmentTotal::totalAmount).reduce(BigDecimal.ZERO,BigDecimal::add),departments,days);
    }
}
