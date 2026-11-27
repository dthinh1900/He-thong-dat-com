package com.mealbooking.backend.controller;

import com.mealbooking.backend.dto.request.TicketRequests.*;
import com.mealbooking.backend.application.model.TicketResponses.*;
import com.mealbooking.backend.domain.enums.TicketType;
import com.mealbooking.backend.security.UserPrincipal;
import com.mealbooking.backend.application.port.in.TicketOperations;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class TicketOperationsController {
    private final TicketOperations service;

    public TicketOperationsController(TicketOperations service) {
        this.service = service;
    }

    @GetMapping("/ticket-purchases")
    public List<PurchaseRow> purchases(@AuthenticationPrincipal UserPrincipal principal) {
        return service.purchases(principal.user().getId());
    }

    @GetMapping("/ticket-purchases/quote")
    public PurchaseQuote quote(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate mealDate,
            @RequestParam TicketType ticketType) {
        return service.quotePurchase(mealDate, ticketType);
    }

    @PostMapping("/ticket-purchases")
    @ResponseStatus(HttpStatus.CREATED)
    public PurchaseRow request(@AuthenticationPrincipal UserPrincipal principal, @Valid @RequestBody PurchaseInput input) {
        return service.requestPurchase(principal.user().getId(), input.toCommand());
    }

    @GetMapping("/ticket-exchanges")
    public ExchangeBoard board(@AuthenticationPrincipal UserPrincipal principal) {
        return service.board(principal.user().getId());
    }

    @PostMapping("/ticket-exchanges")
    @ResponseStatus(HttpStatus.CREATED)
    public ExchangeRow sell(@AuthenticationPrincipal UserPrincipal principal, @Valid @RequestBody ExchangeInput input) {
        return service.listForSale(principal.user().getId(), input.toCommand());
    }

    @PostMapping("/ticket-exchanges/{id}/match")
    public List<TransactionRow> match(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id,
            @Valid @RequestBody ExchangeMatch input) {
        int quantity = input.quantity() == null ? 1 : input.quantity();
        return service.matchSale(principal.user().getId(), id, quantity);
    }

    @DeleteMapping("/ticket-exchanges/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        service.cancelSale(principal.user().getId(), id);
    }
}
