package com.mealbooking.backend.application.port.out;

import com.mealbooking.backend.domain.model.Department;
import java.util.Optional;

public interface DepartmentStore {
    Optional<Department> findById(Long id);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    <S extends Department> S saveAndFlush(S department);
}
