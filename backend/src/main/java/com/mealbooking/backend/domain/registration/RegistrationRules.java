package com.mealbooking.backend.domain.registration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public final class RegistrationRules {
    private RegistrationRules() {}

    public static String lockReason(LocalDate date, LocalDateTime now, LocalTime deadline, boolean excludeWeekends) {
        LocalDate today=now.toLocalDate();
        if (excludeWeekends && date.getDayOfWeek().getValue()>=6) return "Không phục vụ suất ăn vào thứ Bảy và Chủ nhật.";
        if (!date.isAfter(today)) return "Không thể chỉnh sửa ngày hôm nay hoặc ngày đã qua.";
        if (date.equals(today.plusDays(1)) && !now.toLocalTime().isBefore(deadline))
            return "Đã đến hạn " + deadline + ", ngày mai bị khóa.";
        return null;
    }

    public static String adminCancellationLock(LocalDate date, LocalDate today) {
        return date.isAfter(today) ? null : "Admin chỉ có thể hủy vé từ ngày mai.";
    }
}
