package com.mealbooking.backend.application.port.out;

import com.mealbooking.backend.application.model.AdminResponses.DepartmentRow;
import java.util.List;

public interface PeopleDirectory {
    List<DepartmentRow> departments();

    void lockDepartmentsForWrite();

    void lockUsersForWrite();
}
