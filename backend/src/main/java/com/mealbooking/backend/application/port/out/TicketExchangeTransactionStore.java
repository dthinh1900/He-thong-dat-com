package com.mealbooking.backend.application.port.out;

import com.mealbooking.backend.domain.model.TicketExchangeTransaction;
import java.util.List;

public interface TicketExchangeTransactionStore {
    List<TicketExchangeTransaction> findAllByOrderByCompletedAtDesc();

    List<TicketExchangeTransaction> findBySellerIdOrBuyerIdOrderByCompletedAtDesc(Long sellerId, Long buyerId);

    <S extends TicketExchangeTransaction> S save(S transaction);
}
