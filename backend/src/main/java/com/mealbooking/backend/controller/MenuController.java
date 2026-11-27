package com.mealbooking.backend.controller;

import com.mealbooking.backend.application.model.AdminResponses.MenuRow;
import com.mealbooking.backend.application.port.in.WeeklyMenus;
import java.time.LocalDate;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/menus")
public class MenuController {
    private final WeeklyMenus menus;
    public MenuController(WeeklyMenus menus){this.menus=menus;}
    @GetMapping public MenuRow get(@RequestParam(required=false) LocalDate weekStart){return menus.get(weekStart);}
    @GetMapping("/{weekStart}/image") public ResponseEntity<byte[]> image(@PathVariable LocalDate weekStart){
        var image=menus.image(weekStart);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(image.type()))
            .header("X-Content-Type-Options","nosniff").cacheControl(CacheControl.noStore()).body(image.bytes());
    }
}
