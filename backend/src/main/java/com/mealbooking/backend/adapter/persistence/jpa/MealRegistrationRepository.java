package com.mealbooking.backend.adapter.persistence.jpa;

import com.mealbooking.backend.application.port.out.MealRegistrationStore;
import com.mealbooking.backend.domain.model.MealRegistration;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MealRegistrationRepository extends JpaRepository<MealRegistration, Long>, MealRegistrationStore {
    Optional<MealRegistration> findByUserIdAndMonthAndYear(Long userId, Integer month, Integer year);
}
