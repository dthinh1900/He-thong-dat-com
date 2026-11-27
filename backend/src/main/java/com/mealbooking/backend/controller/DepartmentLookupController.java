package com.mealbooking.backend.controller;

import com.mealbooking.backend.application.model.TicketResponses.*;
import com.mealbooking.backend.application.port.in.TicketQueries;
import java.time.LocalDate;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/departments")
public class DepartmentLookupController {
    private final TicketQueries lookup;

    public DepartmentLookupController(TicketQueries lookup) {
        this.lookup = lookup;
    }

    @GetMapping
    public List<DepartmentChoice> departments() {
        return lookup.departments();
    }

    @GetMapping("/roster")
    public Roster roster(@RequestParam LocalDate date, @RequestParam(required = false) Long departmentId) {
        return lookup.roster(date, departmentId);
    }
}
