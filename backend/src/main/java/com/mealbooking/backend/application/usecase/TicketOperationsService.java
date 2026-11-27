package com.mealbooking.backend.application.usecase;

import com.mealbooking.backend.application.port.out.UserStore;
import com.mealbooking.backend.application.port.out.TicketPurchaseStore;
import com.mealbooking.backend.application.port.out.TicketExchangeTransactionStore;
import com.mealbooking.backend.application.port.out.TicketExchangeStore;
import com.mealbooking.backend.application.port.out.MealTicketStore;
import com.mealbooking.backend.application.port.out.MealRegistrationStore;
import com.mealbooking.backend.application.port.out.MealPriceStore;
import com.mealbooking.backend.application.port.out.AuditLogStore;
import com.mealbooking.backend.application.port.in.TicketOperations;
import com.mealbooking.backend.application.model.TicketCommands.*;
import com.mealbooking.backend.application.model.TicketResponses.*;
import com.mealbooking.backend.domain.model.*;
import com.mealbooking.backend.domain.enums.*;
import com.mealbooking.backend.domain.error.BusinessException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;

public class TicketOperationsService implements TicketOperations {
    private final TicketPurchaseStore purchases;
    private final TicketExchangeStore offers;
    private final TicketExchangeTransactionStore transactions;
    private final MealTicketStore tickets;
    private final MealRegistrationStore registrations;
    private final MealPriceStore prices;
    private final UserStore users;
    private final AuditLogStore audit;
    private final Clock clock;

    public TicketOperationsService(TicketPurchaseStore purchases, TicketExchangeStore offers,
            TicketExchangeTransactionStore transactions, MealTicketStore tickets,
            MealPriceStore prices, UserStore users, AuditLogStore audit, Clock clock) {
        this(purchases, offers, transactions, tickets, null, prices, users, audit, clock);
    }

    public TicketOperationsService(TicketPurchaseStore purchases, TicketExchangeStore offers,
            TicketExchangeTransactionStore transactions, MealTicketStore tickets, MealRegistrationStore registrations,
            MealPriceStore prices, UserStore users, AuditLogStore audit, Clock clock) {
        this.purchases = purchases;
        this.offers = offers;
        this.transactions = transactions;
        this.tickets = tickets;
        this.registrations = registrations;
        this.prices = prices;
        this.users = users;
        this.audit = audit;
        this.clock = clock;
    }

    @Override
    public List<PurchaseRow> purchases(Long userId) {
        var rows = userId == null ? purchases.findAllByOrderByCreatedAtDesc()
            : purchases.findByRequesterIdOrderByCreatedAtDesc(userId);
        return rows.stream().map(TicketOperationsService::purchaseRow).toList();
    }

    @Override
    public PurchaseQuote quotePurchase(LocalDate date, TicketType type) {
        validateMeal(date, type);
        return new PurchaseQuote(applicablePrice(date, type).getPrice());
    }

    @Override
    public PurchaseRow requestPurchase(Long userId, PurchaseInput input) {
        validateMeal(input.mealDate(), input.ticketType());
        var requester = activeUser(userId);
        var price = applicablePrice(input.mealDate(), input.ticketType());
        if (input.expectedUnitPrice() != null && input.expectedUnitPrice().compareTo(price.getPrice()) != 0) {
            throw BusinessException.conflict("Đơn giá đã thay đổi. Vui lòng xem giá mới rồi gửi lại yêu cầu.");
        }
        var request = new TicketPurchase();
        request.setPurchaseCode(code("MT"));
        request.setRequester(requester);
        request.setMealDate(input.mealDate());
        request.setTicketType(input.ticketType());
        request.setQuantity(input.quantity());
        request.setQuotedPrice(price);
        request.setTotalAmount(price.getPrice().multiply(BigDecimal.valueOf(input.quantity())));
        request.setNote(input.reason().strip());
        request.setStatus(PurchaseStatus.PENDING);
        request.setCreatedAt(LocalDateTime.now(clock));
        purchases.save(request);
        log(requester, AuditAction.REQUEST_PURCHASE, null, null, request.getPurchaseCode());
        return purchaseRow(request);
    }

