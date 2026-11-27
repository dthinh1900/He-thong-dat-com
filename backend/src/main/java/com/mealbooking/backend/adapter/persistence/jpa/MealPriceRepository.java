package com.mealbooking.backend.adapter.persistence.jpa;

import com.mealbooking.backend.application.port.out.MealPriceStore;
import com.mealbooking.backend.domain.model.MealPrice;
import com.mealbooking.backend.domain.enums.TicketType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MealPriceRepository extends JpaRepository<MealPrice, Long>, MealPriceStore {
    List<MealPrice> findByTicketTypeOrderByEffectiveFromDesc(TicketType type);

    @Query("""
            select p from MealPrice p
            where p.effectiveFrom <= :lastDate and (p.effectiveTo is null or p.effectiveTo >= :firstDate)
            order by p.effectiveFrom desc
            """)
    List<MealPrice> findOverlappingSchedule(@Param("firstDate") LocalDate firstDate,
                                           @Param("lastDate") LocalDate lastDate);
    @Query("select p from MealPrice p where p.ticketType = :type and p.effectiveFrom <= :date and (p.effectiveTo is null or p.effectiveTo >= :date) order by p.effectiveFrom desc")
    Optional<MealPrice> findApplicablePrice(@Param("type") TicketType type, @Param("date") LocalDate date);
}
