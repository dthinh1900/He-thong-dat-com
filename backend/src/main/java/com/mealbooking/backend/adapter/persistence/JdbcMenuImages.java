package com.mealbooking.backend.adapter.persistence;

import com.mealbooking.backend.application.port.in.WeeklyMenus.Content;
import com.mealbooking.backend.application.port.out.MenuImages;
import com.mealbooking.backend.application.model.AdminResponses.MenuRow;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcMenuImages implements MenuImages {
    private final JdbcTemplate jdbc;

    public JdbcMenuImages(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public Optional<MenuRow> find(LocalDate start) {
        return jdbc.query("""
            select id,week_start_date,week_end_date,image_url,uploaded_at from menu_images where week_start_date=?
            """,(r,n)->new MenuRow(r.getLong(1),r.getObject(2,LocalDate.class),r.getObject(3,LocalDate.class),
                r.getString(4),r.getObject(5,LocalDateTime.class)),start).stream()
            .findFirst();
    }

    @Override
    public void save(LocalDate start, String path, Long actorId, LocalDateTime uploadedAt, String type, byte[] bytes) {
        Long id=jdbc.queryForObject("""
            insert into menu_images(week_start_date,week_end_date,image_url,uploaded_by,uploaded_at)
            values (?,?,?,?,?) on conflict(week_start_date) do update set
            week_end_date=excluded.week_end_date,image_url=excluded.image_url,
            uploaded_by=excluded.uploaded_by,uploaded_at=excluded.uploaded_at returning id
            """,Long.class,start,start.plusDays(6),path,actorId,uploadedAt);
        jdbc.update("""
            insert into menu_image_content(menu_id,content_type,content) values (?,?,?)
            on conflict(menu_id) do update set content_type=excluded.content_type,content=excluded.content
            """,id,type,bytes);
    }

    @Override
    public Optional<Content> findImage(LocalDate start) {
        return jdbc.query("""
            select c.content_type,c.content from menu_image_content c join menu_images m on m.id=c.menu_id
            where m.week_start_date=?
            """,(r,n)->new Content(r.getString(1),r.getBytes(2)),start).stream()
            .findFirst();
    }
}
