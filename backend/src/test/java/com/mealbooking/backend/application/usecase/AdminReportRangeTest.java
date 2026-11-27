package com.mealbooking.backend.application.usecase;

import com.mealbooking.backend.adapter.export.ReportWorkbook;
import com.mealbooking.backend.application.model.AdminResponses.*;
import com.mealbooking.backend.domain.error.BusinessException;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import com.mealbooking.backend.application.port.out.MealReportReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class AdminReportRangeTest {
    @Test
    void invalidRangeDoesNotQueryDatabase() {
        var jdbc = mock(MealReportReader.class);
        var service = new AdminReportService(jdbc);
        var from = LocalDate.of(2026, 12, 31);

        assertThrows(BusinessException.class, () -> service.detail(from, from.minusDays(1)));
        assertThrows(BusinessException.class, () -> service.range(from, from.plusDays(366)));
        verifyNoInteractions(jdbc);
    }

    @Test
    void exportsDailySnapshotTotalsAcrossYearBoundary() throws Exception {
        var from = LocalDate.of(2026, 12, 31);
        var amount = new BigDecimal("65000.75");
        var employee = new EmployeeTotal(1L, "NV001", "An", 2, amount);
        var department = new DepartmentTotal(1L, "Theo ngay", 2, amount, List.of(employee));
        var report = new Report(12, 2026, 2, amount, List.of(department),
            List.of(new DailyTotal(from, 1, new BigDecimal("25000.25")),
                new DailyTotal(from.plusDays(1), 1, new BigDecimal("40000.50"))));

        try (var book = new XSSFWorkbook(new ByteArrayInputStream(ReportWorkbook.writeRange(report)))) {
            assertEquals(3, book.getNumberOfSheets());
            var byDay = book.getSheet("Theo ngay");
            assertEquals("2026-12-31", byDay.getRow(1).getCell(0).getStringCellValue());
            assertEquals("2027-01-01", byDay.getRow(2).getCell(0).getStringCellValue());
            assertEquals(40000.50, byDay.getRow(2).getCell(2).getNumericCellValue());
            assertEquals(65000.75, book.getSheet("Tong quan").getRow(2).getCell(2).getNumericCellValue());
        }
    }
}
