package com.mealbooking.backend.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.mealbooking.backend.application.model.SaveMonthRegistration;
import com.mealbooking.backend.application.model.AdminResponses.Settings;
import com.mealbooking.backend.domain.model.*;
import com.mealbooking.backend.domain.enums.*;
import com.mealbooking.backend.domain.error.BusinessException;
import com.mealbooking.backend.application.port.out.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import com.mealbooking.backend.application.port.out.RegistrationSettingsStore;

class MealRegistrationLedgerTest {
    private final LocalDate date = LocalDate.of(2027, 1, 4);
    private final MealRegistrationStore registrations = mock(MealRegistrationStore.class);
    private final MealTicketStore tickets = mock(MealTicketStore.class);
    private final MealPriceStore prices = mock(MealPriceStore.class);
    private final AuditLogStore audit = mock(AuditLogStore.class);
    private final UserStore users = mock(UserStore.class);
    private final RegistrationPolicy policy = mock(RegistrationPolicy.class);
    private final Clock clock = Clock.fixed(Instant.parse("2027-01-03T02:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
    private final MealRegistrationService service =
        new MealRegistrationService(registrations, tickets, prices, audit, users, clock, policy);

    @Test
    void countsAllCurrentOwnedVouchersAndLocksTransferredRegistration() {
        var original = new MealRegistration();
        original.setId(1L);
        when(registrations.findByUserIdAndMonthAndYear(1L, 1, 2027)).thenReturn(Optional.of(original));
        when(tickets.findByRegistrationId(1L)).thenReturn(List.of(ticket(10L, 2L, TicketStatus.AVAILABLE, "24000")));
        when(tickets.findByOwnerIdAndMealDateBetweenOrderByMealDate(1L, date.withDayOfMonth(1), date.withDayOfMonth(31)))
            .thenReturn(List.of(ticket(11L, 1L, TicketStatus.LISTED_FOR_EXCHANGE, "25000"),
                ticket(12L, 1L, TicketStatus.USED, "27000"),
                ticket(13L, 1L, TicketStatus.CANCELLED, "30000")));
        when(policy.settings()).thenReturn(new Settings(LocalTime.of(14,45), LocalTime.of(14,45)));

        var month = service.getMonth(1L, 1, 2027);
        var day = month.days().get(date.getDayOfMonth() - 1);
        assertThat(month.selectedCount()).isEqualTo(2);
        assertThat(month.totalAmount()).isEqualByComparingTo("52000");
        assertThat(day.selected()).isFalse();
        assertThat(day.editable()).isFalse();
        assertThat(day.ticketCount()).isEqualTo(2);
        assertThat(day.ownedAmount()).isEqualByComparingTo("52000");
    }

    @Test
    void loadsMonthlyPriceScheduleOnceAndKeepsDateBoundaries() {
        var first = LocalDate.of(2027, 1, 1);
        when(policy.settings()).thenReturn(new Settings(LocalTime.of(14, 45), LocalTime.of(14, 45)));
        when(prices.findOverlappingSchedule(first, first.withDayOfMonth(31))).thenReturn(List.of(
            price(TicketType.NORMAL, "30000", first.plusDays(15), null),
            price(TicketType.SPECIAL, "44500", first.plusDays(7), first.plusDays(7)),
            price(TicketType.NORMAL, "25000", first.minusMonths(1), first.plusDays(14))));

        var days = service.getMonth(1L, 1, 2027).days();
        assertThat(days.get(14).normalPrice()).isEqualByComparingTo("25000");
        assertThat(days.get(15).normalPrice()).isEqualByComparingTo("30000");
        assertThat(days.get(7).specialPrice()).isEqualByComparingTo("44500");
        assertThat(days.get(8).specialPrice()).isNull();
        verify(prices).findOverlappingSchedule(first, first.withDayOfMonth(31));
        verifyNoMoreInteractions(prices);
    }

    @Test
    void adminCannotCancelListedTicketBehindAnOpenSale() {
        var listed = ticket(10L, 1L, TicketStatus.LISTED_FOR_EXCHANGE, "24000");
        when(tickets.findForUpdate(10L)).thenReturn(Optional.of(listed));
        assertThatThrownBy(() -> service.cancelTicketByAdmin(2L, 10L, null, false)).isInstanceOf(BusinessException.class);
        assertThat(listed.getStatus()).isEqualTo(TicketStatus.LISTED_FOR_EXCHANGE);
        verifyNoInteractions(audit);
    }

    @Test
    void fridayCanBeUpgradedBeforeThursdayCutoffWithConfiguredPrice() {
        var friday = LocalDate.of(2027, 1, 8);
        var employee = new User();
        employee.setId(1L);
        when(users.findForUpdate(1L)).thenReturn(Optional.of(employee));
        var special = price(TicketType.SPECIAL, "40000", friday, null);
        when(prices.findApplicablePrice(TicketType.SPECIAL, friday)).thenReturn(Optional.of(special));
        var registration = new MealRegistration();
        registration.setId(1L);
        when(registrations.findByUserIdAndMonthAndYear(1L, 1, 2027)).thenReturn(Optional.of(registration));
        var existing = ticket(9L, 1L, TicketStatus.AVAILABLE, "25000");
        existing.setMealDate(friday);
        when(tickets.findByRegistrationForUpdate(1L)).thenReturn(List.of(existing));
        var time = ZonedDateTime.of(2027, 1, 7, 14, 44, 0, 0, ZoneId.of("Asia/Ho_Chi_Minh"));
        var input = new SaveMonthRegistration(1, 2027, List.of(friday), Map.of(friday, TicketType.SPECIAL));
        var thursday = friday.minusDays(1);

        assertThatThrownBy(() -> at(time.plusMinutes(1)).saveMonth(1L, input))
            .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> at(time.minusDays(1)).saveMonth(1L,
            new SaveMonthRegistration(1, 2027, List.of(thursday), Map.of(thursday, TicketType.SPECIAL))))
            .isInstanceOf(BusinessException.class);
        verifyNoInteractions(audit);
        at(time.minusDays(1)).saveMonth(1L, input);
        at(time.plusMinutes(1)).saveMonth(1L, input);
        verify(tickets).save(existing);
        assertThat(existing.getTicketType()).isEqualTo(TicketType.SPECIAL);
        assertThat(existing.getMealPrice()).isSameAs(special);
        assertThat(existing.getPrice()).isEqualByComparingTo("40000");
    }

    @Test
    void fridayTypeEditabilityFollowsRegistrationDeadlineEvenWithDifferentCancellationDeadline() {
        var friday = LocalDate.of(2027, 1, 8);
        var time = ZonedDateTime.of(2027, 1, 7, 14, 44, 0, 0, ZoneId.of("Asia/Ho_Chi_Minh"));
        var registration = new MealRegistration();
        registration.setId(1L);
        var existing = ticket(9L, 1L, TicketStatus.AVAILABLE, "25000");
        existing.setMealDate(friday);
        when(registrations.findByUserIdAndMonthAndYear(1L, 1, 2027)).thenReturn(Optional.of(registration));
        when(tickets.findByRegistrationId(1L)).thenReturn(List.of(existing));

        var before = at(time).getMonth(1L, 1, 2027).days().get(7);
        var after = at(time.plusMinutes(1)).getMonth(1L, 1, 2027).days().get(7);
        assertThat(before.editable()).isFalse();
        assertThat(before.typeEditable()).isTrue();
        assertThat(after.typeEditable()).isFalse();
    }

    @Test
    void showsCancellationDetailsForOwnedTicketsWithoutBillingThem() {
        var admin = new User();
        admin.setId(2L);
        when(users.getReferenceById(2L)).thenReturn(admin);
        var cancelled = ticket(10L, 1L, TicketStatus.AVAILABLE, "25000");
        when(tickets.findForUpdate(10L)).thenReturn(Optional.of(cancelled));
        when(tickets.findByOwnerIdAndMealDateBetweenOrderByMealDate(1L, date.withDayOfMonth(1), date.withDayOfMonth(31)))
            .thenReturn(List.of(cancelled, ticket(11L, 1L, TicketStatus.AVAILABLE, "26000"),
                ticket(12L, 1L, TicketStatus.CANCELLED, "24000")));
        when(policy.settings()).thenReturn(new Settings(LocalTime.of(14, 45), LocalTime.of(14, 45)));

        service.cancelTicketByAdmin(2L, 10L, "  Bếp nghỉ  ", false);
        var month = service.getMonth(1L, 1, 2027);
        var day = month.days().get(date.getDayOfMonth() - 1);
        var entries = day.cancelledTickets();

        assertThat(month.totalAmount()).isEqualByComparingTo("26000");
        assertThat(day.ticketCount()).isEqualTo(1);
        assertThat(entries).hasSize(1);
        assertThat(entries.getFirst().source()).isEqualTo(CancellationSource.ADMIN);
        assertThat(entries.getFirst().reason()).isEqualTo("Bếp nghỉ");
        assertThat(entries.getFirst().cancelledAt()).isEqualTo(LocalDateTime.now(clock));
    }

    @Test
    void savesWithdrawalWithoutMarkingCancellationAndRejectsItAtCutoff() {
        var employee = new User();
        employee.setId(1L);
        var registration = new MealRegistration();
        registration.setId(1L);
        registration.setUser(employee);
        var booked = ticket(50L, 1L, TicketStatus.AVAILABLE, "25000");
        booked.setRegistration(registration);
        when(users.findForUpdate(1L)).thenReturn(Optional.of(employee));
        when(registrations.findByUserIdAndMonthAndYear(1L, 1, 2027)).thenReturn(Optional.of(registration));
        when(tickets.findByRegistrationForUpdate(1L)).thenReturn(List.of(booked));
        when(tickets.findByRegistrationId(1L)).thenReturn(List.of(booked));
        when(tickets.findByOwnerIdAndMealDateBetweenOrderByMealDate(1L, date.withDayOfMonth(1), date.withDayOfMonth(31)))
            .thenReturn(List.of(booked));
        var deadline = ZonedDateTime.of(2027, 1, 3, 12, 0, 0, 0, ZoneId.of("Asia/Ho_Chi_Minh"));
        var draft = new SaveMonthRegistration(1, 2027, List.of(), Map.of());

        assertThatThrownBy(() -> at(deadline).saveMonth(1L, draft)).isInstanceOf(BusinessException.class);
        assertThat(booked.getStatus()).isEqualTo(TicketStatus.AVAILABLE);
        verifyNoInteractions(audit);

        var result = at(deadline.minusMinutes(1)).saveMonth(1L, draft);
        var day = result.days().get(date.getDayOfMonth() - 1);
        assertThat(booked.getStatus()).isEqualTo(TicketStatus.WITHDRAWN);
        assertThat(booked.getCancelledAt()).isNull();
        assertThat(day.selected()).isFalse();
        assertThat(day.cancelledTickets()).isEmpty();
        assertThat(result.totalAmount()).isEqualByComparingTo("0");
        verify(audit).save(argThat(log -> log.getAction() == AuditAction.WITHDRAW_TICKET));
    }

    private MealRegistrationService at(ZonedDateTime time) {
        var fixed = Clock.fixed(time.toInstant(), time.getZone());
        var realPolicy = spy(new RegistrationPolicy(mock(RegistrationSettingsStore.class), fixed, true));
        doReturn(new Settings(LocalTime.of(14, 45), LocalTime.of(12, 0))).when(realPolicy).settings();
        return new MealRegistrationService(registrations, tickets, prices, audit, users, fixed, realPolicy);
    }

    private static MealPrice price(TicketType type, String value, LocalDate start, LocalDate end) {
        var price = new MealPrice();
        price.setTicketType(type);
        price.setPrice(new BigDecimal(value));
        price.setEffectiveFrom(start);
        price.setEffectiveTo(end);
        return price;
    }

    private MealTicket ticket(long id, long ownerId, TicketStatus status, String price) {
        var owner = new User();
        owner.setId(ownerId);
        var ticket = new MealTicket();
        ticket.setId(id);
        ticket.setOwner(owner);
        ticket.setMealDate(date);
        ticket.setPrice(new BigDecimal(price));
        ticket.setStatus(status);
        ticket.setTicketType(TicketType.NORMAL);
        return ticket;
    }
}
