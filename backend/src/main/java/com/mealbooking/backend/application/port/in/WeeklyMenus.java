package com.mealbooking.backend.application.port.in;

import com.mealbooking.backend.application.model.AdminResponses.MenuRow;
import java.time.LocalDate;

public interface WeeklyMenus {
    int MAX_IMAGE_BYTES = 5 * 1024 * 1024;

    MenuRow get(LocalDate date);

    MenuRow upload(LocalDate date, byte[] image, Long actorId);

    Content image(LocalDate date);

    record Content(String type, byte[] bytes) {}
}
