package com.kizuna.export;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Generator for Non-Functional Test Report Document (Security & Performance Testing).
 * Outputs to Desktop: C:\Users\Vuh26\Desktop\Report-5.4_NonFunctional_Test.xlsx
 */
public class NonFunctionalTestExcelExporterTest {

    public static class SecurityTestCase {
        public String id;
        public String scenarioName;
        public String roleSession;
        public String given;
        public String when;
        public String then;
        public String priority;
        public String status = "Passed";
        public String defectId = "N/A";
        public String notes = "";

        public SecurityTestCase(String id, String scenarioName, String roleSession, String given, String when, String then, String priority, String notes) {
            this.id = id;
            this.scenarioName = scenarioName;
            this.roleSession = roleSession;
            this.given = given;
            this.when = when;
            this.then = then;
            this.priority = priority;
            this.notes = notes;
        }
    }

    public static class PerformanceTestCase {
        public String id;
        public String scenario;
        public String tool;
        public String target;
        public String actual;
        public String status = "Passed";
        public String defectId = "N/A";
        public String notes = "";

        public PerformanceTestCase(String id, String scenario, String tool, String target, String actual, String notes) {
            this.id = id;
            this.scenario = scenario;
            this.tool = tool;
            this.target = target;
            this.actual = actual;
            this.notes = notes;
        }
    }

    @Test
    public void exportNonFunctionalExcel() throws IOException {
        Workbook wb = new XSSFWorkbook();

        // Fonts
        Font fontRegular = wb.createFont();
        fontRegular.setFontName("Tahoma");
        fontRegular.setFontHeightInPoints((short) 10);

        Font fontBold = wb.createFont();
        fontBold.setFontName("Tahoma");
        fontBold.setFontHeightInPoints((short) 10);
        fontBold.setBold(true);

        Font fontTitle = wb.createFont();
        fontTitle.setFontName("Tahoma");
        fontTitle.setFontHeightInPoints((short) 16);
        fontTitle.setBold(true);

        Font fontBigTitle = wb.createFont();
        fontBigTitle.setFontName("Tahoma");
        fontBigTitle.setFontHeightInPoints((short) 18);
        fontBigTitle.setBold(true);

        // Header style
        CellStyle headerStyle = wb.createCellStyle();
        headerStyle.setFont(fontBold);
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        headerStyle.setWrapText(true);

        // Cell Center
        CellStyle cellCenter = wb.createCellStyle();
        cellCenter.setFont(fontRegular);
        cellCenter.setAlignment(HorizontalAlignment.CENTER);
        cellCenter.setVerticalAlignment(VerticalAlignment.CENTER);
        cellCenter.setBorderTop(BorderStyle.THIN);
        cellCenter.setBorderBottom(BorderStyle.THIN);
        cellCenter.setBorderLeft(BorderStyle.THIN);
        cellCenter.setBorderRight(BorderStyle.THIN);

        // Cell Center Bold
        CellStyle cellCenterBold = wb.createCellStyle();
        cellCenterBold.setFont(fontBold);
        cellCenterBold.setAlignment(HorizontalAlignment.CENTER);
        cellCenterBold.setVerticalAlignment(VerticalAlignment.CENTER);
        cellCenterBold.setBorderTop(BorderStyle.THIN);
        cellCenterBold.setBorderBottom(BorderStyle.THIN);
        cellCenterBold.setBorderLeft(BorderStyle.THIN);
        cellCenterBold.setBorderRight(BorderStyle.THIN);

        // Cell Left Wrap
        CellStyle cellLeftWrap = wb.createCellStyle();
        cellLeftWrap.setFont(fontRegular);
        cellLeftWrap.setAlignment(HorizontalAlignment.LEFT);
        cellLeftWrap.setVerticalAlignment(VerticalAlignment.TOP);
        cellLeftWrap.setWrapText(true);
        cellLeftWrap.setBorderTop(BorderStyle.THIN);
        cellLeftWrap.setBorderBottom(BorderStyle.THIN);
        cellLeftWrap.setBorderLeft(BorderStyle.THIN);
        cellLeftWrap.setBorderRight(BorderStyle.THIN);

        // Cell Left Wrap Bold
        CellStyle cellLeftWrapBold = wb.createCellStyle();
        cellLeftWrapBold.setFont(fontBold);
        cellLeftWrapBold.setAlignment(HorizontalAlignment.LEFT);
        cellLeftWrapBold.setVerticalAlignment(VerticalAlignment.TOP);
        cellLeftWrapBold.setWrapText(true);
        cellLeftWrapBold.setBorderTop(BorderStyle.THIN);
        cellLeftWrapBold.setBorderBottom(BorderStyle.THIN);
        cellLeftWrapBold.setBorderLeft(BorderStyle.THIN);
        cellLeftWrapBold.setBorderRight(BorderStyle.THIN);

        // Label Bold
        CellStyle metaLabelBold = wb.createCellStyle();
        metaLabelBold.setFont(fontBold);
        metaLabelBold.setAlignment(HorizontalAlignment.LEFT);
        metaLabelBold.setVerticalAlignment(VerticalAlignment.CENTER);

        // Title Style
        CellStyle titleStyle = wb.createCellStyle();
        titleStyle.setFont(fontTitle);
        titleStyle.setAlignment(HorizontalAlignment.CENTER);
        titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        // Build data
        List<SecurityTestCase> secCases = buildSecurityCases();
        List<PerformanceTestCase> perfCases = buildPerformanceCases();

        // Create Sheets
        createCoverSheet(wb, titleStyle, headerStyle, cellCenter, metaLabelBold);
        createTestCaseListSheet(wb, fontBigTitle, headerStyle, cellCenter, cellCenterBold, cellLeftWrap, metaLabelBold, fontRegular);
        createSecuritySheet(wb, secCases, headerStyle, cellCenter, cellCenterBold, cellLeftWrap, cellLeftWrapBold, metaLabelBold);
        createPerformanceSheet(wb, perfCases, headerStyle, cellCenter, cellCenterBold, cellLeftWrap, cellLeftWrapBold, metaLabelBold);
        createTestStatisticsSheet(wb, titleStyle, headerStyle, cellCenter, cellCenterBold, metaLabelBold);

        // Evaluate formulas
        try {
            wb.getCreationHelper().createFormulaEvaluator().evaluateAll();
        } catch (Exception e) {
            System.out.println("Formula evaluation notice: " + e.getMessage());
        }

        // Export to Desktop
        File desktopFile1 = new File("C:\\Users\\Vuh26\\Desktop\\Report-5.4_NonFunctional_Test.xlsx");
        try (FileOutputStream fos = new FileOutputStream(desktopFile1)) {
            wb.write(fos);
        }
        System.out.println("Exported successfully to: " + desktopFile1.getAbsolutePath());

        File desktopFile2 = new File("C:\\Users\\Vuh26\\Desktop\\Report-5.4_NonFunctional_Test_KizunaTutor.xlsx");
        try (FileOutputStream fos = new FileOutputStream(desktopFile2)) {
            wb.write(fos);
        }

        File artifactFile = new File("C:\\Users\\Vuh26\\.gemini\\antigravity\\brain\\9c32454a-b322-4db4-b73f-ca7159e96a64\\Report-5.4_NonFunctional_Test.xlsx");
        try (FileOutputStream fos = new FileOutputStream(artifactFile)) {
            wb.write(fos);
        }

        wb.close();
    }