    @Override
    public PurchaseRow decidePurchase(Long adminId, Long id, PurchaseStatus decision) {
        if (decision != PurchaseStatus.APPROVED && decision != PurchaseStatus.REJECTED) {
            throw BusinessException.badRequest("Chọn duyệt hoặc từ chối yêu cầu.");
        }
        var request = purchases.findForUpdate(id)
            .orElseThrow(() -> BusinessException.notFound("Không tìm thấy yêu cầu mua thêm."));
        if (request.getStatus() != PurchaseStatus.PENDING) {
            throw BusinessException.conflict("Yêu cầu đã được xử lý.");
        }
        var reviewer = users.getReferenceById(adminId);
        if (decision == PurchaseStatus.APPROVED) {
            validateMeal(request.getMealDate(), request.getTicketType());
            if (!Boolean.TRUE.equals(request.getRequester().getActive())) {
                throw BusinessException.conflict("Tài khoản người yêu cầu đã ngừng hoạt động.");
            }
            issuePurchaseTickets(request);
        }
        request.setStatus(decision);
        request.setReviewedBy(reviewer);
        request.setReviewedAt(LocalDateTime.now(clock));
        log(reviewer, decision == PurchaseStatus.APPROVED ? AuditAction.APPROVE_PURCHASE : AuditAction.REJECT_PURCHASE,
            null, null, request.getPurchaseCode());
        return purchaseRow(request);
    }

    private void issuePurchaseTickets(TicketPurchase request) {
        var price = request.getQuotedPrice();
        if (price == null) {
            price = applicablePrice(request.getMealDate(), request.getTicketType());
            request.setQuotedPrice(price);
        }
        int quantity = request.getQuantity();
        var total = request.getTotalAmount();
        if (total == null) {
            total = price.getPrice().multiply(BigDecimal.valueOf(quantity));
            request.setTotalAmount(total);
        }
        var unit = total.divide(BigDecimal.valueOf(quantity), 2, RoundingMode.DOWN);
        for (int i = 0; i < quantity; i++) {
            var ticket = new MealTicket();
            ticket.setTicketCode(code("VE"));
            ticket.setPurchaseRequest(request);
            ticket.setOwner(request.getRequester());
            ticket.setMealDate(request.getMealDate());
            ticket.setTicketType(request.getTicketType());
            ticket.setMealPrice(price);
            ticket.setPrice(i == quantity - 1 ? total.subtract(unit.multiply(BigDecimal.valueOf(i))) : unit);
            ticket.setStatus(TicketStatus.AVAILABLE);
            ticket.setCreatedAt(LocalDateTime.now(clock));
            tickets.save(ticket);
        }
    }

    @Override
    public ExchangeBoard board(Long userId) {
        var today = LocalDate.now(clock);
        var market = groupSales(offers.findByExchangeTypeAndMealDateGreaterThanEqualOrderById(ExchangeType.SELL, today))
            .stream().filter(row -> row.remaining() > 0 && !row.userId().equals(userId)).toList();
        var mine = groupSales(offers.findByRequesterIdAndExchangeTypeOrderById(userId, ExchangeType.SELL));
        var history = transactions.findBySellerIdOrBuyerIdOrderByCompletedAtDesc(userId, userId).stream()
            .map(TicketOperationsService::transactionRow).toList();
        var owned = tickets.findByOwnerIdAndMealDateGreaterThanEqualAndStatusOrderByMealDateAscIdAsc(
            userId, today, TicketStatus.AVAILABLE).stream()
            .filter(t -> t.getMealDate().getDayOfWeek().getValue() < 6)
            .map(t -> new OwnedTicket(t.getId(), t.getMealDate(), t.getTicketType(), t.getPrice(), t.getStatus()))
            .toList();
        return new ExchangeBoard(market, mine, history, owned);
    }

    @Override
    public ExchangeRow listForSale(Long userId, ExchangeInput input) {
        if (input.exchangeType() != null && input.exchangeType() != ExchangeType.SELL) {
            throw BusinessException.badRequest("Chợ vé chỉ cho phép đăng bán vé đang sở hữu.");
        }
        validateMeal(input.mealDate(), input.ticketType());
        var seller = users.findForUpdate(userId)
            .filter(u -> Boolean.TRUE.equals(u.getActive()))
            .orElseThrow(() -> BusinessException.forbidden("Tài khoản đã ngừng hoạt động."));
        var stock = tickets.findAvailableForUpdate(userId, input.mealDate(), input.ticketType(), TicketStatus.AVAILABLE);
        if (stock.size() < input.quantity()) {
            throw BusinessException.conflict("Số vé đăng bán vượt số vé có thể chuyển nhượng.");
        }
        String group = code("BAN");
        var sales = new ArrayList<TicketExchange>();
        for (var ticket : stock.subList(0, input.quantity())) {
            var sale = new TicketExchange();
            sale.setExchangeCode(code("VE-BAN"));
            sale.setSaleGroup(group);
            sale.setRequester(seller);
            sale.setExchangeType(ExchangeType.SELL);
            sale.setTicket(ticket);
            sale.setMealDate(ticket.getMealDate());
            sale.setTicketType(ticket.getTicketType());
            sale.setQuantityRequested(1);
            sale.setStatus(ExchangeStatus.OPEN);
            sale.setCreatedAt(LocalDateTime.now(clock));
            ticket.setStatus(TicketStatus.LISTED_FOR_EXCHANGE);
            sales.add(offers.save(sale));
        }
        for (var sale : sales) {
            log(seller, AuditAction.CREATE_EXCHANGE, sale.getTicket(), null,
                "1 vé · ngày " + formatMealDate(sale.getMealDate())
                    + " · Người bán: " + seller.getFullName());
        }
        return saleRow(sales);
    }

