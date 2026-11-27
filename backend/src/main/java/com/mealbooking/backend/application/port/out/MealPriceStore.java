package com.mealbooking.backend.application.port.out;

import com.mealbooking.backend.domain.model.MealPrice;
import com.mealbooking.backend.domain.enums.TicketType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MealPriceStore {
    List<MealPrice> findByTicketTypeOrderByEffectiveFromDesc(TicketType type);

    List<MealPrice> findOverlappingSchedule(LocalDate firstDate, LocalDate lastDate);

    Optional<MealPrice> findApplicablePrice(TicketType type, LocalDate date);

    List<MealPrice> findAllByOrderByTicketTypeAscEffectiveFromDesc();

    <S extends MealPrice> S saveAndFlush(S price);
}
