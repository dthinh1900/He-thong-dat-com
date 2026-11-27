package com.mealbooking.backend.application.usecase;

import com.mealbooking.backend.application.model.AdminCommands.EmployeeInput;
import com.mealbooking.backend.application.model.AdminCommands.ImportRow;
import com.mealbooking.backend.domain.enums.Role;
import com.mealbooking.backend.domain.error.BusinessException;
import com.mealbooking.backend.application.port.out.DepartmentStore;
import com.mealbooking.backend.application.port.out.UserStore;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.mealbooking.backend.application.port.out.PeopleDirectory;
import com.mealbooking.backend.application.port.out.PasswordHasher;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class AdminEmployeeImportTest {
    @Test
    void duplicateExcelRowsFailBeforeAnyUserIsWritten() {
        var users = mock(UserStore.class);
        var departments = mock(DepartmentStore.class);
        var encoder = mock(PasswordHasher.class);
        var jdbc = mock(PeopleDirectory.class);
        var people = new AdminPeopleService(users, departments, encoder, jdbc);
        var rows = List.of(new ImportRow(2, employee("NV101", "an")),
            new ImportRow(7, employee("nv101 ", "binh")));

        var error = assertThrows(BusinessException.class, () -> people.importEmployees(rows, 1L));
        assertTrue(error.getMessage().contains("Dòng 7"));
        verifyNoInteractions(users, departments, encoder, jdbc);
    }

    private static EmployeeInput employee(String code, String user) {
        return new EmployeeInput(code, "Nhân viên kiểm thử", user, null, null, 1L,
            Role.USER, true, "example-password");
    }
}
