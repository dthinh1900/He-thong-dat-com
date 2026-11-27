package com.mealbooking.backend.adapter.persistence;

import com.mealbooking.backend.application.port.out.PeopleDirectory;
import com.mealbooking.backend.application.model.AdminResponses.DepartmentRow;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcPeopleDirectory implements PeopleDirectory {
    private final JdbcTemplate jdbc;

    public JdbcPeopleDirectory(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<DepartmentRow> departments() {
        return jdbc.query("""
            select d.id,d.name,d.description,d.active,count(u.id) employee_count
            from departments d left join users u on u.department_id=d.id
            group by d.id,d.name,d.description,d.active order by d.name,d.id
            """,(r,n)->new DepartmentRow(r.getLong("id"),r.getString("name"),r.getString("description"),
                r.getBoolean("active"),r.getLong("employee_count")));
    }

    public void lockDepartmentsForWrite() { jdbc.execute("lock table departments in share row exclusive mode"); }

    public void lockUsersForWrite() { jdbc.execute("lock table users in share row exclusive mode"); }
}
