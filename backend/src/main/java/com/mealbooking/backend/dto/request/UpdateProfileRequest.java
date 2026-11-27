package com.mealbooking.backend.dto.request;

import com.mealbooking.backend.application.model.UpdateProfile;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
    @NotBlank @Size(max = 160) String fullName,
    @Email @Size(max = 160) String email,
    @Size(max = 30) String phone,
    @Size(min = 8, max = 72) String newPassword
) {
    public UpdateProfile toCommand() {
        return new UpdateProfile(fullName, email, phone, newPassword);
    }
}
