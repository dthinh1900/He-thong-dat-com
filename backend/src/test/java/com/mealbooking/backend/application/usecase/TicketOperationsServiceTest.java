package com.mealbooking.backend.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.mealbooking.backend.application.model.TicketCommands.*;
import com.mealbooking.backend.domain.model.*;
import com.mealbooking.backend.domain.enums.*;
import com.mealbooking.backend.domain.error.BusinessException;
import com.mealbooking.backend.application.port.out.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TicketOperationsServiceTest {
    private final TicketPurchaseStore purchases = mock(TicketPurchaseStore.class);
    private final TicketExchangeStore offers = mock(TicketExchangeStore.class);
    private final TicketExchangeTransactionStore transactions = mock(TicketExchangeTransactionStore.class);
    private final MealTicketStore tickets = mock(MealTicketStore.class);
    private final MealPriceStore prices = mock(MealPriceStore.class);
    private final UserStore users = mock(UserStore.class);
    private final AuditLogStore audit = mock(AuditLogStore.class);
    private final LocalDate date = LocalDate.of(2027, 1, 4);
    private final Clock clock = Clock.fixed(Instant.parse("2027-01-04T01:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
    private final TicketOperationsService service =
        new TicketOperationsService(purchases, offers, transactions, tickets, prices, users, audit, clock);

    @Test
    void rejectsSellWhenInventoryIsShortBeforeChangingAnyTicket() {
        when(users.findForUpdate(1L)).thenReturn(Optional.of(user(1L)));
        when(tickets.findAvailableForUpdate(1L, date, TicketType.NORMAL, TicketStatus.AVAILABLE))
            .thenReturn(List.of(ticket(10L, user(1L), TicketStatus.AVAILABLE)));
        assertThatThrownBy(() -> service.listForSale(1L, new ExchangeInput(ExchangeType.SELL, date, TicketType.NORMAL, 2)))
            .isInstanceOf(BusinessException.class);
        verifyNoInteractions(offers, transactions, audit);
    }

    @Test
    void partialMatchTransfersOnlyChosenTicketAndWritesOneTransaction() {
        var seller = user(1L);
        var buyer = user(2L);
        var a = ticket(10L, seller, TicketStatus.LISTED_FOR_EXCHANGE);
        var b = ticket(11L, seller, TicketStatus.LISTED_FOR_EXCHANGE);
        var first = offer(100L, a);
        var second = offer(101L, b);
        when(offers.findSaleGroupForUpdate(100L, ExchangeType.SELL)).thenReturn(List.of(first, second));
        when(tickets.findForUpdate(10L)).thenReturn(Optional.of(a));
        when(users.findById(2L)).thenReturn(Optional.of(buyer));
        service.matchSale(2L, 100L, 1);
        assertThat(a.getOwner()).isSameAs(buyer);
        assertThat(a.getStatus()).isEqualTo(TicketStatus.AVAILABLE);
        assertThat(first.getStatus()).isEqualTo(ExchangeStatus.COMPLETED);
        assertThat(b.getOwner()).isSameAs(seller);
        assertThat(second.getStatus()).isEqualTo(ExchangeStatus.OPEN);
        verify(transactions).save(argThat(x -> x.getTicket() == a && x.getBuyer() == buyer && x.getSeller() == seller));
    }

    @Test
    void approvalUsesRequestedSnapshotAndCannotIssueTwice() {
        var request = new TicketPurchase();
        var requester = user(1L);
        var admin = user(2L);
        var price = new MealPrice();
        price.setPrice(new BigDecimal("35000"));
        request.setId(4L);
        request.setRequester(requester);
        request.setMealDate(date);
        request.setTicketType(TicketType.NORMAL);
        request.setQuantity(2);
        request.setQuotedPrice(price);
        request.setTotalAmount(new BigDecimal("50000"));
        request.setStatus(PurchaseStatus.PENDING);
        when(purchases.findForUpdate(4L)).thenReturn(Optional.of(request));
        when(users.getReferenceById(2L)).thenReturn(admin);
        service.decidePurchase(2L, 4L, PurchaseStatus.APPROVED);
        assertThat(request.getStatus()).isEqualTo(PurchaseStatus.APPROVED);
        verify(tickets, times(2)).save(argThat(t -> t.getPurchaseRequest() == request
            && t.getOwner() == requester && t.getPrice().compareTo(new BigDecimal("25000")) == 0));
        assertThatThrownBy(() -> service.decidePurchase(2L, 4L, PurchaseStatus.APPROVED))
            .isInstanceOf(BusinessException.class);
        verifyNoMoreInteractions(tickets);
    }

    private User user(long id) {
        var user = new User();
        user.setId(id);
        user.setFullName("Employee " + id);
        return user;
    }

    private MealTicket ticket(long id, User owner, TicketStatus status) {
        var ticket = new MealTicket();
        ticket.setId(id);
        ticket.setOwner(owner);
        ticket.setMealDate(date);
        ticket.setTicketType(TicketType.NORMAL);
        ticket.setPrice(new BigDecimal("25000"));
        ticket.setStatus(status);
        return ticket;
    }

    private TicketExchange offer(long id, MealTicket ticket) {
        var offer = new TicketExchange();
        offer.setId(id);
        offer.setRequester(ticket.getOwner());
        offer.setExchangeType(ExchangeType.SELL);
        offer.setMealDate(date);
        offer.setTicketType(TicketType.NORMAL);
        offer.setTicket(ticket);
        offer.setQuantityRequested(1);
        offer.setStatus(ExchangeStatus.OPEN);
        return offer;
    }
}
