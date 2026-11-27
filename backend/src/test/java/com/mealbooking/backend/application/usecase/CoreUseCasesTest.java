package com.mealbooking.backend.application.usecase;

import com.mealbooking.backend.application.model.AuthenticatedAccount;
import com.mealbooking.backend.application.port.in.AccountProfiles;
import com.mealbooking.backend.application.port.out.AccountAuthenticator;
import com.mealbooking.backend.application.port.out.MealHistoryReader;
import com.mealbooking.backend.application.port.out.SessionTokenIssuer;
import com.mealbooking.backend.domain.error.BusinessException;
import com.mealbooking.backend.application.port.out.MealHistoryReader.OwnedTicket;
import com.mealbooking.backend.application.model.TicketResponses.MealHistoryEntry;
import com.mealbooking.backend.application.model.TicketResponses.MealHistorySource;
import com.mealbooking.backend.application.model.UserResponse;
import com.mealbooking.backend.domain.enums.Role;
import com.mealbooking.backend.domain.enums.TicketStatus;
import com.mealbooking.backend.domain.enums.TicketType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CoreUseCasesTest {
    @Test
    void signsInThroughPortsAndReturnsExistingResponse() {
        var account = new AuthenticatedAccount(12L, "nv-test", Role.USER);
        var profile = new UserResponse(12L, "NV", "nv-test", "Test User", null, null, null, Role.USER, true, null, null);
        var authenticator = mock(AccountAuthenticator.class);
        var tokens = mock(SessionTokenIssuer.class);
        var profiles = mock(AccountProfiles.class);
        when(authenticator.verify("nv-test", "fixture-password")).thenReturn(account);
        when(tokens.issue(account)).thenReturn("fixture-token");
        when(profiles.getProfile(12L)).thenReturn(profile);

        var response = new SignInUseCase(authenticator, tokens, profiles).login(" nv-test ", "fixture-password");

        assertThat(response.token()).isEqualTo("fixture-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.user()).isEqualTo(profile);
    }

    @Test
    void personalHistoryUsesOwnerAndMonthAndExcludesCancelledTicketFromTotal() {
        var reader = mock(MealHistoryReader.class);
        var from = LocalDate.of(2028, 2, 1);
        var to = LocalDate.of(2028, 2, 29);
        var active = new OwnedTicket(1, from, TicketType.NORMAL, new BigDecimal("25000"), TicketStatus.AVAILABLE,
            MealHistorySource.REGISTRATION);
        var cancelled = new OwnedTicket(2, to, TicketType.SPECIAL, new BigDecimal("40000"), TicketStatus.CANCELLED,
            MealHistorySource.PURCHASE);
        when(reader.findOwnedTickets(12L, from, to)).thenReturn(List.of(active, cancelled));

        var history = new PersonalMealHistoryUseCase(reader).personalMealHistory(12L, 2, 2028);

        assertThat(history.totalTickets()).isEqualTo(1);
        assertThat(history.totalAmount()).isEqualByComparingTo("25000");
        assertThat(history.tickets()).extracting(MealHistoryEntry::ticketId).containsExactly(active.id(), cancelled.id());
        assertThat(history.tickets()).extracting(MealHistoryEntry::billable).containsExactly(true, false);
        verify(reader).findOwnedTickets(12L, from, to);
    }

    @Test
    void rejectsInvalidMonthBeforeReadingAnyTickets() {
        var reader = mock(MealHistoryReader.class);
        var useCase = new PersonalMealHistoryUseCase(reader);

        assertThatThrownBy(() -> useCase.personalMealHistory(12L, 13, 2028))
            .isInstanceOfSatisfying(BusinessException.class,
                error -> assertThat(error.rejection()).isEqualTo(BusinessException.Rejection.INVALID_INPUT));
        verifyNoInteractions(reader);
    }
}
