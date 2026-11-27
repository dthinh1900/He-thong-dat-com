package com.mealbooking.backend.application.usecase;

import com.mealbooking.backend.application.port.in.WeeklyMenus;
import com.mealbooking.backend.application.port.out.MenuImageInspector;
import com.mealbooking.backend.application.port.out.MenuImages;
import com.mealbooking.backend.domain.error.BusinessException;
import com.mealbooking.backend.application.model.AdminResponses.MenuRow;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;

public class AdminMenuService implements WeeklyMenus {
    private final MenuImages images;
    private final MenuImageInspector inspector;
    private final Clock clock;

    public AdminMenuService(MenuImages images, MenuImageInspector inspector, Clock clock) {
        this.images = images;
        this.inspector = inspector;
        this.clock = clock;
    }

    private LocalDate week(LocalDate date) {
        return (date == null ? LocalDate.now(clock) : date).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    @Override
    public MenuRow get(LocalDate date) {
        return images.find(week(date)).orElseThrow(() -> BusinessException.notFound("Chưa có thực đơn tuần này."));
    }

    @Override
    public MenuRow upload(LocalDate date, byte[] bytes, Long actorId) {
        if (bytes.length == 0 || bytes.length > MAX_IMAGE_BYTES) {
            throw BusinessException.badRequest("Ảnh phải từ 1 byte đến 5 MB.");
        }
        String type = inspector.inspect(bytes);
        LocalDate start = week(date);
        images.save(start, "/api/menus/" + start + "/image", actorId, LocalDateTime.now(clock), type, bytes);
        return get(start);
    }

    @Override
    public Content image(LocalDate date) {
        return images.findImage(week(date)).orElseThrow(() -> BusinessException.notFound("Không tìm thấy ảnh thực đơn."));
    }
}
