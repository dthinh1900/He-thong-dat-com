package com.mealbooking.backend.application.port.out;

import com.mealbooking.backend.application.model.AdminResponses.Report;
import com.mealbooking.backend.application.model.AdminReportDetail;
import java.time.LocalDate;

public interface MealReportReader {
    Report summarize(LocalDate start, LocalDate endExclusive);

    AdminReportDetail detail(LocalDate from, LocalDate to);
}
