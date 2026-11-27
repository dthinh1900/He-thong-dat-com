package com.mealbooking.backend.config;

import com.mealbooking.backend.domain.model.Department;
import com.mealbooking.backend.domain.model.User;
import com.mealbooking.backend.domain.enums.Role;
import com.mealbooking.backend.adapter.persistence.jpa.DepartmentRepository;
import com.mealbooking.backend.application.port.out.UserStore;
import java.nio.charset.StandardCharsets;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "app.initial-admin.enabled", havingValue = "true")
public class InitialAdminProvisioning implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    private final UserStore users;
    private final DepartmentRepository departments;
    private final PasswordEncoder encoder;
    private final Environment environment;

    public InitialAdminProvisioning(JdbcTemplate jdbc, UserStore users, DepartmentRepository departments,
            PasswordEncoder encoder, Environment environment) {
        this.jdbc = jdbc;
        this.users = users;
        this.departments = departments;
        this.encoder = encoder;
        this.environment = environment;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        jdbc.execute("LOCK TABLE users IN SHARE ROW EXCLUSIVE MODE");
        if (users.count() != 0) return;

        User admin = new User();
        admin.setUsername(required("username", 80));
        admin.setEmployeeCode(required("employee-code", 50));
        admin.setFullName(required("full-name", 160));
        String password = environment.getProperty("app.initial-admin.password", "");
        if (password.isBlank() || password.length() < 12 || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Initial admin password must be at least 12 characters and at most 72 UTF-8 bytes.");
        }
        admin.setPassword(encoder.encode(password));
        admin.setRole(Role.ADMIN);
        String departmentName = required("department", 150);
        Department department = departments.findByName(departmentName).orElseGet(() -> {
            Department created = new Department();
            created.setName(departmentName);
            return departments.save(created);
        });
        admin.setDepartment(department);
        users.save(admin);
    }

    private String required(String name, int maxLength) {
        String property = "app.initial-admin." + name;
        String value = environment.getProperty(property, "").trim();
        if (value.isEmpty() || value.length() > maxLength) {
            throw new IllegalArgumentException("Configure " + property + " before provisioning an initial admin.");
        }
        return value;
    }
}
