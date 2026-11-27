package com.mealbooking.backend.application.port.out;

import com.mealbooking.backend.domain.model.TicketPurchase;
import java.util.List;
import java.util.Optional;

public interface TicketPurchaseStore {
    List<TicketPurchase> findByRequesterIdOrderByCreatedAtDesc(Long requesterId);

    List<TicketPurchase> findAllByOrderByCreatedAtDesc();

    Optional<TicketPurchase> findForUpdate(Long id);

    <S extends TicketPurchase> S save(S purchase);
}