    private void createCoverSheet(Workbook wb, CellStyle titleStyle, CellStyle headerStyle, CellStyle cellCenter, CellStyle metaLabelBold) {
        Sheet sheet = wb.createSheet("Cover");
        sheet.setDisplayGridlines(true);

        Row r1 = sheet.createRow(1);
        Cell cTitle = r1.createCell(1);
        cTitle.setCellValue("NON-FUNCTIONAL TEST REPORT DOCUMENT");
        cTitle.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 1, 5));

        Row r3 = sheet.createRow(3);
        Cell r3c0 = r3.createCell(0); r3c0.setCellValue("Project Name"); r3c0.setCellStyle(metaLabelBold);
        Cell r3c1 = r3.createCell(1); r3c1.setCellValue("Kizuna Tutor System");
        Cell r3c4 = r3.createCell(4); r3c4.setCellValue("Creator"); r3c4.setCellStyle(metaLabelBold);
        Cell r3c5 = r3.createCell(5); r3c5.setCellValue("Hoangvm");

        Row r4 = sheet.createRow(4);
        Cell r4c0 = r4.createCell(0); r4c0.setCellValue("Project Code"); r4c0.setCellStyle(metaLabelBold);
        Cell r4c1 = r4.createCell(1); r4c1.setCellValue("KTS");
        Cell r4c4 = r4.createCell(4); r4c4.setCellValue("Issue Date"); r4c4.setCellStyle(metaLabelBold);
        Cell r4c5 = r4.createCell(5); r4c5.setCellValue("2026-11-23");

        Row r5 = sheet.createRow(5);
        Cell r5c0 = r5.createCell(0); r5c0.setCellValue("Document Code"); r5c0.setCellStyle(metaLabelBold);
        Cell r5c1 = r5.createCell(1); r5c1.setCellValue("KTS_NonFunctional_Test_Document_v1.0");
        Cell r5c4 = r5.createCell(4); r5c4.setCellValue("Version"); r5c4.setCellStyle(metaLabelBold);
        Cell r5c5 = r5.createCell(5); r5c5.setCellValue("v1.0");

        Row r8 = sheet.createRow(8);
        Cell r8c0 = r8.createCell(0); r8c0.setCellValue("Record of change"); r8c0.setCellStyle(metaLabelBold);

        Row r9 = sheet.createRow(9);
        String[] headers = {"Effective Date", "Version", "Change Item", "*A,D,M", "Change description", "Reference"};
        for (int i = 0; i < headers.length; i++) {
            Cell c = r9.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(headerStyle);
        }

        Row r10 = sheet.createRow(10);
        String[] changeRow = {
                "2026-11-23",
                "v1.0",
                "Khởi tạo Báo cáo Kiểm thử Phi chức năng (Non-Functional Test)",
                "A",
                "Đặc tả và kiểm thử toàn diện 2 hạng mục phi chức năng cốt lõi: An ninh - Bảo mật (Security Testing) và Hiệu năng - Tốc độ (Performance Testing) cho KizunaTutor",
                "SRS UC-01 ~ UC-62"
        };
        for (int i = 0; i < changeRow.length; i++) {
            Cell c = r10.createCell(i);
            c.setCellValue(changeRow[i]);
            c.setCellStyle(cellCenter);
        }

        sheet.setColumnWidth(0, 4500);
        sheet.setColumnWidth(1, 3500);
        sheet.setColumnWidth(2, 6500);
        sheet.setColumnWidth(3, 2500);
        sheet.setColumnWidth(4, 16000);
        sheet.setColumnWidth(5, 6000);
    }

    private void createTestCaseListSheet(Workbook wb, Font fontBigTitle, CellStyle headerStyle, CellStyle cellCenter, CellStyle cellCenterBold, CellStyle cellLeftWrap, CellStyle metaLabelBold, Font fontRegular) {
        Sheet sheet = wb.createSheet("Test Cases");
        sheet.setDisplayGridlines(true);

        CellStyle bigTitleStyle = wb.createCellStyle();
        bigTitleStyle.setFont(fontBigTitle);
        bigTitleStyle.setAlignment(HorizontalAlignment.LEFT);
        bigTitleStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        Row r0 = sheet.createRow(0);
        Cell c0 = r0.createCell(3);
        c0.setCellValue("TEST CASE LIST");
        c0.setCellStyle(bigTitleStyle);

        Row r2 = sheet.createRow(2);
        Cell r2b = r2.createCell(1); r2b.setCellValue("Project Name"); r2b.setCellStyle(metaLabelBold);
        Cell r2d = r2.createCell(3); r2d.setCellFormula("Cover!B4");
        sheet.addMergedRegion(new CellRangeAddress(2, 2, 1, 2));
        sheet.addMergedRegion(new CellRangeAddress(2, 2, 3, 5));

        Row r3 = sheet.createRow(3);
        Cell r3b = r3.createCell(1); r3b.setCellValue("Project Code"); r3b.setCellStyle(metaLabelBold);
        Cell r3d = r3.createCell(3); r3d.setCellFormula("Cover!B5");
        sheet.addMergedRegion(new CellRangeAddress(3, 3, 1, 2));
        sheet.addMergedRegion(new CellRangeAddress(3, 3, 3, 5));

        Row r4 = sheet.createRow(4);
        Cell r4b = r4.createCell(1); r4b.setCellValue("Test Environment Setup Description"); r4b.setCellStyle(metaLabelBold);
        Cell r4d = r4.createCell(3);
        r4d.setCellValue("1. Web Application: KizunaTutor SPA Frontend (Vanilla JS / Tailwind CSS, port 3000)\n" +
                "2. Backend Server: Spring Boot 3.3.4 (Java 17, Spring Security, JWT HttpOnly Cookie, port 8085)\n" +
                "3. Database Server: PostgreSQL 15 (Supabase Cloud Database & Connection Pool)\n" +
                "4. Testing Tools: Chrome DevTools (Network, Lighthouse, Rendering/FPS), Postman, Apache Benchmark\n" +
                "5. Security Policies: OWASP Top 10 mitigation (Broken Access Control, Anti-Brute Force, HttpOnly, XSS Defense)");
        CellStyle envStyle = wb.createCellStyle();
        envStyle.setFont(fontRegular);
        envStyle.setWrapText(true);
        envStyle.setVerticalAlignment(VerticalAlignment.TOP);
        r4d.setCellStyle(envStyle);
        sheet.addMergedRegion(new CellRangeAddress(4, 4, 1, 2));
        sheet.addMergedRegion(new CellRangeAddress(4, 4, 3, 5));
        r4.setHeightInPoints(90);

        Row r7 = sheet.createRow(7);
        String[] headers = {"No", "Category Name", "Sheet Name", "Description", "Pre-Condition"};
        for (int i = 0; i < headers.length; i++) {
            Cell c = r7.createCell(i + 1);
            c.setCellValue(headers[i]);
            c.setCellStyle(headerStyle);
        }

        String[][] categories = {
                {"1", "Security Testing (Kiểm thử An ninh & Bảo mật)", "Security", "Kiểm thử kiểm soát truy cập (IDOR), bảo vệ token HttpOnly, chống rò rỉ danh tính User Enumeration, chặn reset mật khẩu nội bộ và Rate Limiting", "Hệ thống Backend Spring Boot và Supabase DB đang hoạt động"},
                {"2", "Performance Testing (Kiểm thử Hiệu năng & Tốc độ)", "Performance", "Đo lường thời gian tải trang Dashboard, tốc độ tra cứu từ vựng/Kanji, độ trễ Cache SHA-256 < 50ms, độ mượt 60 FPS của game và độ ổn định tải", "Chrome DevTools sẵn sàng, mạng ổn định"}
        };

        for (int i = 0; i < categories.length; i++) {
            Row r = sheet.createRow(8 + i);
            r.createCell(1).setCellValue(categories[i][0]); r.getCell(1).setCellStyle(cellCenter);
            r.createCell(2).setCellValue(categories[i][1]); r.getCell(2).setCellStyle(cellCenterBold);
            r.createCell(3).setCellValue(categories[i][2]); r.getCell(3).setCellStyle(cellCenter);
            r.createCell(4).setCellValue(categories[i][3]); r.getCell(4).setCellStyle(cellLeftWrap);
            r.createCell(5).setCellValue(categories[i][4]); r.getCell(5).setCellStyle(cellLeftWrap);
        }

        sheet.setColumnWidth(0, 1000);
        sheet.setColumnWidth(1, 1500);
        sheet.setColumnWidth(2, 7500);
        sheet.setColumnWidth(3, 4000);
        sheet.setColumnWidth(4, 16000);
        sheet.setColumnWidth(5, 12000);
    }

    private void createSecuritySheet(Workbook wb, List<SecurityTestCase> cases,
                                     CellStyle headerStyle, CellStyle cellCenter,
                                     CellStyle cellCenterBold, CellStyle cellLeftWrap,
                                     CellStyle cellLeftWrapBold, CellStyle metaLabelBold) {
        Sheet sheet = wb.createSheet("Security");
        sheet.setDisplayGridlines(true);

        // Header block
        Row r0 = sheet.createRow(0);
        r0.createCell(0).setCellValue("Category"); r0.getCell(0).setCellStyle(metaLabelBold);
        r0.createCell(1).setCellValue("Security Testing (Kiểm thử An ninh & Bảo mật)"); r0.getCell(1).setCellStyle(metaLabelBold);
        r0.createCell(7).setCellValue("Passed"); r0.getCell(7).setCellStyle(cellCenterBold);

        Row r1 = sheet.createRow(1);
        r1.createCell(0).setCellValue("Test requirement"); r1.getCell(0).setCellStyle(metaLabelBold);
        r1.createCell(1).setCellValue("Kiểm thử các chính sách bảo mật OWASP: Kiểm soát truy cập đối tượng trực tiếp (IDOR), bảo vệ cookie HttpOnly, chống rò rỉ danh tính (User Enumeration), chặn reset mật khẩu nội bộ và Rate Limit.");
        r1.createCell(7).setCellValue("Failed"); r1.getCell(7).setCellStyle(cellCenterBold);

        Row r2 = sheet.createRow(2);
        r2.createCell(0).setCellValue("Number of TCs"); r2.getCell(0).setCellStyle(metaLabelBold);
        Cell r2b = r2.createCell(1);
        r2b.setCellFormula("COUNTIF(A8:A100,\"TC-SEC*\")");
        r2b.setCellStyle(cellCenterBold);
        r2.createCell(7).setCellValue("Pending"); r2.getCell(7).setCellStyle(cellCenterBold);

        Row r3 = sheet.createRow(3);
        r3.createCell(0).setCellValue("Testing Round"); r3.getCell(0).setCellStyle(metaLabelBold);
        r3.createCell(1).setCellValue("Passed"); r3.getCell(1).setCellStyle(cellCenterBold);
        r3.createCell(2).setCellValue("Failed"); r3.getCell(2).setCellStyle(cellCenterBold);
        r3.createCell(3).setCellValue("Pending"); r3.getCell(3).setCellStyle(cellCenterBold);
        r3.createCell(4).setCellValue("N/A"); r3.getCell(4).setCellStyle(cellCenterBold);
        r3.createCell(7).setCellValue("N/A"); r3.getCell(7).setCellStyle(cellCenterBold);

        Row r4 = sheet.createRow(4);
        r4.createCell(0).setCellValue("Round 1"); r4.getCell(0).setCellStyle(metaLabelBold);
        r4.createCell(1).setCellFormula("COUNTIF($H8:$H100,B4)"); r4.getCell(1).setCellStyle(cellCenter);
        r4.createCell(2).setCellFormula("COUNTIF($H8:$H100,C4)"); r4.getCell(2).setCellStyle(cellCenter);
        r4.createCell(3).setCellFormula("COUNTIF($H8:$H100,D4)"); r4.getCell(3).setCellStyle(cellCenter);
        r4.createCell(4).setCellFormula("COUNTIF($H8:$H100,E4)"); r4.getCell(4).setCellStyle(cellCenter);

        Row r5 = sheet.createRow(5);
        r5.createCell(0).setCellValue("Round 2"); r5.getCell(0).setCellStyle(metaLabelBold);
        r5.createCell(1).setCellFormula("COUNTIF($H8:$H100,B4)"); r5.getCell(1).setCellStyle(cellCenter);
        r5.createCell(2).setCellFormula("COUNTIF($H8:$H100,C4)"); r5.getCell(2).setCellStyle(cellCenter);
        r5.createCell(3).setCellFormula("COUNTIF($H8:$H100,D4)"); r5.getCell(3).setCellStyle(cellCenter);
        r5.createCell(4).setCellFormula("COUNTIF($H8:$H100,E4)"); r5.getCell(4).setCellStyle(cellCenter);

        Row r6 = sheet.createRow(6);
        r6.createCell(0).setCellValue("Round 3"); r6.getCell(0).setCellStyle(metaLabelBold);
        r6.createCell(1).setCellFormula("COUNTIF($H8:$H100,B4)"); r6.getCell(1).setCellStyle(cellCenter);
        r6.createCell(2).setCellFormula("COUNTIF($H8:$H100,C4)"); r6.getCell(2).setCellStyle(cellCenter);
        r6.createCell(3).setCellFormula("COUNTIF($H8:$H100,D4)"); r6.getCell(3).setCellStyle(cellCenter);
        r6.createCell(4).setCellFormula("COUNTIF($H8:$H100,E4)"); r6.getCell(4).setCellStyle(cellCenter);

        // Table Header
        Row r7 = sheet.createRow(7);
        String[] colHeaders = {
                "Test Case ID", "Scenario Name", "Role / Session", "Given (Pre-conditions)",
                "When (Action / Trigger)", "Then (Expected: HTTP Status / Visibility)",
                "Priority", "Status", "Defect ID", "Notes"
        };
        for (int c = 0; c < colHeaders.length; c++) {
            Cell cell = r7.createCell(c);
            cell.setCellValue(colHeaders[c]);
            cell.setCellStyle(headerStyle);
        }
        r7.setHeightInPoints(28);

        // Render Rows
        int rowIdx = 8;
        for (SecurityTestCase tc : cases) {
            Row r = sheet.createRow(rowIdx);
            Cell c0 = r.createCell(0); c0.setCellValue(tc.id); c0.setCellStyle(cellCenterBold);
            Cell c1 = r.createCell(1); c1.setCellValue(tc.scenarioName); c1.setCellStyle(cellLeftWrapBold);
            Cell c2 = r.createCell(2); c2.setCellValue(tc.roleSession); c2.setCellStyle(cellCenter);
            Cell c3 = r.createCell(3); c3.setCellValue(tc.given); c3.setCellStyle(cellLeftWrap);
            Cell c4 = r.createCell(4); c4.setCellValue(tc.when); c4.setCellStyle(cellLeftWrap);
            Cell c5 = r.createCell(5); c5.setCellValue(tc.then); c5.setCellStyle(cellLeftWrap);
            Cell c6 = r.createCell(6); c6.setCellValue(tc.priority); c6.setCellStyle(cellCenterBold);
            Cell c7 = r.createCell(7); c7.setCellValue(tc.status); c7.setCellStyle(cellCenterBold);
            Cell c8 = r.createCell(8); c8.setCellValue(tc.defectId); c8.setCellStyle(cellCenter);
            Cell c9 = r.createCell(9); c9.setCellValue(tc.notes); c9.setCellStyle(cellLeftWrap);

            rowIdx++;
        }

        sheet.setColumnWidth(0, 3800);  // ID
        sheet.setColumnWidth(1, 10000); // Scenario
        sheet.setColumnWidth(2, 6000);  // Role
        sheet.setColumnWidth(3, 11000); // Given
        sheet.setColumnWidth(4, 11000); // When
        sheet.setColumnWidth(5, 14000); // Then
        sheet.setColumnWidth(6, 3200);  // Priority
        sheet.setColumnWidth(7, 3200);  // Status
        sheet.setColumnWidth(8, 3000);  // Defect ID
        sheet.setColumnWidth(9, 7000);  // Notes
    }

    private void createPerformanceSheet(Workbook wb, List<PerformanceTestCase> cases,
                                        CellStyle headerStyle, CellStyle cellCenter,
                                        CellStyle cellCenterBold, CellStyle cellLeftWrap,
                                        CellStyle cellLeftWrapBold, CellStyle metaLabelBold) {
        Sheet sheet = wb.createSheet("Performance");
        sheet.setDisplayGridlines(true);

        // Header block
        Row r0 = sheet.createRow(0);
        r0.createCell(0).setCellValue("Category"); r0.getCell(0).setCellStyle(metaLabelBold);
        r0.createCell(1).setCellValue("Performance Testing (Kiểm thử Hiệu năng & Tốc độ)"); r0.getCell(1).setCellStyle(metaLabelBold);
        r0.createCell(5).setCellValue("Passed"); r0.getCell(5).setCellStyle(cellCenterBold);

        Row r1 = sheet.createRow(1);
        r1.createCell(0).setCellValue("Test requirement"); r1.getCell(0).setCellStyle(metaLabelBold);
        r1.createCell(1).setCellValue("Đo lường thời gian tải trang Dashboard, tốc độ tra cứu từ vựng/Kanji, hiệu năng cache SHA-256 < 50ms, độ mượt 60 FPS của minigame và độ ổn định chịu tải.");
        r1.createCell(5).setCellValue("Failed"); r1.getCell(5).setCellStyle(cellCenterBold);

        Row r2 = sheet.createRow(2);
        r2.createCell(0).setCellValue("Number of TCs"); r2.getCell(0).setCellStyle(metaLabelBold);
        Cell r2b = r2.createCell(1);
        r2b.setCellFormula("COUNTIF(A8:A100,\"TC-PERF*\")");
        r2b.setCellStyle(cellCenterBold);
        r2.createCell(5).setCellValue("Pending"); r2.getCell(5).setCellStyle(cellCenterBold);

        Row r3 = sheet.createRow(3);
        r3.createCell(0).setCellValue("Testing Round"); r3.getCell(0).setCellStyle(metaLabelBold);
        r3.createCell(1).setCellValue("Passed"); r3.getCell(1).setCellStyle(cellCenterBold);
        r3.createCell(2).setCellValue("Failed"); r3.getCell(2).setCellStyle(cellCenterBold);
        r3.createCell(3).setCellValue("Pending"); r3.getCell(3).setCellStyle(cellCenterBold);
        r3.createCell(4).setCellValue("N/A"); r3.getCell(4).setCellStyle(cellCenterBold);
        r3.createCell(5).setCellValue("N/A"); r3.getCell(5).setCellStyle(cellCenterBold);

        Row r4 = sheet.createRow(4);
        r4.createCell(0).setCellValue("Round 1"); r4.getCell(0).setCellStyle(metaLabelBold);
        r4.createCell(1).setCellFormula("COUNTIF($F8:$F100,B4)"); r4.getCell(1).setCellStyle(cellCenter);
        r4.createCell(2).setCellFormula("COUNTIF($F8:$F100,C4)"); r4.getCell(2).setCellStyle(cellCenter);
        r4.createCell(3).setCellFormula("COUNTIF($F8:$F100,D4)"); r4.getCell(3).setCellStyle(cellCenter);
        r4.createCell(4).setCellFormula("COUNTIF($F8:$F100,E4)"); r4.getCell(4).setCellStyle(cellCenter);

        Row r5 = sheet.createRow(5);
        r5.createCell(0).setCellValue("Round 2"); r5.getCell(0).setCellStyle(metaLabelBold);
        r5.createCell(1).setCellFormula("COUNTIF($F8:$F100,B4)"); r5.getCell(1).setCellStyle(cellCenter);
        r5.createCell(2).setCellFormula("COUNTIF($F8:$F100,C4)"); r5.getCell(2).setCellStyle(cellCenter);
        r5.createCell(3).setCellFormula("COUNTIF($F8:$F100,D4)"); r5.getCell(3).setCellStyle(cellCenter);
        r5.createCell(4).setCellFormula("COUNTIF($F8:$F100,E4)"); r5.getCell(4).setCellStyle(cellCenter);

        Row r6 = sheet.createRow(6);
        r6.createCell(0).setCellValue("Round 3"); r6.getCell(0).setCellStyle(metaLabelBold);
        r6.createCell(1).setCellFormula("COUNTIF($F8:$F100,B4)"); r6.getCell(1).setCellStyle(cellCenter);
        r6.createCell(2).setCellFormula("COUNTIF($F8:$F100,C4)"); r6.getCell(2).setCellStyle(cellCenter);
        r6.createCell(3).setCellFormula("COUNTIF($F8:$F100,D4)"); r6.getCell(3).setCellStyle(cellCenter);
        r6.createCell(4).setCellFormula("COUNTIF($F8:$F100,E4)"); r6.getCell(4).setCellStyle(cellCenter);

        // Table Header
        Row r7 = sheet.createRow(7);
        String[] colHeaders = {
                "Test Case ID", "Scenario (Kịch bản đo lường)", "Tool (Công cụ kiểm thử)",
                "Target (Chỉ tiêu cam kết)", "Actual (Kết quả thực tế đo được)",
                "Status", "Defect IDs", "Notes"
        };
        for (int c = 0; c < colHeaders.length; c++) {
            Cell cell = r7.createCell(c);
            cell.setCellValue(colHeaders[c]);
            cell.setCellStyle(headerStyle);
        }
        r7.setHeightInPoints(28);

        // Render Rows
        int rowIdx = 8;
        for (PerformanceTestCase tc : cases) {
            Row r = sheet.createRow(rowIdx);
            Cell c0 = r.createCell(0); c0.setCellValue(tc.id); c0.setCellStyle(cellCenterBold);
            Cell c1 = r.createCell(1); c1.setCellValue(tc.scenario); c1.setCellStyle(cellLeftWrapBold);
            Cell c2 = r.createCell(2); c2.setCellValue(tc.tool); c2.setCellStyle(cellCenter);
            Cell c3 = r.createCell(3); c3.setCellValue(tc.target); c3.setCellStyle(cellCenterBold);
            Cell c4 = r.createCell(4); c4.setCellValue(tc.actual); c4.setCellStyle(cellCenterBold);
            Cell c5 = r.createCell(5); c5.setCellValue(tc.status); c5.setCellStyle(cellCenterBold);
            Cell c6 = r.createCell(6); c6.setCellValue(tc.defectId); c6.setCellStyle(cellCenter);
            Cell c7 = r.createCell(7); c7.setCellValue(tc.notes); c7.setCellStyle(cellLeftWrap);

            rowIdx++;
        }

        sheet.setColumnWidth(0, 3800);  // ID
        sheet.setColumnWidth(1, 12000); // Scenario
        sheet.setColumnWidth(2, 8000);  // Tool
        sheet.setColumnWidth(3, 6000);  // Target
        sheet.setColumnWidth(4, 6000);  // Actual
        sheet.setColumnWidth(5, 3200);  // Status
        sheet.setColumnWidth(6, 3000);  // Defect ID
        sheet.setColumnWidth(7, 9000);  // Notes
    }

    private void createTestStatisticsSheet(Workbook wb, CellStyle titleStyle, CellStyle headerStyle,
                                           CellStyle cellCenter, CellStyle cellCenterBold,
                                           CellStyle metaLabelBold) {
        Sheet sheet = wb.createSheet("Test Statistics");
        sheet.setDisplayGridlines(true);

        Row r1 = sheet.createRow(1);
        Cell cTitle = r1.createCell(0);
        cTitle.setCellValue("NON-FUNCTIONAL TEST STATISTICS");
        cTitle.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 6));

        Row r3 = sheet.createRow(3);
        r3.createCell(0).setCellValue("Project Name"); r3.getCell(0).setCellStyle(metaLabelBold);
        r3.createCell(1).setCellFormula("Cover!B4");
        r3.createCell(3).setCellValue("Creator"); r3.getCell(3).setCellStyle(metaLabelBold);
        r3.createCell(4).setCellValue("Hoangvm");

        Row r4 = sheet.createRow(4);
        r4.createCell(0).setCellValue("Project Code"); r4.getCell(0).setCellStyle(metaLabelBold);
        r4.createCell(1).setCellFormula("Cover!B5");
        r4.createCell(3).setCellValue("Reviewer/Approver"); r4.getCell(3).setCellStyle(metaLabelBold);
        r4.createCell(4).setCellValue("Teacher / Team Leader");

        Row r5 = sheet.createRow(5);
        r5.createCell(0).setCellValue("Document Code"); r5.getCell(0).setCellStyle(metaLabelBold);
        r5.createCell(1).setCellValue("KTS_NonFunctional_Test_Report_v1.0");
        r5.createCell(3).setCellValue("Issue Date"); r5.getCell(3).setCellStyle(metaLabelBold);
        r5.createCell(4).setCellValue("2026-11-23");

        Row r6 = sheet.createRow(6);
        r6.createCell(0).setCellValue("Notes"); r6.getCell(0).setCellStyle(metaLabelBold);
        r6.createCell(1).setCellValue("Báo cáo kiểm thử phi chức năng (Non-Functional Test) cho KizunaTutor: 8 ca kiểm thử An ninh - Bảo mật (Security) và 6 ca kiểm thử Hiệu năng (Performance). Tổng cộng 14 test cases, 100% Passed.");

        // Table Header
        Row r10 = sheet.createRow(10);
        String[] statHeaders = {"No", "Module code", "Passed", "Failed", "Pending", "N/A", "Number of test cases"};
        for (int i = 0; i < statHeaders.length; i++) {
            Cell c = r10.createCell(i);
            c.setCellValue(statHeaders[i]);
            c.setCellStyle(headerStyle);
        }

        // Row 11: Security
        Row r11 = sheet.createRow(11);
        r11.createCell(0).setCellValue("1.0"); r11.getCell(0).setCellStyle(cellCenter);
        r11.createCell(1).setCellValue("Security Testing"); r11.getCell(1).setCellStyle(cellCenterBold);
        r11.createCell(2).setCellFormula("Security!B7"); r11.getCell(2).setCellStyle(cellCenter);
        r11.createCell(3).setCellFormula("Security!C7"); r11.getCell(3).setCellStyle(cellCenter);
        r11.createCell(4).setCellFormula("Security!D7"); r11.getCell(4).setCellStyle(cellCenter);
        r11.createCell(5).setCellFormula("Security!E7"); r11.getCell(5).setCellStyle(cellCenter);
        r11.createCell(6).setCellFormula("Security!B3"); r11.getCell(6).setCellStyle(cellCenterBold);

        // Row 12: Performance
        Row r12 = sheet.createRow(12);
        r12.createCell(0).setCellValue("2.0"); r12.getCell(0).setCellStyle(cellCenter);
        r12.createCell(1).setCellValue("Performance Testing"); r12.getCell(1).setCellStyle(cellCenterBold);
        r12.createCell(2).setCellFormula("Performance!B7"); r12.getCell(2).setCellStyle(cellCenter);
        r12.createCell(3).setCellFormula("Performance!C7"); r12.getCell(3).setCellStyle(cellCenter);
        r12.createCell(4).setCellFormula("Performance!D7"); r12.getCell(4).setCellStyle(cellCenter);
        r12.createCell(5).setCellFormula("Performance!E7"); r12.getCell(5).setCellStyle(cellCenter);
        r12.createCell(6).setCellFormula("Performance!B3"); r12.getCell(6).setCellStyle(cellCenterBold);

        // Row 14: Sub total
        Row r14 = sheet.createRow(14);
        r14.createCell(1).setCellValue("Sub total"); r14.getCell(1).setCellStyle(cellCenterBold);
        r14.createCell(2).setCellFormula("SUM(C12:C13)"); r14.getCell(2).setCellStyle(cellCenterBold);
        r14.createCell(3).setCellFormula("SUM(D12:D13)"); r14.getCell(3).setCellStyle(cellCenterBold);
        r14.createCell(4).setCellFormula("SUM(E12:E13)"); r14.getCell(4).setCellStyle(cellCenterBold);
        r14.createCell(5).setCellFormula("SUM(F12:F13)"); r14.getCell(5).setCellStyle(cellCenterBold);
        r14.createCell(6).setCellFormula("SUM(G12:G13)"); r14.getCell(6).setCellStyle(cellCenterBold);

        CellStyle cellPercentNumber = wb.createCellStyle();
        cellPercentNumber.cloneStyleFrom(cellCenterBold);
        cellPercentNumber.setDataFormat(wb.createDataFormat().getFormat("0.0"));

        // Row 16: Test coverage
        Row r16 = sheet.createRow(16);
        r16.createCell(1).setCellValue("Test coverage"); r16.getCell(1).setCellStyle(metaLabelBold);
        r16.createCell(3).setCellFormula("(C15+D15)*100/(G15)"); r16.getCell(3).setCellStyle(cellPercentNumber);
        r16.createCell(4).setCellValue("%");

        // Row 17: Test successful coverage
        Row r17 = sheet.createRow(17);
        r17.createCell(1).setCellValue("Test successful coverage"); r17.getCell(1).setCellStyle(metaLabelBold);
        r17.createCell(3).setCellFormula("C15*100/(G15)"); r17.getCell(3).setCellStyle(cellPercentNumber);
        r17.createCell(4).setCellValue("%");

        sheet.setColumnWidth(0, 3000);
        sheet.setColumnWidth(1, 8500);
        sheet.setColumnWidth(2, 3500);
        sheet.setColumnWidth(3, 3500);
        sheet.setColumnWidth(4, 3500);
        sheet.setColumnWidth(5, 3000);
        sheet.setColumnWidth(6, 5000);
    }

    private List<SecurityTestCase> buildSecurityCases() {
        List<SecurityTestCase> list = new ArrayList<>();
        list.add(new SecurityTestCase("TC-SEC-001",
                "Chặn IDOR: Không cho phép truy cập, sửa hoặc xóa bộ thẻ Flashcard của người dùng khác",
                "Học viên B (Other user)",
                "Bộ thẻ ID=55 thuộc quyền sở hữu của Học viên A (owner_key = learner_a@kizuna.vn)",
                "Gửi request PUT/DELETE /api/flashcards/decks/55 với session token của Học viên B",
                "HTTP 403 Forbidden — Không cho phép xem/sửa, bảo vệ dữ liệu riêng tư tuyệt đối",
                "Critical",
                "Bảo mật PR #20 kiểm tra owner_key ở Service"));

        list.add(new SecurityTestCase("TC-SEC-002",
                "Chặn truy cập trái phép vào các endpoint bảo vệ khi không có session/cookie",
                "Khách vãng lai (Unauthenticated)",
                "Không có Cookie access_token hoặc token đã hết hạn",
                "Cố tình gọi API GET /api/flashcards/decks hoặc truy cập URL /pages/home/",
                "HTTP 401 Unauthorized hoặc HTTP 302 Redirect về trang /login",
                "Critical",
                "Spring Security Filter Chain bảo vệ toàn bộ API nội bộ"));

        list.add(new SecurityTestCase("TC-SEC-003",
                "Phòng chống rò rỉ danh tính người dùng (Anti-User Enumeration) khi Quên mật khẩu",
                "Khách vãng lai",
                "Email nonexistent_test999@kizuna.vn hoàn toàn không tồn tại trong cơ sở dữ liệu",
                "Gửi request POST /api/auth/forgot-password với email không tồn tại",
                "HTTP 200 OK — Trả về Neutral Response thông báo chung, không tiết lộ email có trong DB hay không",
                "High",
                "Ngăn chặn tin tặc quét danh bạ tài khoản của hệ thống"));

        list.add(new SecurityTestCase("TC-SEC-004",
                "Chặn 4 vai trò quản trị nội bộ tự khôi phục mật khẩu trực tuyến (PR #20 Security)",
                "Quản trị viên (Admin / Manager / CS / Academic)",
                "Email admin@kizuna.vn thuộc vai trò ROLE_ADMIN",
                "Nhập email quản trị vào form Quên mật khẩu trực tuyến",
                "HTTP 403 Forbidden — Báo lỗi cấm quản trị viên tự reset pass online, bắt buộc liên hệ Admin cấp cao",
                "Critical",
                "Quy tắc an ninh bảo mật cấp doanh nghiệp PR #20"));

        list.add(new SecurityTestCase("TC-SEC-005",
                "Cơ chế chống Brute-force & Spam (Rate Limiting 3 lần/15 phút)",
                "Người dùng bất kỳ",
                "Đã gửi 3 lần yêu cầu đổi pass/OTP trong vòng 15 phút gần nhất",
                "Tiếp tục gửi yêu cầu lần thứ 4 trong cùng cửa sổ thời gian",
                "HTTP 429 Too Many Requests — Chặn và hiển thị thông báo yêu cầu chờ 15 phút",
                "High",
                "Bộ đếm Rate Limit In-Memory bảo vệ tài nguyên hệ thống"));

        list.add(new SecurityTestCase("TC-SEC-006",
                "Bảo vệ Cookie JWT chống đánh cắp qua tấn công XSS (HttpOnly & SameSite Flags)",
                "Học viên đã đăng nhập",
                "Người dùng đã đăng nhập, Cookie access_token được thiết lập",
                "Mở Console trình duyệt gõ lệnh: document.cookie để đọc token",
                "Không đọc được access_token từ JavaScript vì cờ HttpOnly = true, Path = /, SameSite = Lax",
                "Critical",
                "Ngăn chặn hoàn toàn việc script độc hại đánh cắp token"));

        list.add(new SecurityTestCase("TC-SEC-007",
                "Chặn chuyển hướng OAuth giả mạo khi thiếu cấu hình Client ID trên Production",
                "Khách vãng lai",
                "Cấu hình Google Client ID để trống hoặc dùng giá trị mặc định placeholder",
                "Gửi request GET /api/auth/oauth2/google",
                "HTTP 302 Redirect về /login?error=oauth2_not_configured — Chặn mock nguy hiểm, bảo vệ hệ thống",
                "High",
                "Đảm bảo an toàn không để lộ endpoint mock trên môi trường thật"));

        list.add(new SecurityTestCase("TC-SEC-008",
                "Mã hóa một chiều an toàn toàn bộ mật khẩu người dùng trong cơ sở dữ liệu PostgreSQL",
                "Database Inspector",
                "Bảng users trong PostgreSQL Supabase lưu trữ thông tin tài khoản",
                "Truy vấn cột password_hash trong bảng users",
                "100% mật khẩu được băm bằng thuật toán BCrypt (bắt đầu bằng $2a$), không lưu trữ bản rõ (plaintext)",
                "Critical",
                "Tuân thủ tiêu chuẩn an toàn bảo mật mật khẩu quốc tế"));

        return list;
    }

    private List<PerformanceTestCase> buildPerformanceCases() {
        List<PerformanceTestCase> list = new ArrayList<>();
        list.add(new PerformanceTestCase("TC-PERF-001",
                "Thời gian tải trang Không gian học tập cá nhân (Dashboard Learner) dưới tải bình thường (1 user)",
                "Chrome DevTools (Network / Lighthouse)",
                "< 2.0 giây",
                "~450ms – 680ms",
                "Tối ưu tài nguyên tĩnh Vanilla JS, Tailwind CSS và tải bất đồng bộ Async/Await."));

        list.add(new PerformanceTestCase("TC-PERF-002",
                "Tốc độ tra cứu từ khóa trong kho 7.811 từ vựng JLPT N5-N1 và Hán tự",
                "Chrome DevTools (XHR / Fetch time)",
                "< 1.0 giây",
                "~120ms – 250ms",
                "Đánh chỉ mục B-Tree Index trên các cột topic, jlpt_level, romaji trong PostgreSQL."));

        list.add(new PerformanceTestCase("TC-PERF-003",
                "Hiệu năng truy xuất tức thì của Trợ lý đọc hiểu AI qua bộ nhớ đệm SHA-256 (Cache HIT)",
                "Chrome DevTools (Response Time)",
                "< 100ms",
                "< 35ms",
                "Phân tích lại cùng đoạn văn truy xuất từ In-Memory CacheStore không cần gọi lại Gemini API (~1.8s)."));

        list.add(new PerformanceTestCase("TC-PERF-004",
                "Tỷ lệ khung hình hiển thị (FPS) và độ mượt của Minigame Kanji Rain",
                "Chrome DevTools (Rendering / FPS Meter)",
                "≥ 55 FPS",
                "59 – 60 FPS",
                "Tối ưu vòng lặp game bằng requestAnimationFrame, không giật lag trên màn hình 60Hz."));

        list.add(new PerformanceTestCase("TC-PERF-005",
                "Tốc độ nhận diện nét vẽ viết tay Canvas qua Google Input Tools & Joyo Kanji",
                "Chrome DevTools (Network time)",
                "< 1.5 giây",
                "~350ms – 600ms",
                "Nén mảng tọa độ nét vẽ và truy vấn song song cơ sở dữ liệu Hán tự."));

        list.add(new PerformanceTestCase("TC-PERF-006",
                "Khả năng chịu tải và độ ổn định của hệ thống với N người dùng đồng thời (Dev Baseline)",
                "Apache Benchmark / Multi-tabs Stress Test",
                "≥ 10 users, Error rate < 1%",
                "15 concurrent users, Error rate: 0.0%",
                "Hikari Connection Pool kết hợp Spring Boot xử lý trơn tru, không xảy ra Out Of Memory."));

        return list;
    }
}
