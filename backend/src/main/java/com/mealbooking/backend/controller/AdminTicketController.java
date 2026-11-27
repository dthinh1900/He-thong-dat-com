package com.mealbooking.backend.controller;

import com.mealbooking.backend.application.port.in.TicketOperations;
import com.mealbooking.backend.application.port.in.TicketQueries;
import com.mealbooking.backend.application.port.in.MealRegistrations;
import com.mealbooking.backend.dto.request.AdminRequests.CancellationInput;
import com.mealbooking.backend.dto.request.TicketRequests.PurchaseDecision;
import com.mealbooking.backend.application.model.TicketResponses.*;
import com.mealbooking.backend.security.UserPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminTicketController {
    private final TicketOperations operations;
    private final TicketQueries lookup;
    private final MealRegistrations registration;

    public AdminTicketController(TicketOperations operations, TicketQueries lookup,
            MealRegistrations registration) {
        this.operations = operations;
        this.lookup = lookup;
        this.registration = registration;
    }

    @GetMapping("/purchases")
    public List<PurchaseRow> purchases() {
        return operations.purchases(null);
    }

    @PatchMapping("/purchases/{id}")
    public PurchaseRow decide(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id,
            @Valid @RequestBody PurchaseDecision input) {
        return operations.decidePurchase(principal.user().getId(), id, input.decision());
    }

    @GetMapping("/exchange-transactions")
    public List<TransactionRow> transactions() {
        return operations.transactions();
    }

    @GetMapping("/exchange-audit")
    public List<ExchangeAuditRow> audit() {
        return lookup.exchangeAudit();
    }

    @GetMapping("/employees/{id}/history")
    public EmployeeHistory history(@PathVariable Long id, @RequestParam int month, @RequestParam int year) {
        return lookup.employeeHistory(id, month, year);
    }

    @PatchMapping("/meal-tickets/{id}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id,
            @Valid @RequestBody(required = false) CancellationInput input) {
        registration.cancelTicketByAdmin(principal.user().getId(), id, input == null ? null : input.reason(),
            input == null ? null : input.withdrawal());
    }
}