    @Override
    public List<TransactionRow> matchSale(Long buyerId, Long id, int quantity) {
        var sales = lockedSales(id);
        var first = sales.getFirst();
        var seller = first.getRequester();
        if (seller.getId().equals(buyerId)) {
            throw BusinessException.forbidden("Không thể nhận vé do chính bạn đăng bán.");
        }
        validateMeal(first.getMealDate(), first.getTicketType());
        var remaining = openSales(sales);
        if (quantity < 1 || quantity > remaining.size() || !Boolean.TRUE.equals(seller.getActive())) {
            throw BusinessException.conflict("Bài bán không còn đủ vé. Hãy tải lại danh sách.");
        }
        var buyer = activeUser(buyerId);
        var matched = new ArrayList<TransactionRow>();
        for (var sale : remaining.subList(0, quantity)) {
            var ticket = lockedListedTicket(sale, seller.getId());
            ticket.setOwner(buyer);
            ticket.setStatus(TicketStatus.AVAILABLE);
            assignBuyerRegistration(ticket, buyer);
            sale.setQuantityCompleted(1);
            sale.setStatus(ExchangeStatus.COMPLETED);
            var now = LocalDateTime.now(clock);
            sale.setCompletedAt(now);
            var transfer = new TicketExchangeTransaction();
            transfer.setTransactionCode(code("GD"));
            transfer.setSellRequest(sale);
            transfer.setTicket(ticket);
            transfer.setSeller(seller);
            transfer.setBuyer(buyer);
            transfer.setAmount(ticket.getPrice());
            transfer.setCompletedAt(now);
            transactions.save(transfer);
            log(buyer, AuditAction.COMPLETE_EXCHANGE, ticket, transfer,
                "+1 vé · ngày " + formatMealDate(ticket.getMealDate())
                    + " · Người bán: " + seller.getFullName());
            matched.add(transactionRow(transfer));
        }
        return matched;
    }

