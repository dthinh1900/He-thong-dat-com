package com.mealbooking.backend.controller;

import com.mealbooking.backend.application.model.AdminReportDetail;
import com.mealbooking.backend.application.port.in.MealReports;
import com.mealbooking.backend.adapter.export.ReportWorkbook;
import java.io.IOException;
import java.time.LocalDate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/reports")
@PreAuthorize("hasRole('ADMIN')")
public class AdminReportingController {
    private final MealReports reports;

    public AdminReportingController(MealReports reports) {
        this.reports = reports;
    }

    @GetMapping("/detail")
    public AdminReportDetail detail(@RequestParam LocalDate from, @RequestParam LocalDate to) {
        return reports.detail(from, to);
    }

    @GetMapping("/range/export")
    public ResponseEntity<byte[]> export(@RequestParam LocalDate from, @RequestParam LocalDate to)
            throws IOException {
        byte[] workbook = ReportWorkbook.writeRange(reports.range(from, to));
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=meal-report-" + from + "-" + to + ".xlsx")
            .body(workbook);
    }
}
