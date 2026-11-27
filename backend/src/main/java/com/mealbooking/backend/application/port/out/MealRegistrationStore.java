package com.mealbooking.backend.application.port.out;

import com.mealbooking.backend.domain.model.MealRegistration;
import java.util.Optional;

public interface MealRegistrationStore {
    Optional<MealRegistration> findByUserIdAndMonthAndYear(Long userId, Integer month, Integer year);

    <S extends MealRegistration> S save(S registration);
}