    private void assignBuyerRegistration(MealTicket ticket, User buyer) {
        if (registrations == null) return; // Compatibility for isolated service tests using the legacy constructor.
        int month = ticket.getMealDate().getMonthValue();
        int year = ticket.getMealDate().getYear();
        var registration = registrations.findByUserIdAndMonthAndYear(buyer.getId(), month, year)
            .orElseGet(() -> {
                var created = new MealRegistration();
                created.setRegistrationCode("DK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
                created.setUser(buyer);
                created.setMonth(month);
                created.setYear(year);
                return registrations.save(created);
            });
        ticket.setRegistration(registration);
    }

    @Override
    public void cancelSale(Long userId, Long id) {
        var sales = lockedSales(id);
        var first = sales.getFirst();
        if (!first.getRequester().getId().equals(userId)) {
            throw BusinessException.forbidden("Chỉ người đăng bán mới có thể hủy bài.");
        }
        var remaining = openSales(sales);
        if (remaining.isEmpty()) {
            throw BusinessException.conflict("Bài bán không còn vé để hủy.");
        }
        for (var sale : remaining) {
            var ticket = lockedListedTicket(sale, userId);
            ticket.setStatus(TicketStatus.AVAILABLE);
            sale.setStatus(ExchangeStatus.CANCELLED);
        }
        log(first.getRequester(), AuditAction.CANCEL_EXCHANGE, remaining.getFirst().getTicket(), null,
            remaining.size() + " vé · ngày " + formatMealDate(first.getMealDate())
                + " · Người bán: " + first.getRequester().getFullName());
    }

    @Override
    public List<TransactionRow> transactions() {
        return transactions.findAllByOrderByCompletedAtDesc().stream()
            .map(TicketOperationsService::transactionRow).toList();
    }

    private List<TicketExchange> lockedSales(Long id) {
        var rows = offers.findSaleGroupForUpdate(id, ExchangeType.SELL);
        if (rows.isEmpty()) throw BusinessException.notFound("Không tìm thấy bài đăng bán.");
        return rows;
    }

    private MealTicket lockedListedTicket(TicketExchange sale, Long sellerId) {
        return tickets.findForUpdate(sale.getTicket().getId())
            .filter(ticket -> ticket.getOwner().getId().equals(sellerId)
                && ticket.getStatus() == TicketStatus.LISTED_FOR_EXCHANGE)
            .orElseThrow(() -> BusinessException.conflict("Vé không còn ở trạng thái đăng bán."));
    }

    private List<ExchangeRow> groupSales(List<TicketExchange> rows) {
        var groups = new LinkedHashMap<String, List<TicketExchange>>();
        for (var row : rows) groups.computeIfAbsent(saleCode(row), ignored -> new ArrayList<>()).add(row);
        return groups.values().stream().map(this::saleRow)
            .sorted(Comparator.comparing(ExchangeRow::id).reversed()).toList();
    }

    private ExchangeRow saleRow(List<TicketExchange> sales) {
        var first = sales.getFirst();
        int completed = sales.stream().mapToInt(TicketExchange::getQuantityCompleted).sum();
        var open = openSales(sales);
        boolean expired = !open.isEmpty() && first.getMealDate().isBefore(LocalDate.now(clock));
        var remainingPrices = expired ? List.<BigDecimal>of() : open.stream().map(sale -> sale.getTicket().getPrice()).toList();
        var status = expired ? ExchangeStatus.EXPIRED
            : !open.isEmpty() ? completed > 0 ? ExchangeStatus.PARTIALLY_FILLED : ExchangeStatus.OPEN
            : completed == sales.size() ? ExchangeStatus.COMPLETED : ExchangeStatus.CANCELLED;
        var amount = remainingPrices.isEmpty() ? first.getTicket().getPrice() : remainingPrices.getFirst();
        return new ExchangeRow(first.getId(), saleCode(first), first.getRequester().getId(),
            first.getRequester().getFullName(), ExchangeType.SELL, first.getMealDate(), first.getTicketType(),
            sales.size(), completed, remainingPrices.size(), status, first.getTicket().getId(),
            amount, first.getCreatedAt(), remainingPrices);
    }

    private static String saleCode(TicketExchange sale) {
        return sale.getSaleGroup() == null ? sale.getExchangeCode() : sale.getSaleGroup();
    }

    private static List<TicketExchange> openSales(List<TicketExchange> sales) {
        return sales.stream().filter(sale -> sale.getStatus() == ExchangeStatus.OPEN
            || sale.getStatus() == ExchangeStatus.PARTIALLY_FILLED).toList();
    }

    private void validateMeal(LocalDate date, TicketType type) {
        if (date.isBefore(LocalDate.now(clock)) || date.getYear() > 2200 || date.getDayOfWeek().getValue() > 5) {
            throw BusinessException.badRequest("Chọn ngày làm việc từ hôm nay trở đi.");
        }
        if (type == TicketType.SPECIAL && date.getDayOfWeek() != DayOfWeek.FRIDAY) {
            throw BusinessException.badRequest("Suất đặc biệt chỉ có vào thứ Sáu.");
        }
    }

    private User activeUser(Long id) {
        return users.findById(id).filter(user -> Boolean.TRUE.equals(user.getActive()))
            .orElseThrow(() -> BusinessException.forbidden("Tài khoản đã ngừng hoạt động."));
    }

    private MealPrice applicablePrice(LocalDate date, TicketType type) {
        return prices.findApplicablePrice(type, date)
            .orElseThrow(() -> BusinessException.badRequest("Chưa có giá áp dụng cho ngày và loại suất đã chọn."));
    }

    private static String formatMealDate(LocalDate date) {
        return date.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private void log(User actor, AuditAction action, MealTicket ticket, TicketExchangeTransaction transaction, String description) {
        var event = new AuditLog();
        event.setActor(actor);
        event.setAction(action);
        event.setTicket(ticket);
        event.setTransaction(transaction);
        event.setDescription(description);
        event.setCreatedAt(LocalDateTime.now(clock));
        audit.save(event);
    }

    private static PurchaseRow purchaseRow(TicketPurchase request) {
        return new PurchaseRow(request.getId(), request.getPurchaseCode(), request.getRequester().getId(),
            request.getRequester().getFullName(), request.getMealDate(), request.getTicketType(), request.getQuantity(),
            request.getTotalAmount(), request.getStatus(), request.getNote(), request.getCreatedAt(), request.getReviewedAt());
    }

    private static TransactionRow transactionRow(TicketExchangeTransaction transfer) {
        var ticket = transfer.getTicket();
        var sale = transfer.getSellRequest();
        return new TransactionRow(transfer.getId(), transfer.getTransactionCode(), transfer.getSeller().getId(),
            transfer.getSeller().getFullName(), transfer.getBuyer().getId(), transfer.getBuyer().getFullName(),
            ticket.getId(), ticket.getMealDate(), sale == null ? ticket.getTicketType() : sale.getTicketType(),
            transfer.getAmount() == null ? ticket.getPrice() : transfer.getAmount(), transfer.getCompletedAt());
    }

    private static String code(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }
}
