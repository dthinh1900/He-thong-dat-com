package com.mealbooking.backend.adapter.persistence.jpa;

import com.mealbooking.backend.application.port.out.TicketExchangeStore;
import com.mealbooking.backend.domain.model.TicketExchange;
import com.mealbooking.backend.domain.enums.ExchangeType;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TicketExchangeRepository extends JpaRepository<TicketExchange, Long>, TicketExchangeStore {
    List<TicketExchange> findByExchangeTypeAndMealDateGreaterThanEqualOrderById(
        ExchangeType type, LocalDate date);
    List<TicketExchange> findByRequesterIdAndExchangeTypeOrderById(Long requesterId, ExchangeType type);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select e from TicketExchange e where e.exchangeType = :type and
          (e.id = :id or e.saleGroup = (select anchor.saleGroup from TicketExchange anchor where anchor.id = :id))
        order by e.id
        """)
    List<TicketExchange> findSaleGroupForUpdate(@Param("id") Long id, @Param("type") ExchangeType type);
}
