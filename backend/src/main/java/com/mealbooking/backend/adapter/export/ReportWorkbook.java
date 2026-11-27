package com.mealbooking.backend.adapter.export;

import com.mealbooking.backend.application.model.AdminResponses.Report;
import java.io.*;
import java.util.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public final class ReportWorkbook {
    private ReportWorkbook() {}
    public static byte[] write(Report report) throws IOException {
        return write(report, false);
    }
    public static byte[] writeRange(Report report) throws IOException {
        return write(report, true);
    }
    private static byte[] write(Report report, boolean includeDays) throws IOException {
        try(var workbook=new XSSFWorkbook();var out=new ByteArrayOutputStream()) {
            Font bold=workbook.createFont();bold.setBold(true);
            CellStyle heading=workbook.createCellStyle();heading.setFont(bold);
            CellStyle money=workbook.createCellStyle();money.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));
            Set<String> names=new HashSet<>();
            Sheet summary=workbook.createSheet("Tong quan");names.add("tong quan");
            if (includeDays) names.add("theo ngay");
            header(summary,heading,"Phòng ban","Số vé","Tổng tiền (VNĐ)");
            int index=1;
            for(var department:report.departments()) {
                Row sr=summary.createRow(index++);sr.createCell(0).setCellValue(department.name());
                sr.createCell(1).setCellValue(department.tickets());amount(sr,2,department.totalAmount(),money);
                String base=WorkbookUtil.createSafeSheetName(department.name());
                String name=base;int suffix=1;
                while(!names.add(name.toLowerCase(Locale.ROOT))) {
                    String tail="-"+suffix++;name=base.substring(0,Math.min(base.length(),31-tail.length()))+tail;
                }
                Sheet sheet=workbook.createSheet(name);
                header(sheet,heading,"Mã nhân viên","Họ tên","Số vé","Tổng tiền (VNĐ)");
                int j=1;
                for(var employee:department.employees()) {
                    Row row=sheet.createRow(j++);row.createCell(0).setCellValue(employee.code());
                    row.createCell(1).setCellValue(employee.name());row.createCell(2).setCellValue(employee.tickets());
                    amount(row,3,employee.totalAmount(),money);
                }
                Row total=sheet.createRow(j);total.createCell(0).setCellValue("TỔNG CỘNG");
                total.createCell(2).setCellValue(department.tickets());amount(total,3,department.totalAmount(),money);
                widths(sheet,4);sheet.createFreezePane(0,1);
            }
            Row total=summary.createRow(index);total.createCell(0).setCellValue("TỔNG CỘNG");
            total.createCell(1).setCellValue(report.totalTickets());amount(total,2,report.totalAmount(),money);
            widths(summary,3);summary.createFreezePane(0,1);
            if (includeDays) {
                Sheet daily = workbook.createSheet("Theo ngay");
                header(daily, heading, "Ngày ăn", "Số vé", "Tổng tiền (VNĐ)");
                int dayIndex = 1;
                for (var day : report.days()) {
                    Row row = daily.createRow(dayIndex++);
                    row.createCell(0).setCellValue(day.date().toString());
                    row.createCell(1).setCellValue(day.tickets());
                    amount(row, 2, day.totalAmount(), money);
                }
                widths(daily, 3);
                daily.createFreezePane(0, 1);
            }
            workbook.write(out);return out.toByteArray();
        }
    }
    private static void header(Sheet sheet,CellStyle style,String... values) {
        Row row=sheet.createRow(0);for(int i=0;i<values.length;i++){Cell c=row.createCell(i);c.setCellValue(values[i]);c.setCellStyle(style);}
    }
    private static void amount(Row row,int col,java.math.BigDecimal amount,CellStyle style){
        Cell c=row.createCell(col);c.setCellValue(amount.doubleValue());c.setCellStyle(style);
    }
    private static void widths(Sheet sheet,int columns){for(int i=0;i<columns;i++)sheet.setColumnWidth(i,(i==0||i==1?30:22)*256);}
}
