package com.mealbooking.backend.application.port.out;

import com.mealbooking.backend.application.port.in.WeeklyMenus.Content;
import com.mealbooking.backend.application.model.AdminResponses.MenuRow;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

public interface MenuImages {
    Optional<MenuRow> find(LocalDate weekStart);

    Optional<Content> findImage(LocalDate weekStart);

    void save(LocalDate weekStart, String path, Long actorId, LocalDateTime uploadedAt, String contentType, byte[] bytes);
}
