package com.mealbooking.backend.application.port.in;

import com.mealbooking.backend.application.model.TicketCommands.ExchangeInput;
import com.mealbooking.backend.application.model.TicketCommands.PurchaseInput;
import com.mealbooking.backend.application.model.TicketResponses.ExchangeBoard;
import com.mealbooking.backend.application.model.TicketResponses.ExchangeRow;
import com.mealbooking.backend.application.model.TicketResponses.PurchaseQuote;
import com.mealbooking.backend.application.model.TicketResponses.PurchaseRow;
import com.mealbooking.backend.application.model.TicketResponses.TransactionRow;
import com.mealbooking.backend.domain.enums.PurchaseStatus;
import com.mealbooking.backend.domain.enums.TicketType;
import java.time.LocalDate;
import java.util.List;

public interface TicketOperations {
    List<PurchaseRow> purchases(Long userId);

    PurchaseQuote quotePurchase(LocalDate date, TicketType type);

    PurchaseRow requestPurchase(Long userId, PurchaseInput input);

    PurchaseRow decidePurchase(Long adminId, Long id, PurchaseStatus decision);

    ExchangeBoard board(Long userId);

    ExchangeRow listForSale(Long userId, ExchangeInput input);

    List<TransactionRow> matchSale(Long buyerId, Long id, int quantity);

    void cancelSale(Long userId, Long id);

    List<TransactionRow> transactions();
}
