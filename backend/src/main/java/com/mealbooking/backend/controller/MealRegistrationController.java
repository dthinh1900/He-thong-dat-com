package com.mealbooking.backend.controller;

import com.mealbooking.backend.domain.error.BusinessException;
import com.mealbooking.backend.dto.request.MonthRegistrationRequest;
import com.mealbooking.backend.application.model.MonthRegistrationResponse;
import com.mealbooking.backend.security.UserPrincipal;
import com.mealbooking.backend.application.port.in.MealRegistrations;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Validated
public class MealRegistrationController {
    private final MealRegistrations registrationService;

    public MealRegistrationController(MealRegistrations registrationService) {
        this.registrationService = registrationService;
    }

    @GetMapping("/meal-registrations/month")
    public MonthRegistrationResponse getMonth(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam @Min(1) @Max(12) int month,
            @RequestParam @Min(2000) @Max(2200) int year) {
        return registrationService.getMonth(principal.user().getId(), month, year);
    }

    @PostMapping("/meal-registrations")
    public MonthRegistrationResponse saveMonth(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody MonthRegistrationRequest request) {
        return registrationService.saveMonth(principal.user().getId(), request.toCommand());
    }

    @PatchMapping("/meal-tickets/{ticketId}/cancel")
    public void rejectDirectCancellation() {
        throw BusinessException.forbidden("Hãy lưu lựa chọn trên lịch trước hạn hoặc liên hệ quản trị viên để hủy vé đã khóa.");
    }
}
