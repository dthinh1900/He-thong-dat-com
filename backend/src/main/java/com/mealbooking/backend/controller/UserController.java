
package com.mealbooking.backend.controller;

import com.mealbooking.backend.dto.request.UpdateProfileRequest;
import com.mealbooking.backend.application.model.TicketResponses.PersonalMealHistory;
import com.mealbooking.backend.application.model.UserResponse;
import com.mealbooking.backend.security.UserPrincipal;
import com.mealbooking.backend.application.port.in.AccountProfiles;
import com.mealbooking.backend.application.port.in.ReadPersonalMealHistory;

import jakarta.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me")
public class UserController {

    private final AccountProfiles userService;
    private final ReadPersonalMealHistory ticketHistory;

    public UserController(AccountProfiles userService, ReadPersonalMealHistory ticketHistory) {
        this.userService = userService;
        this.ticketHistory = ticketHistory;
    }

    @GetMapping("/meal-history")
    public PersonalMealHistory mealHistory(@AuthenticationPrincipal UserPrincipal principal,
            @RequestParam int month, @RequestParam int year) {
        return ticketHistory.personalMealHistory(principal.user().getId(), month, year);
    }

    @GetMapping
    public UserResponse getMe(
            @AuthenticationPrincipal UserPrincipal principal) {

        return userService.getProfile(principal.user().getId());
    }

    @PatchMapping
    public UserResponse updateMe(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request) {

        return userService.updateProfile(
                principal.user().getId(),
                request.toCommand());
    }
}
