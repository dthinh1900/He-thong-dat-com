package com.mealbooking.backend.application.usecase;

import com.mealbooking.backend.adapter.export.ReportWorkbook;
import com.mealbooking.backend.application.model.AdminResponses.*;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.List;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReportWorkbookTest {
    @Test void exportsSnapshotTotalsAndUniqueDepartmentSheets() throws Exception {
        var employee=new EmployeeTotal(1L,"NV001","An",2,new BigDecimal("55000"));
        var a=new DepartmentTotal(1L,"Tong quan",2,new BigDecimal("55000"),List.of(employee));
        var b=new DepartmentTotal(2L,"A/B",0,BigDecimal.ZERO,List.of());
        var c=new DepartmentTotal(3L,"A?B",0,BigDecimal.ZERO,List.of());
        var report=new Report(11,2026,2,new BigDecimal("55000"),List.of(a,b,c),List.of());
        try(var workbook=new XSSFWorkbook(new ByteArrayInputStream(ReportWorkbook.write(report)))){
            assertEquals(4,workbook.getNumberOfSheets());
            assertEquals(55000,workbook.getSheetAt(1).getRow(1).getCell(3).getNumericCellValue());
            assertEquals(55000,workbook.getSheetAt(0).getRow(4).getCell(2).getNumericCellValue());
        }
    }
}
