package com.mealbooking.backend.adapter.persistence.jpa;

import com.mealbooking.backend.application.port.out.TicketPurchaseStore;
import com.mealbooking.backend.domain.model.TicketPurchase;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TicketPurchaseRepository extends JpaRepository<TicketPurchase, Long>, TicketPurchaseStore {
    List<TicketPurchase> findByRequesterIdOrderByCreatedAtDesc(Long requesterId);
    List<TicketPurchase> findAllByOrderByCreatedAtDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from TicketPurchase p where p.id = :id")
    Optional<TicketPurchase> findForUpdate(@Param("id") Long id);
}
