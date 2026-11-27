package com.mealbooking.backend.application.usecase;

import com.mealbooking.backend.application.port.out.PasswordHasher;
import com.mealbooking.backend.application.port.out.UserStore;
import com.mealbooking.backend.application.port.in.AccountProfiles;
import com.mealbooking.backend.application.model.UpdateProfile;
import com.mealbooking.backend.application.model.UserResponse;
import com.mealbooking.backend.domain.model.User;
import com.mealbooking.backend.domain.error.BusinessException;

public class UserService implements AccountProfiles {
    private final UserStore userRepository;
    private final PasswordHasher passwordEncoder;
    public UserService(UserStore userRepository, PasswordHasher passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override

    public UserResponse getProfile(Long userId) {
        return toResponse(findUser(userId));
    }

    @Override

    public UserResponse updateProfile(Long userId, UpdateProfile request) {
        User user = findUser(userId);
        user.setFullName(request.fullName().trim());
        user.setEmail(blankToNull(request.email()));
        user.setPhone(blankToNull(request.phone()));
        if (request.newPassword() != null && !request.newPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.newPassword()));
        }
        return toResponse(userRepository.save(user));
    }

    public User findUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> BusinessException.notFound("Không tìm thấy người dùng."));
    }

    private static UserResponse toResponse(User user) {
        return new UserResponse(
            user.getId(), user.getEmployeeCode(), user.getUsername(), user.getFullName(),
            user.getEmail(), user.getPhone(),
            user.getDepartment() == null ? null : user.getDepartment().getName(),
            user.getRole(), user.getActive(), user.getCreatedAt(), user.getUpdatedAt()
        );
    }

    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
