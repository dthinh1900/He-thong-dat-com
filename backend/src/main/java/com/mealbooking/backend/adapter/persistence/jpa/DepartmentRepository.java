package com.mealbooking.backend.adapter.persistence.jpa;

import com.mealbooking.backend.application.port.out.DepartmentStore;
import com.mealbooking.backend.domain.model.Department;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department, Long>, DepartmentStore {
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
    Optional<Department> findByName(String name);
}
