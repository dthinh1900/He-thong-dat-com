package com.mealbooking.backend.application.usecase;

import com.mealbooking.backend.application.port.out.MealReportReader;
import com.mealbooking.backend.application.port.in.MealReports;
import com.mealbooking.backend.application.model.AdminResponses.Report;
import com.mealbooking.backend.application.model.AdminReportDetail;
import com.mealbooking.backend.domain.error.BusinessException;
import java.time.*;
import java.time.temporal.ChronoUnit;

public class AdminReportService implements MealReports {
    private final MealReportReader reader;
    public AdminReportService(MealReportReader reader) { this.reader=reader; }

    public Report report(int month,int year) {
        if(month<1||month>12||year<2000||year>2200) throw BusinessException.badRequest("Tháng/năm không hợp lệ.");
        LocalDate start = LocalDate.of(year, month, 1);
        return reader.summarize(start, start.plusMonths(1));
    }

    public Report range(LocalDate from, LocalDate to) {
        validateRange(from, to);
        return reader.summarize(from, to.plusDays(1));
    }

    public AdminReportDetail detail(LocalDate from, LocalDate to) {
        validateRange(from, to);
        return reader.detail(from, to);
    }

    private static void validateRange(LocalDate from, LocalDate to) {
        if (to.isBefore(from) || from.getYear() < 2000 || to.getYear() > 2200
                || ChronoUnit.DAYS.between(from, to) >= 366) {
            throw BusinessException.badRequest("Khoảng ngày không hợp lệ hoặc vượt 366 ngày.");
        }
    }


}
