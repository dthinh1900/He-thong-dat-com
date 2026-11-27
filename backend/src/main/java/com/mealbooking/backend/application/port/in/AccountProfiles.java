package com.mealbooking.backend.application.port.in;

import com.mealbooking.backend.application.model.UpdateProfile;
import com.mealbooking.backend.application.model.UserResponse;

public interface AccountProfiles {
    UserResponse getProfile(Long userId);

    UserResponse updateProfile(Long userId, UpdateProfile request);
}
