package com.mealbooking.backend.controller;

import com.mealbooking.backend.dto.request.EmployeeImportRequest;
import com.mealbooking.backend.application.model.AdminResponses.EmployeeRow;
import com.mealbooking.backend.security.UserPrincipal;
import com.mealbooking.backend.application.port.in.EmployeeAdministration;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/employees/import")
@PreAuthorize("hasRole('ADMIN')")
public class AdminEmployeeImportController {
    private final EmployeeAdministration people;

    public AdminEmployeeImportController(EmployeeAdministration people) {
        this.people = people;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public List<EmployeeRow> importEmployees(@Valid @RequestBody EmployeeImportRequest request,
            @AuthenticationPrincipal UserPrincipal actor) {
        return people.importEmployees(request.toCommands(), actor.user().getId());
    }
}
