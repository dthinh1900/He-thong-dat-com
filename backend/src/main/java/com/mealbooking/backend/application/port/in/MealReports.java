package com.mealbooking.backend.application.port.in;

import com.mealbooking.backend.application.model.AdminResponses.Report;
import com.mealbooking.backend.application.model.AdminReportDetail;
import java.time.LocalDate;

public interface MealReports {
    Report report(int month, int year);

    Report range(LocalDate from, LocalDate to);

    AdminReportDetail detail(LocalDate from, LocalDate to);
}
