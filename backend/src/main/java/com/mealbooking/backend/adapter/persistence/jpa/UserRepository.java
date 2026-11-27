package com.mealbooking.backend.adapter.persistence.jpa;

import com.mealbooking.backend.application.port.out.UserStore;
import com.mealbooking.backend.domain.model.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long>, UserStore {
    Optional<User> findByUsername(String username);
    // Serialize writes for an account without blocking foreign-key checks for ticket transfers.
    @org.springframework.data.jpa.repository.Query(value = "select * from users where id = :id for no key update", nativeQuery = true)
    Optional<User> findForUpdate(@org.springframework.data.repository.query.Param("id") Long id);
    long countByDepartmentId(Long id);
    long countByRoleAndActiveTrue(com.mealbooking.backend.domain.enums.Role role);
    boolean existsByUsernameIgnoreCaseAndIdNot(String username,Long id);
    boolean existsByEmployeeCodeIgnoreCaseAndIdNot(String employeeCode,Long id);
    boolean existsByUsername(String username);
    boolean existsByEmployeeCode(String employeeCode);
}
