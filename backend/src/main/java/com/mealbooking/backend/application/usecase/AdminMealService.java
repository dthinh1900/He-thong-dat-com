package com.mealbooking.backend.application.usecase;

import com.mealbooking.backend.application.port.out.AdminMealData;
import com.mealbooking.backend.application.port.out.UserStore;
import com.mealbooking.backend.application.port.out.MealPriceStore;
import com.mealbooking.backend.application.port.in.MealRegistrations;
import com.mealbooking.backend.application.port.in.AdminMeals;
import com.mealbooking.backend.application.model.AdminCommands.PriceInput;
import com.mealbooking.backend.application.model.AdminResponses.*;
import com.mealbooking.backend.domain.model.*;
import com.mealbooking.backend.domain.enums.*;
import com.mealbooking.backend.domain.error.BusinessException;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

public class AdminMealService implements AdminMeals {
    private final AdminMealData data;
    private final Clock clock;
    private final RegistrationPolicy policy;
    private final MealRegistrations registrationService;
    private final MealPriceStore prices;
    private final UserStore users;
    public AdminMealService(AdminMealData data, Clock clock, RegistrationPolicy policy,
            MealRegistrations registrationService, MealPriceStore prices, UserStore users) {
        this.data=data; this.clock=clock; this.policy=policy; this.registrationService=registrationService;
        this.prices=prices; this.users=users;
    }

    @Override
    public Overview overview() {
        LocalDate today=LocalDate.now(clock),tomorrow=today.plusDays(1);
        return data.overview(today, tomorrow, today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)));
    }

    @Override
    public KitchenCounts kitchenCounts(LocalDate date) {
        var report = data.lastKitchenReport(date);
        return new KitchenCounts(date, data.portionCounts(date), report);
    }

    @Override
    public KitchenCounts recordKitchenCounts(LocalDate date, Long actorId) {
        if (date.isBefore(LocalDate.now(clock))) {
            throw BusinessException.badRequest("Chọn ngày hôm nay hoặc một ngày sắp tới.");
        }
        var counts = data.portionCounts(date);
        var reportedAt = LocalDateTime.now(clock);
        data.saveKitchenReport(date, counts, actorId, reportedAt);
        return new KitchenCounts(date, counts, new KitchenReport(counts, reportedAt));
    }

    @Override
    public List<RegistrationRow> registrations(LocalDate date,Long departmentId) {
        Settings settings=policy.settings();
        boolean hasPrice=prices.findApplicablePrice(TicketType.NORMAL,date).isPresent();
        return data.registrationStates(date, departmentId).stream().map(row -> {
                Long id=row.ticketId(); String status=row.status();
                boolean transferred=id!=null && row.ownerId()!=row.userId();
                boolean registered=!transferred && status!=null && Set.of("AVAILABLE","LISTED_FOR_EXCHANGE","USED").contains(status);
                String lock=transferred ? "Vé đăng ký đã chuyển nhượng." :
                    registered ? policy.adminCancellationLock(date) : policy.lockReason(date,false,settings);
                if(lock==null && status!=null && !Set.of("AVAILABLE","CANCELLED","WITHDRAWN").contains(status)) lock="Trạng thái vé không cho phép chỉnh sửa.";
                if(lock==null && !registered && !row.active()) lock="Tài khoản đã khóa.";
                if(lock==null && id==null && !hasPrice) lock="Chưa cấu hình giá vé.";
                boolean withdrawable = registered && "AVAILABLE".equals(status) && lock == null
                    && policy.lockReason(date, true, settings) == null;
                return new RegistrationRow(row.userId(),row.employeeCode(),row.fullName(),
                    row.departmentId(),row.active(),registered,id,status,lock==null,lock,withdrawable);
            }).toList();
    }
    @Override
    public void setRegistration(Long userId,LocalDate date,boolean registered,Long actorId,String cancellationReason,Boolean withdrawal) {
        registrationService.setDayByAdmin(userId,date,registered,actorId,cancellationReason,withdrawal);
    }

    @Override
    public List<PriceRow> prices() {
        return prices.findAllByOrderByTicketTypeAscEffectiveFromDesc()
            .stream().map(AdminMealService::priceRow).toList();
    }
    @Override
    public PriceRow addPrice(PriceInput input,Long actorId) {
        LocalDate start = input.effectiveFrom();
        if (start.isBefore(LocalDate.now(clock))) {
            throw BusinessException.badRequest("Chọn ngày áp dụng từ hôm nay trở đi.");
        }
        data.lockPricesForWrite();
        List<MealPrice> schedule=prices.findByTicketTypeOrderByEffectiveFromDesc(input.ticketType());
        LocalDate nextStart = null;
        LocalDate end = null;
        for (MealPrice entry : schedule) {
            if (entry.getEffectiveFrom().isAfter(start)) {
                nextStart = entry.getEffectiveFrom();
                continue;
            }
            if (entry.getEffectiveFrom().equals(start)) {
                entry.setPrice(input.price());
                return priceRow(prices.saveAndFlush(entry));
            }
            if (entry.getEffectiveTo() == null || !entry.getEffectiveTo().isBefore(start)) {
                end = entry.getEffectiveTo();
                entry.setEffectiveTo(start.minusDays(1));
                prices.saveAndFlush(entry);
            }
            break;
        }
        MealPrice p=new MealPrice(); p.setTicketType(input.ticketType()); p.setPrice(input.price());
        p.setEffectiveFrom(start);
        p.setEffectiveTo(end != null ? end : nextStart == null ? null : nextStart.minusDays(1));
        p.setCreatedBy(users.getReferenceById(actorId));
        return priceRow(prices.saveAndFlush(p));
    }
    private static PriceRow priceRow(MealPrice p) {
        return new PriceRow(p.getId(),p.getTicketType(),p.getPrice(),p.getEffectiveFrom(),p.getEffectiveTo());
    }
}
