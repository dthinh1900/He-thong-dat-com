package com.mealbooking.backend.application.usecase;

import com.mealbooking.backend.application.port.out.UserStore;
import com.mealbooking.backend.application.port.out.MealTicketStore;
import com.mealbooking.backend.application.port.out.MealRegistrationStore;
import com.mealbooking.backend.application.port.out.MealPriceStore;
import com.mealbooking.backend.application.port.out.AuditLogStore;
import com.mealbooking.backend.application.port.in.MealRegistrations;
import com.mealbooking.backend.application.model.SaveMonthRegistration;
import com.mealbooking.backend.application.model.MealDayResponse;
import com.mealbooking.backend.application.model.CancelledMealTicket;
import com.mealbooking.backend.application.model.MonthRegistrationResponse;
import com.mealbooking.backend.domain.model.AuditLog;
import com.mealbooking.backend.domain.model.MealPrice;
import com.mealbooking.backend.domain.model.MealRegistration;
import com.mealbooking.backend.domain.model.MealTicket;
import com.mealbooking.backend.domain.model.User;
import com.mealbooking.backend.domain.enums.AuditAction;
import com.mealbooking.backend.domain.enums.CancellationSource;
import com.mealbooking.backend.domain.enums.TicketStatus;
import com.mealbooking.backend.domain.enums.TicketType;
import com.mealbooking.backend.domain.error.BusinessException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class MealRegistrationService implements MealRegistrations {
    private static final TicketType DEFAULT_TICKET_TYPE = TicketType.NORMAL;

    private final MealRegistrationStore registrationRepository;
    private final MealTicketStore ticketRepository;
    private final MealPriceStore priceRepository;
    private final AuditLogStore auditLogRepository;
    private final UserStore userRepository;
    private final Clock clock;
    private final RegistrationPolicy policy;

    public MealRegistrationService(
            MealRegistrationStore registrationRepository,
            MealTicketStore ticketRepository,
            MealPriceStore priceRepository,
            AuditLogStore auditLogRepository,
            UserStore userRepository,
            Clock clock,
            RegistrationPolicy policy) {
        this.registrationRepository = registrationRepository;
        this.ticketRepository = ticketRepository;
        this.priceRepository = priceRepository;
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
        this.clock = clock;
        this.policy = policy;
    }

    @Override
    public MonthRegistrationResponse getMonth(Long userId, int month, int year) {
        validateMonth(month, year);
        MealRegistration registration = registrationRepository.findByUserIdAndMonthAndYear(userId, month, year)
                .orElse(null);
        Map<LocalDate, MealTicket> tickets = new HashMap<>();
        if (registration != null) {
            for (MealTicket ticket : ticketRepository.findByRegistrationId(registration.getId())) {
                tickets.put(ticket.getMealDate(), ticket);
            }
        }

        LocalDate firstDate = LocalDate.of(year, month, 1);
        int daysInMonth = firstDate.lengthOfMonth();
        LocalDate lastDate = firstDate.withDayOfMonth(daysInMonth);
        List<MealPrice> monthPrices = priceRepository.findOverlappingSchedule(firstDate, lastDate);
        Map<LocalDate, List<MealTicket>> owned = new HashMap<>();
        Map<LocalDate, List<CancelledMealTicket>> cancelled = new HashMap<>();
        for (MealTicket ticket : ticketRepository.findByOwnerIdAndMealDateBetweenOrderByMealDate(
                userId, firstDate, lastDate)) {
            if (ticket.getStatus().isBillable()) {
                owned.computeIfAbsent(ticket.getMealDate(), ignored -> new ArrayList<>()).add(ticket);
            } else if (ticket.getStatus() == TicketStatus.CANCELLED) {
                var details = cancellation(ticket);
                if (details.source() == CancellationSource.ADMIN) {
                    cancelled.computeIfAbsent(ticket.getMealDate(), ignored -> new ArrayList<>()).add(details);
                }
            }
        }
        List<MealDayResponse> days = new ArrayList<>(daysInMonth);
        BigDecimal total = BigDecimal.ZERO;
        int selectedCount = 0;
        var settings = policy.settings();
        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = firstDate.withDayOfMonth(day);
            MealTicket ticket = tickets.get(date);
            if (ticket != null && ticket.getStatus() == TicketStatus.WITHDRAWN) ticket = null;
            boolean transferred = ticket != null && !userId.equals(ticket.getOwner().getId());
            boolean selected = !transferred && ticket != null && ticket.getStatus().isBillable();
            BigDecimal normalPrice = priceForDate(monthPrices, TicketType.NORMAL, date);
            BigDecimal specialPrice = priceForDate(monthPrices, TicketType.SPECIAL, date);
            TicketType ticketType = ticket == null ? DEFAULT_TICKET_TYPE : ticket.getTicketType();
            BigDecimal price = ticket != null ? ticket.getPrice() : normalPrice;
            String lockReason = transferred ? "Vé đăng ký đã chuyển nhượng."
                : getLockReason(date, !selected && normalPrice == null, selected, settings);
            if (lockReason == null && ticket != null && ticket.getStatus() != TicketStatus.AVAILABLE
                    && ticket.getStatus() != TicketStatus.CANCELLED) {
                lockReason = "Trạng thái vé không cho phép chỉnh sửa.";
            }
            if (lockReason != null && selected && ticket.getStatus() == TicketStatus.AVAILABLE) {
                lockReason += " Liên hệ quản trị viên để hủy vé.";
            }
            boolean editable = lockReason == null;
            boolean typeEditable = !transferred
                    && (ticket == null || ticket.getStatus() == TicketStatus.AVAILABLE
                        || ticket.getStatus() == TicketStatus.CANCELLED)
                    && policy.lockReason(date, false, settings) == null;
            var dayTickets = owned.getOrDefault(date, List.of());
            BigDecimal ownedAmount = dayTickets.stream().map(MealTicket::getPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
            total = total.add(ownedAmount);
            selectedCount += dayTickets.size();
            days.add(new MealDayResponse(
                    date, selected, ticket == null ? null : ticket.getId(),
                    ticket == null ? null : ticket.getTicketCode(),
                    ticket == null ? null : ticket.getStatus().name(), ticketType,
                    price, normalPrice, specialPrice, editable, typeEditable, lockReason, dayTickets.size(), ownedAmount,
                    cancelled.getOrDefault(date, List.of()).stream()
                        .sorted(Comparator.comparing(CancelledMealTicket::ticketId)).toList()));
        }
        return new MonthRegistrationResponse(month, year, selectedCount, total, settings.cutoff(), RegistrationPolicy.withdrawalDeadline(settings), days);
    }

    private static CancelledMealTicket cancellation(MealTicket ticket) {
        var source = ticket.getCancellationSource();
        if (source == null) {
            var actor = ticket.getCancelledBy();
            if (actor == null) source = CancellationSource.UNKNOWN;
            else if (actor.getId().equals(ticket.getOwner().getId())) source = CancellationSource.SELF;
            else source = CancellationSource.ADMIN;
        }
        return new CancelledMealTicket(ticket.getId(), ticket.getTicketCode(), ticket.getTicketType(), ticket.getPrice(),
            ticket.getCancelledAt(), source, ticket.getCancellationReason());
    }

    private static BigDecimal priceForDate(List<MealPrice> schedule, TicketType type, LocalDate date) {
        for (MealPrice price : schedule) {
            if (price.getTicketType() == type && !price.getEffectiveFrom().isAfter(date)
                    && (price.getEffectiveTo() == null || !price.getEffectiveTo().isBefore(date))) {
                return price.getPrice();
            }
        }
        return null;
    }

    @Override
    public MonthRegistrationResponse saveMonth(Long userId, SaveMonthRegistration request) {
        return saveMonth(userId, request, userId);
    }

    private MonthRegistrationResponse saveMonth(Long userId, SaveMonthRegistration request, Long actorId) {
        validateMonth(request.month(), request.year());
        User user = userRepository.findForUpdate(userId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy tài khoản."));
        User actor = userId.equals(actorId) ? user : userRepository.getReferenceById(actorId);
        var settings = policy.settings();
        LocalDate firstDate = LocalDate.of(request.year(), request.month(), 1);
        LocalDate lastDate = firstDate.withDayOfMonth(firstDate.lengthOfMonth());

        Set<LocalDate> selectedDates = new HashSet<>(request.selectedDates());
        if (selectedDates.size() != request.selectedDates().size()) {
            throw BusinessException.badRequest("Danh sách ngày đăng ký có ngày bị trùng.");
        }
        if (selectedDates.stream().anyMatch(date -> date == null || date.isBefore(firstDate) || date.isAfter(lastDate))) {
            throw BusinessException.badRequest("Có ngày đăng ký không thuộc tháng đã chọn.");
        }
        Map<LocalDate, TicketType> requestedTypes = request.selectedTypes() == null
                ? Map.of() : request.selectedTypes();
        if (requestedTypes.keySet().stream().anyMatch(date -> date == null || date.isBefore(firstDate) || date.isAfter(lastDate))) {
            throw BusinessException.badRequest("Có loại suất ăn không thuộc tháng đã chọn.");
        }

        MealRegistration registration = registrationRepository
                .findByUserIdAndMonthAndYear(userId, request.month(), request.year()).orElse(null);
        List<MealTicket> currentTickets = registration == null ? new ArrayList<>()
                : ticketRepository.findByRegistrationForUpdate(registration.getId());
        Map<LocalDate, MealTicket> byDate = new HashMap<>();
        currentTickets.forEach(ticket -> byDate.put(ticket.getMealDate(), ticket));

        // Validate the entire request before writing anything.
        Map<LocalDate, TicketType> targetTypes = new HashMap<>();
        for (LocalDate date : selectedDates) {
            MealTicket existing = byDate.get(date);
            if (existing != null && !userId.equals(existing.getOwner().getId())) {
                throw BusinessException.conflict("Vé đăng ký ngày " + date + " đã chuyển nhượng.");
            }
            TicketType targetType = requestedTypes.getOrDefault(date,
                    existing == null || existing.getStatus().canReregister() ? DEFAULT_TICKET_TYPE : existing.getTicketType());
            if (targetType == null) targetType = DEFAULT_TICKET_TYPE;
            if (targetType == TicketType.SPECIAL && date.getDayOfWeek() != DayOfWeek.FRIDAY) {
                throw BusinessException.badRequest("Chỉ thứ Sáu mới được chọn suất đặc biệt.");
            }
            if (existing == null || existing.getStatus().canReregister()) {
                if (!Boolean.TRUE.equals(user.getActive())) throw BusinessException.conflict("Tài khoản đã khóa.");
                String lock = getLockReason(date, false, false, settings);
                if (lock != null) throw BusinessException.badRequest("Không thể đăng ký ngày " + date + ": " + lock);
                if (priceRepository.findApplicablePrice(targetType, date).isEmpty()) {
                    throw BusinessException.badRequest("Chưa cấu hình giá vé " + targetType + " cho ngày " + date + ".");
                }
            } else if (existing.getStatus() != TicketStatus.AVAILABLE && targetType != existing.getTicketType()) {
                throw BusinessException.conflict("Vé ngày " + date + " đang ở trạng thái không thể chỉnh sửa.");
            } else if (targetType != existing.getTicketType()) {
                String lock = getLockReason(date, false, false, settings);
                if (lock != null) throw BusinessException.badRequest("Không thể đổi suất ngày " + date + ": " + lock);
            }
            targetTypes.put(date, targetType);
        }

        for (MealTicket existing : currentTickets) {
            boolean isKept = selectedDates.contains(existing.getMealDate());
            if (existing.getStatus() == TicketStatus.AVAILABLE && !isKept && userId.equals(existing.getOwner().getId())) {
                String lock = getLockReason(existing.getMealDate(), false, true, settings);
                if (lock != null) throw BusinessException.badRequest(
                    "Không thể bỏ chọn ngày " + existing.getMealDate() + ". " + lock + " Liên hệ quản trị viên để hủy vé.");
            }
        }

        if (registration == null && !selectedDates.isEmpty()) {
            registration = new MealRegistration();
            registration.setRegistrationCode("DK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            registration.setUser(user);
            registration.setMonth(request.month());
            registration.setYear(request.year());
            registration = registrationRepository.save(registration);
        }

        if (registration != null) {
            for (LocalDate date : selectedDates) {
                MealTicket existing = byDate.get(date);
                TicketType targetType = targetTypes.get(date);
                if (existing == null || existing.getStatus().canReregister()) {
                    MealPrice price = priceRepository.findApplicablePrice(targetType, date)
                            .orElseThrow(() -> BusinessException.badRequest("Chưa cấu hình giá cho ngày " + date + "."));
                    if (existing == null) {
                        MealTicket ticket = new MealTicket();
                        ticket.setTicketCode("VE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
                        ticket.setRegistration(registration);
                        ticket.setOwner(user);
                        ticket.setMealDate(date);
                        existing = ticket;
                    }
                    existing.setMealPrice(price);
                    existing.setTicketType(targetType);
                    existing.setPrice(price.getPrice());
                    existing.setStatus(TicketStatus.AVAILABLE);
                    existing.setCancelledAt(null);
                    existing.setCancelledBy(null);
                    existing.setCancellationSource(null);
                    existing.setCancellationReason(null);
                    existing = ticketRepository.save(existing);
                    writeAudit(actor, existing, AuditAction.REGISTER_TICKET,
                            "Đăng ký " + targetType + " ngày " + date);
                } else if (existing.getTicketType() != targetType) {
                    MealPrice price = priceRepository.findApplicablePrice(targetType, date)
                            .orElseThrow(() -> BusinessException.badRequest("Chưa cấu hình giá cho ngày " + date + "."));
                    existing.setMealPrice(price);
                    existing.setTicketType(targetType);
                    existing.setPrice(price.getPrice());
                    ticketRepository.save(existing);
                    writeAudit(actor, existing, AuditAction.REGISTER_TICKET,
                            "Đổi loại suất thành " + targetType + " ngày " + date);
                }
            }
            for (MealTicket existing : currentTickets) {
                if (existing.getStatus() == TicketStatus.AVAILABLE && !selectedDates.contains(existing.getMealDate())
                        && userId.equals(existing.getOwner().getId())) {
                    markWithdrawn(existing, actor);
                }
            }
        }
        return getMonth(userId, request.month(), request.year());
    }

    @Override
    public void cancelTicketByAdmin(Long actorId, Long ticketId, String reason, Boolean withdrawal) {
        MealTicket ticket = ticketRepository.findForUpdate(ticketId)
            .orElseThrow(() -> BusinessException.notFound("Không tìm thấy vé ăn."));
        removeTicketByAdmin(ticket, actorId, reason, withdrawal);
    }

    private void removeTicketByAdmin(MealTicket ticket, Long actorId, String reason, Boolean withdrawal) {
        if (ticket.getStatus() != TicketStatus.AVAILABLE) {
            throw BusinessException.conflict("Chỉ có thể chỉnh sửa vé đang có hiệu lực và chưa đăng bán.");
        }
        String lock = policy.adminCancellationLock(ticket.getMealDate());
        if (lock != null) throw BusinessException.badRequest(lock);
        boolean canWithdraw = policy.canWithdraw(ticket, policy.settings());
        if (withdrawal != null && withdrawal != canWithdraw) {
            throw BusinessException.conflict("Vé đã đổi điều kiện thao tác. Hãy tải lại và chọn thao tác phù hợp.");
        }
        User actor = userRepository.getReferenceById(actorId);
        if (canWithdraw) markWithdrawn(ticket, actor);
        else markCancelled(ticket, actor, reason);
    }

    private void markWithdrawn(MealTicket ticket, User actor) {
        ticket.setStatus(TicketStatus.WITHDRAWN);
        ticketRepository.save(ticket);
        writeAudit(actor, ticket, AuditAction.WITHDRAW_TICKET, "Bỏ chọn suất ăn ngày " + ticket.getMealDate());
    }

    private void markCancelled(MealTicket ticket, User actor, String reason) {
        String note = reason == null ? null : reason.strip();
        if (note != null && note.length() > 500) throw BusinessException.badRequest("Lý do hủy chỉ được tối đa 500 ký tự.");
        ticket.setStatus(TicketStatus.CANCELLED);
        ticket.setCancelledAt(java.time.LocalDateTime.now(clock));
        ticket.setCancelledBy(actor);
        ticket.setCancellationSource(CancellationSource.ADMIN);
        ticket.setCancellationReason(note == null || note.isEmpty() ? null : note);
        ticketRepository.save(ticket);
        String description = "Quản trị viên hủy vé ăn ngày " + ticket.getMealDate();
        if (ticket.getCancellationReason() != null) description += ". Lý do " + ticket.getCancellationReason();
        writeAudit(actor, ticket, AuditAction.CANCEL_TICKET, description);
    }

    @Override
    public void setDayByAdmin(Long userId, LocalDate date, boolean registered, Long actorId, String cancellationReason, Boolean withdrawal) {
        validateMonth(date.getMonthValue(), date.getYear());
        userRepository.findForUpdate(userId).orElseThrow(() -> BusinessException.notFound("Không tìm thấy nhân viên."));
        var registration = registrationRepository.findByUserIdAndMonthAndYear(userId,date.getMonthValue(),date.getYear());
        List<MealTicket> tickets = registration.map(r -> ticketRepository.findByRegistrationForUpdate(r.getId())).orElseGet(List::of);
        MealTicket ticket = tickets.stream().filter(t -> t.getMealDate().equals(date)).findFirst().orElse(null);
        if (ticket != null && !userId.equals(ticket.getOwner().getId())) {
            throw BusinessException.conflict("Vé đăng ký đã chuyển nhượng.");
        }
        if(ticket != null && ticket.getStatus()!=TicketStatus.AVAILABLE && !ticket.getStatus().canReregister())
            throw BusinessException.conflict("Trạng thái vé không cho phép chỉnh sửa.");
        if(registered == (ticket != null && ticket.getStatus()==TicketStatus.AVAILABLE)) return;
        if (!registered) {
            removeTicketByAdmin(ticket, actorId, cancellationReason, withdrawal);
            return;
        }
        Set<LocalDate> selected = new HashSet<>();
        tickets.stream().filter(t -> userId.equals(t.getOwner().getId()) && t.getStatus().isBillable())
            .forEach(t -> selected.add(t.getMealDate()));
        selected.add(date);
        saveMonth(userId,new SaveMonthRegistration(date.getMonthValue(),date.getYear(),new ArrayList<>(selected), Map.of()),actorId);
    }

    private String getLockReason(LocalDate date, boolean noPrice, boolean cancellation,
            com.mealbooking.backend.application.model.AdminResponses.Settings settings) {
        String lock = policy.lockReason(date,cancellation,settings);
        return lock != null ? lock : noPrice ? "Chưa có bảng giá vé thường áp dụng cho ngày này." : null;
    }

    private void writeAudit(User actor, MealTicket ticket, AuditAction action, String description) {
        AuditLog log = new AuditLog();
        log.setActor(actor);
        log.setTicket(ticket);
        log.setAction(action);
        log.setDescription(description);
        auditLogRepository.save(log);
    }

    private void validateMonth(int month, int year) {
        if (month < 1 || month > 12 || year < 2000 || year > 2200) {
            throw BusinessException.badRequest("Tháng hoặc năm không hợp lệ.");
        }
    }
}
