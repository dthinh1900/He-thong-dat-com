package com.mealbooking.backend.adapter.persistence.jpa;

import com.mealbooking.backend.application.port.out.TicketExchangeTransactionStore;
import com.mealbooking.backend.domain.model.TicketExchangeTransaction;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketExchangeTransactionRepository extends JpaRepository<TicketExchangeTransaction, Long>, TicketExchangeTransactionStore {
    List<TicketExchangeTransaction> findAllByOrderByCompletedAtDesc();
    List<TicketExchangeTransaction> findBySellerIdOrBuyerIdOrderByCompletedAtDesc(Long sellerId, Long buyerId);
}
