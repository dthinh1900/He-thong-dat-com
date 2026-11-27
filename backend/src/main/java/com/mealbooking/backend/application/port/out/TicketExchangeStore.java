package com.mealbooking.backend.application.port.out;

import com.mealbooking.backend.domain.model.TicketExchange;
import com.mealbooking.backend.domain.enums.ExchangeType;
import java.time.LocalDate;
import java.util.List;

public interface TicketExchangeStore {
    List<TicketExchange> findByExchangeTypeAndMealDateGreaterThanEqualOrderById(ExchangeType type, LocalDate date);

    List<TicketExchange> findByRequesterIdAndExchangeTypeOrderById(Long requesterId, ExchangeType type);

    List<TicketExchange> findSaleGroupForUpdate(Long id, ExchangeType type);

    <S extends TicketExchange> S save(S offer);
}
