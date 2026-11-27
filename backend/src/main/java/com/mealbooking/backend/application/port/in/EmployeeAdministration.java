package com.mealbooking.backend.application.port.in;

import com.mealbooking.backend.application.model.AdminCommands.DepartmentInput;
import com.mealbooking.backend.application.model.AdminCommands.EmployeeInput;
import com.mealbooking.backend.application.model.AdminCommands.ImportRow;
import com.mealbooking.backend.application.model.AdminResponses.DepartmentRow;
import com.mealbooking.backend.application.model.AdminResponses.EmployeeRow;
import java.util.List;

public interface EmployeeAdministration {
    List<DepartmentRow> departments();

    DepartmentRow saveDepartment(Long id, DepartmentInput input);

    List<EmployeeRow> employees(Long departmentId, String search);

    EmployeeRow employee(Long id);

    EmployeeRow saveEmployee(Long id, EmployeeInput input, Long actorId);

    List<EmployeeRow> importEmployees(List<ImportRow> rows, Long actorId);

    EmployeeRow setActive(Long id, boolean active, Long actorId);

    void retire(Long id, Long actorId);
}
