package com.mealbooking.backend.adapter.persistence.jpa;

import com.mealbooking.backend.application.port.out.MealTicketStore;
import com.mealbooking.backend.domain.model.MealTicket;
import com.mealbooking.backend.domain.enums.TicketStatus;
import com.mealbooking.backend.domain.enums.TicketType;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MealTicketRepository extends JpaRepository<MealTicket, Long>, MealTicketStore {
    List<MealTicket> findByRegistrationId(Long registrationId);
    List<MealTicket> findByOwnerIdAndMealDateBetweenOrderByMealDate(Long ownerId, LocalDate from, LocalDate to);
    List<MealTicket> findByOwnerIdAndMealDateGreaterThanEqualAndStatusOrderByMealDateAscIdAsc(
        Long ownerId, LocalDate date, TicketStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from MealTicket t where t.id = :id")
    Optional<MealTicket> findForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from MealTicket t where t.registration.id = :id order by t.id")
    List<MealTicket> findByRegistrationForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select t from MealTicket t
        where t.owner.id = :ownerId and t.mealDate = :date and t.ticketType = :type and t.status = :status
        order by t.id
        """)
    List<MealTicket> findAvailableForUpdate(@Param("ownerId") Long ownerId, @Param("date") LocalDate date,
        @Param("type") TicketType type, @Param("status") TicketStatus status);
}
