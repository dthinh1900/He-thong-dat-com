package com.mealbooking.backend.application.port.out;

import com.mealbooking.backend.domain.model.User;
import com.mealbooking.backend.domain.enums.Role;
import java.util.List;
import java.util.Optional;

public interface UserStore {
    Optional<User> findById(Long id);

    User getReferenceById(Long id);

    Optional<User> findForUpdate(Long id);

    boolean existsById(Long id);

    <S extends User> S save(S user);

    <S extends User> S saveAndFlush(S user);

    long count();

    long countByDepartmentId(Long id);

    long countByRoleAndActiveTrue(Role role);

    List<User> findAllByOrderByFullNameAscIdAsc();

    boolean existsByUsernameIgnoreCaseAndIdNot(String username, Long id);

    boolean existsByEmployeeCodeIgnoreCaseAndIdNot(String employeeCode, Long id);
}
