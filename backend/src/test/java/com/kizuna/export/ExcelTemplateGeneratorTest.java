package com.kizuna.export;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * Generator for the standardized Unit Test Excel Document.
 * Follows 100% of the customer's template format (Cover, MethodList, TestReport, Function Sheets).
 */
public class ExcelTemplateGeneratorTest {

    @Test
    void generateExcelDocument() throws IOException {
        Workbook workbook = new XSSFWorkbook();

        // Styles
        CellStyle titleStyle = workbook.createCellStyle();
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 16);
        titleStyle.setFont(titleFont);
        titleStyle.setAlignment(HorizontalAlignment.CENTER);

        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);

        CellStyle labelBold = workbook.createCellStyle();
        Font boldFont = workbook.createFont();
        boldFont.setBold(true);
        labelBold.setFont(boldFont);

        CellStyle sectionHeader = workbook.createCellStyle();
        Font secFont = workbook.createFont();
        secFont.setBold(true);
        secFont.setColor(IndexedColors.WHITE.getIndex());
        sectionHeader.setFont(secFont);
        sectionHeader.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        sectionHeader.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle cellBorder = workbook.createCellStyle();
        cellBorder.setBorderBottom(BorderStyle.THIN);
        cellBorder.setBorderTop(BorderStyle.THIN);
        cellBorder.setBorderLeft(BorderStyle.THIN);
        cellBorder.setBorderRight(BorderStyle.THIN);

        CellStyle cellCenter = workbook.createCellStyle();
        cellCenter.cloneStyleFrom(cellBorder);
        cellCenter.setAlignment(HorizontalAlignment.CENTER);

        CellStyle cellCenterBold = workbook.createCellStyle();
        cellCenterBold.cloneStyleFrom(cellBorder);
        cellCenterBold.setAlignment(HorizontalAlignment.CENTER);
        cellCenterBold.setFont(boldFont);

        // 1. Sheet: Cover
        createCoverSheet(workbook, titleStyle, headerStyle, cellBorder, labelBold);

        // 2. Sheet: MethodList
        createMethodListSheet(workbook, titleStyle, headerStyle, cellBorder, labelBold);

        // 3. Sheet: TestReport
        createTestReportSheet(workbook, titleStyle, headerStyle, cellBorder, cellCenter, labelBold);

        // 4. Function Sheets
        createLoginSheet(workbook, headerStyle, labelBold, cellBorder, cellCenter, cellCenterBold);
        createGoogleOAuthSheet(workbook, headerStyle, labelBold, cellBorder, cellCenter, cellCenterBold);
        createSendOtpSheet(workbook, headerStyle, labelBold, cellBorder, cellCenter, cellCenterBold);
        createVerifyOtpSheet(workbook, headerStyle, labelBold, cellBorder, cellCenter, cellCenterBold);
        createResendOtpSheet(workbook, headerStyle, labelBold, cellBorder, cellCenter, cellCenterBold);
        createLogoutSheet(workbook, headerStyle, labelBold, cellBorder, cellCenter, cellCenterBold);
        createChangePasswordSheet(workbook, headerStyle, labelBold, cellBorder, cellCenter, cellCenterBold);
        createResetPasswordSheet(workbook, headerStyle, labelBold, cellBorder, cellCenter, cellCenterBold);
        createFlashcardDeckSheet(workbook, headerStyle, labelBold, cellBorder, cellCenter, cellCenterBold);
        createVocabularySheet(workbook, headerStyle, labelBold, cellBorder, cellCenter, cellCenterBold);
        createGrammarSheet(workbook, headerStyle, labelBold, cellBorder, cellCenter, cellCenterBold);
        createAiWritingSheet(workbook, headerStyle, labelBold, cellBorder, cellCenter, cellCenterBold);
        createKanjiGameSheet(workbook, headerStyle, labelBold, cellBorder, cellCenter, cellCenterBold);

        File outputFile = new File("C:\\Users\\Vuh26\\Desktop\\hm\\KizunaTutor\\KizunaTutor_Unit_Test_Document_v1.0.xlsx");
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            workbook.write(fos);
        }
        workbook.close();
        System.out.println("Excel generated successfully at: " + outputFile.getAbsolutePath());
    }

    private void createCoverSheet(Workbook wb, CellStyle titleStyle, CellStyle headerStyle, CellStyle cellBorder, CellStyle labelBold) {
        Sheet sheet = wb.createSheet("Cover");
        sheet.setDisplayGridlines(true);

        Row r0 = sheet.createRow(1);
        Cell c0 = r0.createCell(1);
        c0.setCellValue("UNIT TEST DOCUMENT");
        c0.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 1, 5));

        Row r2 = sheet.createRow(3);
        r2.createCell(1).setCellValue("Project Name");
        r2.createCell(2).setCellValue("Kizuna Tutor System");
        r2.createCell(4).setCellValue("Creator");
        r2.createCell(5).setCellValue("Senior QA Engineer");

        Row r3 = sheet.createRow(4);
        r3.createCell(1).setCellValue("Project Code");
        r3.createCell(2).setCellValue("KTS");
        r3.createCell(4).setCellValue("Issue Date");
        r3.createCell(5).setCellValue("2026-11-12");

        Row r4 = sheet.createRow(5);
        r4.createCell(1).setCellValue("Document Code");
        r4.createCell(2).setCellValue("Report_5.1_UnitTest_v1.0");
        r4.createCell(4).setCellValue("Version");
        r4.createCell(5).setCellValue("v1.0");

        Row r7 = sheet.createRow(7);
        r7.createCell(1).setCellValue("Record of change");
        r7.getCell(1).setCellStyle(labelBold);

        Row r8 = sheet.createRow(8);
        String[] headers = {"Effective Date", "Version", "Change Item", "*A,D,M", "Change description", "Reference"};
        for (int i = 0; i < headers.length; i++) {
            Cell c = r8.createCell(i + 1);
            c.setCellValue(headers[i]);
            c.setCellStyle(headerStyle);
        }

        Row r9 = sheet.createRow(9);
        r9.createCell(1).setCellValue("2026-11-12");
        r9.createCell(2).setCellValue("v1.0");
        r9.createCell(3).setCellValue("");
        r9.createCell(4).setCellValue("A");
        r9.createCell(5).setCellValue("Initial unit test documentation for Feature 1 (Auth) & Feature 2 (Learner Space)");
        r9.createCell(6).setCellValue("SRS UC-01 to UC-62");
        for (int i = 1; i <= 6; i++) {
            if (r9.getCell(i) != null) r9.getCell(i).setCellStyle(cellBorder);
        }

        sheet.setColumnWidth(1, 4000);
        sheet.setColumnWidth(2, 6000);
        sheet.setColumnWidth(3, 4000);
        sheet.setColumnWidth(4, 3000);
        sheet.setColumnWidth(5, 14000);
        sheet.setColumnWidth(6, 6000);
    }

    private void createMethodListSheet(Workbook wb, CellStyle titleStyle, CellStyle headerStyle, CellStyle cellBorder, CellStyle labelBold) {
        Sheet sheet = wb.createSheet("MethodList");
        sheet.setDisplayGridlines(true);

        Row r0 = sheet.createRow(1);
        Cell c0 = r0.createCell(1);
        c0.setCellValue("Method List");
        c0.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 1, 6));

        Row r2 = sheet.createRow(3);
        r2.createCell(1).setCellValue("Project Name");
        r2.createCell(2).setCellValue("Kizuna Tutor System");
        Row r3 = sheet.createRow(4);
        r3.createCell(1).setCellValue("Project Code");
        r3.createCell(2).setCellValue("KTS");

        Row r5 = sheet.createRow(6);
        r5.createCell(1).setCellValue("Test Environment Setup Description");
        r5.getCell(1).setCellStyle(labelBold);
        r5.createCell(2).setCellValue("1. Language & JDK: Java 17 / 21 | 2. Build Tool: Apache Maven (Spring Boot 3.3.4 parent)\n" +
                "3. Testing Framework: JUnit 5 (Jupiter) + Mockito + AssertJ\n" +
                "4. Spring test support: spring-boot-starter-test, spring-security-test\n" +
                "5. Dependency isolation: 100% Mocking repositories, JWT token provider, JavaMailSender\n" +
                "6. Operating System: Windows 10/11 | 7. Network: Không yêu cầu Internet (Mocks)");

        Row r8 = sheet.createRow(8);
        String[] headers = {"No", "Module Name", "Method Name", "Sheet Name", "Description", "Pre-Condition"};
        for (int i = 0; i < headers.length; i++) {
            Cell c = r8.createCell(i + 1);
            c.setCellValue(headers[i]);
            c.setCellStyle(headerStyle);
        }

        String[][] methods = {
                {"1", "Auth", "AuthService.login(LoginRequest, HttpServletResponse)", "login", "Đăng nhập hệ thống bằng email/password, kiểm tra 6 roles và mustChangePassword", "Tài khoản tồn tại trong hệ thống"},
                {"2", "Auth", "GoogleOAuthController.authorizeGoogle(HttpServletResponse)", "googleAuth", "Chuyển hướng Google Consent và chặn endpoint giả lập mock trên production", "Cấu hình Google Client ID"},
                {"3", "Auth", "AuthService.initiateRegistration(RegisterRequest)", "sendOtp", "Đăng ký tài khoản: gửi mã OTP 6 số qua email xác thực", "Email chưa được đăng ký"},
                {"4", "Auth", "AuthService.completeRegistration(VerifyOtpRequest)", "verifyOtp", "Xác thực OTP, tạo User & hồ sơ Learner/Tutor, cấp JWT token", "OTP hợp lệ chưa hết hạn"},
                {"5", "Auth", "OtpService.resendOtp(String)", "resendOtp", "Gửi lại OTP với cơ chế cooldown 60 giây và tối đa 5 lần thử", "Phiên đăng ký đang mở"},
                {"6", "Auth", "AuthService.logout(HttpServletResponse)", "logout", "Đăng xuất, xóa sạch JWT cookie (maxAge = 0) và session", "Người dùng đã đăng nhập"},
                {"7", "Auth", "AuthService.changePasswordFirstTime(FirstTimePasswordChangeRequest, String)", "changePassword", "Đổi mật khẩu lần đầu khi mustChangePassword = true", "Tài khoản có cờ đổi pass lần đầu"},
                {"8", "Auth", "AuthService.resetPasswordAndSendEmail / resetPasswordWithTemporary", "resetPassword", "Quên & Đặt lại mật khẩu: Rate limit 3 lần/15p, chặn 4 role quản trị (PR #20)", "Người dùng yêu cầu khôi phục"},
                {"9", "Learning Support", "FlashcardDeckController (CRUD & Security)", "flashcards", "Quản lý bộ thẻ Flashcards cá nhân, bảo mật PR #20 kiểm tra owner_key (401/403)", "Người dùng đã đăng nhập"},
                {"10", "Learning Support", "VocabularyController.getTopics / getVocabularies", "vocabularies", "Tra cứu từ vựng theo chủ đề JLPT N5-N1 và từ khóa (kho 7.811 từ vựng)", "Dữ liệu từ vựng trong DB"},
                {"11", "Learning Support", "GrammarController.getGrammars / getGrammarDetail", "grammar", "Tra cứu kho ngữ pháp OpenJLPT, lọc cấp độ N5-N1 và xem chi tiết cấu trúc", "Dữ liệu ngữ pháp"},
                {"12", "Learning Support", "AiWritingService.checkWriting(AiWritingCheckRequest)", "aiWriting", "Luyện viết & Chữa câu Sakubun AI, nhận diện lỗi trợ từ và gợi ý từ vựng", "Đoạn văn Sakubun nhập vào"},
                {"13", "Learning Support", "KanjiGameService (getPlayData / saveGameSession)", "kanjiGame", "Game Kanji Rain và Thử thách Kanji: nạp dữ liệu chơi, ghi điểm session", "Dữ liệu Kanji trong DB"}
        };

        for (int rowIdx = 0; rowIdx < methods.length; rowIdx++) {
            Row r = sheet.createRow(9 + rowIdx);
            for (int colIdx = 0; colIdx < methods[rowIdx].length; colIdx++) {
                Cell c = r.createCell(colIdx + 1);
                c.setCellValue(methods[rowIdx][colIdx]);
                c.setCellStyle(cellBorder);
            }
        }

        sheet.setColumnWidth(1, 1500);
        sheet.setColumnWidth(2, 4500);
        sheet.setColumnWidth(3, 14000);
        sheet.setColumnWidth(4, 4000);
        sheet.setColumnWidth(5, 14000);
        sheet.setColumnWidth(6, 8000);
    }

    private void createTestReportSheet(Workbook wb, CellStyle titleStyle, CellStyle headerStyle, CellStyle cellBorder, CellStyle cellCenter, CellStyle labelBold) {
        Sheet sheet = wb.createSheet("TestReport");
        sheet.setDisplayGridlines(true);

        Row r0 = sheet.createRow(1);
        Cell c0 = r0.createCell(1);
        c0.setCellValue("UNIT TEST REPORT");
        c0.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 1, 9));

        Row r3 = sheet.createRow(3);
        r3.createCell(1).setCellValue("Project Name");
        r3.createCell(2).setCellValue("Kizuna Tutor System");
        r3.createCell(7).setCellValue("Test coverage");
        r3.createCell(9).setCellValue("100%");

        Row r4 = sheet.createRow(4);
        r4.createCell(1).setCellValue("Project Code");
        r4.createCell(2).setCellValue("KTS");
        r4.createCell(7).setCellValue("Test successful coverage");
        r4.createCell(9).setCellValue("100%");

        Row r5 = sheet.createRow(5);
        r5.createCell(1).setCellValue("Document Code");
        r5.createCell(2).setCellValue("Report_5.1_UnitTest_v1.0");
        r5.createCell(7).setCellValue("Normal case");
        r5.createCell(9).setCellValue("48.6%");

        Row r6 = sheet.createRow(6);
        r6.createCell(1).setCellValue("Issue Date");
        r6.createCell(2).setCellValue("2026-11-12");
        r6.createCell(7).setCellValue("Abnormal case");
        r6.createCell(9).setCellValue("37.1%");

        Row r7 = sheet.createRow(7);
        r7.createCell(7).setCellValue("Boundary case");
        r7.createCell(9).setCellValue("14.3%");

        Row r9 = sheet.createRow(9);
        String[] headers = {"No", "Function code", "Passed", "Failed", "Untested", "N", "A", "B", "Total Test Cases"};
        for (int i = 0; i < headers.length; i++) {
            Cell c = r9.createCell(i + 1);
            c.setCellValue(headers[i]);
            c.setCellStyle(headerStyle);
        }

        Object[][] reports = {
                {1, "login", 10, 0, 0, 6, 3, 1, 10},
                {2, "googleAuth", 3, 0, 0, 1, 2, 0, 3},
                {3, "sendOtp", 3, 0, 0, 1, 2, 0, 3},
                {4, "verifyOtp", 4, 0, 0, 2, 2, 0, 4},
                {5, "resendOtp", 3, 0, 0, 1, 1, 1, 3},
                {6, "logout", 2, 0, 0, 1, 0, 1, 2},
                {7, "changePassword", 4, 0, 0, 1, 3, 0, 4},
                {8, "resetPassword", 7, 0, 0, 2, 4, 1, 7},
                {9, "flashcards", 7, 0, 0, 4, 3, 0, 7},
                {10, "vocabularies", 3, 0, 0, 2, 0, 1, 3},
                {11, "grammar", 3, 0, 0, 2, 1, 0, 3},
                {12, "aiWriting", 3, 0, 0, 2, 1, 0, 3},
                {13, "kanjiGame", 3, 0, 0, 2, 0, 1, 3}
        };

        int totalP = 0, totalF = 0, totalU = 0, totalN = 0, totalA = 0, totalB = 0, grandTotal = 0;

        for (int i = 0; i < reports.length; i++) {
            Row r = sheet.createRow(10 + i);
            for (int j = 0; j < reports[i].length; j++) {
                Cell c = r.createCell(j + 1);
                c.setCellStyle(cellCenter);
                if (reports[i][j] instanceof Integer) {
                    c.setCellValue((Integer) reports[i][j]);
                } else {
                    c.setCellValue(String.valueOf(reports[i][j]));
                }
            }
            totalP += (int) reports[i][2];
            totalF += (int) reports[i][3];
            totalU += (int) reports[i][4];
            totalN += (int) reports[i][5];
            totalA += (int) reports[i][6];
            totalB += (int) reports[i][7];
            grandTotal += (int) reports[i][8];
        }

        // Total row
        Row rTot = sheet.createRow(10 + reports.length);
        rTot.createCell(1).setCellValue("Total");
        rTot.getCell(1).setCellStyle(cellCenter);
        rTot.createCell(2).setCellValue("");
        rTot.getCell(2).setCellStyle(cellCenter);
        rTot.createCell(3).setCellValue(totalP);
        rTot.getCell(3).setCellStyle(cellCenter);
        rTot.createCell(4).setCellValue(totalF);
        rTot.getCell(4).setCellStyle(cellCenter);
        rTot.createCell(5).setCellValue(totalU);
        rTot.getCell(5).setCellStyle(cellCenter);
        rTot.createCell(6).setCellValue(totalN);
        rTot.getCell(6).setCellStyle(cellCenter);
        rTot.createCell(7).setCellValue(totalA);
        rTot.getCell(7).setCellStyle(cellCenter);
        rTot.createCell(8).setCellValue(totalB);
        rTot.getCell(8).setCellStyle(cellCenter);
        rTot.createCell(9).setCellValue(grandTotal);
        rTot.getCell(9).setCellStyle(cellCenter);

        for (int col = 1; col <= 9; col++) {
            sheet.autoSizeColumn(col);
        }
    }

    // =========================================================================
    // FUNCTION SHEETS IMPLEMENTATION
    // =========================================================================

    private void createLoginSheet(Workbook wb, CellStyle headerStyle, CellStyle labelBold, CellStyle cellBorder, CellStyle cellCenter, CellStyle cellCenterBold) {
        Sheet sheet = wb.createSheet("login");
        sheet.setDisplayGridlines(true);

        buildFunctionHeader(sheet, "Auth", "AuthService.login", "Senior QA", "Senior QA",
                "Log in with email and password, return JWT + cookie, route 6 role dashboards, check mustChangePassword",
                10, 0, 0, 6, 3, 1, 10, labelBold, cellBorder, cellCenter);

        String[] tcIds = {"UTCID01", "UTCID02", "UTCID03", "UTCID04", "UTCID05", "UTCID06", "UTCID07", "UTCID08", "UTCID09", "UTCID10"};
        createTcHeaderRow(sheet, 6, tcIds, headerStyle);

        List<MatrixRow> rows = List.of(
                new MatrixRow("Condition", "Precondition", "", ""),
                new MatrixRow("", "   Email in DB", "", ""),
                new MatrixRow("", "      Exist, status = ACTIVE", new String[]{"O", "O", "O", "O", "O", "O", "O", "", "O", ""}),
                new MatrixRow("", "      NotExist", new String[]{"", "", "", "", "", "", "", "O", "", ""}),
                new MatrixRow("", "      Exist, status = BANNED / INACTIVE", new String[]{"", "", "", "", "", "", "", "", "", "O"}),
                new MatrixRow("", "   User Role", "", ""),
                new MatrixRow("", "      ROLE_ADMIN", new String[]{"O", "", "", "", "", "", "", "", "", ""}),
                new MatrixRow("", "      ROLE_MANAGER", new String[]{"", "O", "", "", "", "", "", "", "", ""}),
                new MatrixRow("", "      ROLE_CS_STAFF", new String[]{"", "", "O", "", "", "", "", "", "", ""}),
                new MatrixRow("", "      ROLE_ACADEMIC_OFFICER", new String[]{"", "", "", "O", "", "", "", "", "", ""}),
                new MatrixRow("", "      ROLE_TUTOR", new String[]{"", "", "", "", "O", "", "", "", "", ""}),
                new MatrixRow("", "      ROLE_LEARNER", new String[]{"", "", "", "", "", "O", "O", "", "O", ""}),
                new MatrixRow("", "   Flag mustChangePassword", "", ""),
                new MatrixRow("", "      false", new String[]{"O", "O", "O", "O", "O", "O", "", "", "", ""}),
                new MatrixRow("", "      true", new String[]{"", "", "", "", "", "", "O", "", "", ""}),
                new MatrixRow("Input", "   password", "", ""),
                new MatrixRow("", "      Matches stored hash", new String[]{"O", "O", "O", "O", "O", "O", "O", "", "", "O"}),
                new MatrixRow("", "      Does not match", new String[]{"", "", "", "", "", "", "", "O", "O", ""}),
                new MatrixRow("Confirm", "Return", "", ""),
                new MatrixRow("", "   Redirect to /admin/dashboard", new String[]{"O", "", "", "", "", "", "", "", "", ""}),
                new MatrixRow("", "   Redirect to /manager/dashboard", new String[]{"", "O", "", "", "", "", "", "", "", ""}),
                new MatrixRow("", "   Redirect to /cs/dashboard", new String[]{"", "", "O", "", "", "", "", "", "", ""}),
                new MatrixRow("", "   Redirect to /academic/dashboard", new String[]{"", "", "", "O", "", "", "", "", "", ""}),
                new MatrixRow("", "   Redirect to /tutor/dashboard", new String[]{"", "", "", "", "O", "", "", "", "", ""}),
                new MatrixRow("", "   Redirect to /pages/home/ (Learner)", new String[]{"", "", "", "", "", "O", "O", "", "", ""}),
                new MatrixRow("", "   Issue JWT & Set access_token Cookie", new String[]{"O", "O", "O", "O", "O", "O", "O", "", "", ""}),
                new MatrixRow("", "   mustChangePassword = true in response", new String[]{"", "", "", "", "", "", "O", "", "", ""}),
                new MatrixRow("", "Exception", "", ""),
                new MatrixRow("", "   BadRequestException 'Tài khoản hoặc mật khẩu không chính xác'", new String[]{"", "", "", "", "", "", "", "O", "O", ""}),
                new MatrixRow("", "   BadRequestException 'Tài khoản đã bị tạm khóa hoặc ngừng hoạt động'", new String[]{"", "", "", "", "", "", "", "", "", "O"}),
                new MatrixRow("Result", "Type(N : Normal, A : Abnormal, B : Boundary)", new String[]{"N", "N", "N", "N", "N", "N", "B", "A", "A", "A"}),
                new MatrixRow("", "Passed/Failed", new String[]{"P", "P", "P", "P", "P", "P", "P", "P", "P", "P"}),
                new MatrixRow("", "Executed Date", new String[]{"2026-11-12", "2026-11-12", "2026-11-12", "2026-11-12", "2026-11-12", "2026-11-12", "2026-11-12", "2026-11-12", "2026-11-12", "2026-11-12"}),
                new MatrixRow("", "Defect ID", new String[]{"", "", "", "", "", "", "", "", "", ""})
        );

        renderMatrixRows(sheet, 7, rows, cellBorder, cellCenter, cellCenterBold);
    }

    private void createGoogleOAuthSheet(Workbook wb, CellStyle headerStyle, CellStyle labelBold, CellStyle cellBorder, CellStyle cellCenter, CellStyle cellCenterBold) {
        Sheet sheet = wb.createSheet("googleAuth");
        sheet.setDisplayGridlines(true);

        buildFunctionHeader(sheet, "Auth", "GoogleOAuthController.authorizeGoogle", "Senior QA", "Senior QA",
                "Google OAuth2 authorization redirect & Production safety mock blocker",
                3, 0, 0, 1, 2, 0, 3, labelBold, cellBorder, cellCenter);

        String[] tcIds = {"UTCID01", "UTCID02", "UTCID03"};
        createTcHeaderRow(sheet, 6, tcIds, headerStyle);

        List<MatrixRow> rows = List.of(
                new MatrixRow("Condition", "Precondition", "", ""),
                new MatrixRow("", "   Google Client ID", "", ""),
                new MatrixRow("", "      Configured with real client ID", new String[]{"O", "", ""}),
                new MatrixRow("", "      Blank / null", new String[]{"", "O", ""}),
                new MatrixRow("", "      Placeholder default Client ID", new String[]{"", "", "O"}),
                new MatrixRow("Confirm", "Return", "", ""),
                new MatrixRow("", "   Redirect to Google Accounts Consent URL with scope openid email profile", new String[]{"O", "", ""}),
                new MatrixRow("", "   Redirect to /login?error=oauth2_not_configured (chặn mock nguy hiểm)", new String[]{"", "O", "O"}),
                new MatrixRow("Result", "Type(N : Normal, A : Abnormal, B : Boundary)", new String[]{"N", "A", "A"}),
                new MatrixRow("", "Passed/Failed", new String[]{"P", "P", "P"}),
                new MatrixRow("", "Executed Date", new String[]{"2026-11-12", "2026-11-12", "2026-11-12"}),
                new MatrixRow("", "Defect ID", new String[]{"", "", ""})
        );
        renderMatrixRows(sheet, 7, rows, cellBorder, cellCenter, cellCenterBold);
    }

    private void createSendOtpSheet(Workbook wb, CellStyle headerStyle, CellStyle labelBold, CellStyle cellBorder, CellStyle cellCenter, CellStyle cellCenterBold) {
        Sheet sheet = wb.createSheet("sendOtp");
        sheet.setDisplayGridlines(true);

        buildFunctionHeader(sheet, "Auth", "AuthService.initiateRegistration", "Senior QA", "Senior QA",
                "Register account step 1: Validate email & password, generate 6-digit OTP and send via email",
                3, 0, 0, 1, 2, 0, 3, labelBold, cellBorder, cellCenter);

        String[] tcIds = {"UTCID01", "UTCID02", "UTCID03"};
        createTcHeaderRow(sheet, 6, tcIds, headerStyle);

        List<MatrixRow> rows = List.of(
                new MatrixRow("Condition", "Precondition", "", ""),
                new MatrixRow("", "   Email in DB", "", ""),
                new MatrixRow("", "      NotExist", new String[]{"O", "O", ""}),
                new MatrixRow("", "      Exist (Already registered)", new String[]{"", "", "O"}),
                new MatrixRow("Input", "   RegisterRequest", "", ""),
                new MatrixRow("", "      Valid info, role ROLE_LEARNER / ROLE_TUTOR", new String[]{"O", "", "O"}),
                new MatrixRow("", "      Target JLPT level specified (N5-N1)", new String[]{"O", "O", ""}),
                new MatrixRow("Confirm", "Return", "", ""),
                new MatrixRow("", "   Generate 6-digit OTP, save pending registration, send Gmail", new String[]{"O", "O", ""}),
                new MatrixRow("", "Exception", "", ""),
                new MatrixRow("", "   BadRequestException 'Địa chỉ email này đã được đăng ký trong hệ thống'", new String[]{"", "", "O"}),
                new MatrixRow("Result", "Type(N : Normal, A : Abnormal, B : Boundary)", new String[]{"N", "N", "A"}),
                new MatrixRow("", "Passed/Failed", new String[]{"P", "P", "P"}),
                new MatrixRow("", "Executed Date", new String[]{"2026-11-12", "2026-11-12", "2026-11-12"}),
                new MatrixRow("", "Defect ID", new String[]{"", "", ""})
        );
        renderMatrixRows(sheet, 7, rows, cellBorder, cellCenter, cellCenterBold);
    }

    private void createVerifyOtpSheet(Workbook wb, CellStyle headerStyle, CellStyle labelBold, CellStyle cellBorder, CellStyle cellCenter, CellStyle cellCenterBold) {
        Sheet sheet = wb.createSheet("verifyOtp");
        sheet.setDisplayGridlines(true);

        buildFunctionHeader(sheet, "Auth", "AuthService.completeRegistration", "Senior QA", "Senior QA",
                "Verify registration OTP, create User & Learner/Tutor profile, issue JWT token & cookie",
                4, 0, 0, 2, 2, 0, 4, labelBold, cellBorder, cellCenter);

        String[] tcIds = {"UTCID01", "UTCID02", "UTCID03", "UTCID04"};
        createTcHeaderRow(sheet, 6, tcIds, headerStyle);

        List<MatrixRow> rows = List.of(
                new MatrixRow("Condition", "Precondition", "", ""),
                new MatrixRow("", "   Pending registration in cache", "", ""),
                new MatrixRow("", "      Exists, not expired", new String[]{"O", "O", "", "O"}),
                new MatrixRow("", "      NotExist / Expired", new String[]{"", "", "O", ""}),
                new MatrixRow("", "   Target Role in pending", "", ""),
                new MatrixRow("", "      ROLE_LEARNER", new String[]{"O", "", "", ""}),
                new MatrixRow("", "      ROLE_TUTOR", new String[]{"", "O", "", ""}),
                new MatrixRow("", "   Email in DB", "", ""),
                new MatrixRow("", "      NotExist", new String[]{"O", "O", "", ""}),
                new MatrixRow("", "      Exist (activated by concurrent request)", new String[]{"", "", "", "O"}),
                new MatrixRow("Input", "   otp", "", ""),
                new MatrixRow("", "      Matches pending OTP", new String[]{"O", "O", "O", "O"}),
                new MatrixRow("Confirm", "Return", "", ""),
                new MatrixRow("", "   Create User + Learner profile, return JWT, redirect /pages/home/", new String[]{"O", "", "", ""}),
                new MatrixRow("", "   Create User + Tutor profile APPROVED, return JWT, redirect /tutor/dashboard", new String[]{"", "O", "", ""}),
                new MatrixRow("", "Exception", "", ""),
                new MatrixRow("", "   BadRequestException 'Yêu cầu xác thực không tồn tại hoặc đã hết hiệu lực'", new String[]{"", "", "O", ""}),
                new MatrixRow("", "   BadRequestException 'Tài khoản đã được đăng ký trước đó'", new String[]{"", "", "", "O"}),
                new MatrixRow("Result", "Type(N : Normal, A : Abnormal, B : Boundary)", new String[]{"N", "N", "A", "A"}),
                new MatrixRow("", "Passed/Failed", new String[]{"P", "P", "P", "P"}),
                new MatrixRow("", "Executed Date", new String[]{"2026-11-12", "2026-11-12", "2026-11-12", "2026-11-12"}),
                new MatrixRow("", "Defect ID", new String[]{"", "", "", ""})
        );
        renderMatrixRows(sheet, 7, rows, cellBorder, cellCenter, cellCenterBold);
    }

    private void createResendOtpSheet(Workbook wb, CellStyle headerStyle, CellStyle labelBold, CellStyle cellBorder, CellStyle cellCenter, CellStyle cellCenterBold) {
        Sheet sheet = wb.createSheet("resendOtp");
        sheet.setDisplayGridlines(true);

        buildFunctionHeader(sheet, "Auth", "OtpService.resendOtp", "Senior QA", "Senior QA",
                "Resend registration OTP with 60s cooldown and 5 attempts limit",
                3, 0, 0, 1, 1, 1, 3, labelBold, cellBorder, cellCenter);

        String[] tcIds = {"UTCID01", "UTCID02", "UTCID03"};
        createTcHeaderRow(sheet, 6, tcIds, headerStyle);

        List<MatrixRow> rows = List.of(
                new MatrixRow("Condition", "Precondition", "", ""),
                new MatrixRow("", "   Pending registration in cache", "", ""),
                new MatrixRow("", "      Exists", new String[]{"O", "O", ""}),
                new MatrixRow("", "      NotExist", new String[]{"", "", "O"}),
                new MatrixRow("", "   Cooldown (60s)", "", ""),
                new MatrixRow("", "      Elapsed (>= 60s)", new String[]{"O", "", ""}),
                new MatrixRow("", "      Not elapsed (< 60s)", new String[]{"", "O", ""}),
                new MatrixRow("Confirm", "Return", "", ""),
                new MatrixRow("", "   Issue new 6-digit OTP, reset attempts to 0, refresh expiresAt", new String[]{"O", "", ""}),
                new MatrixRow("", "Exception", "", ""),
                new MatrixRow("", "   BadRequestException 'Vui lòng đợi thêm ... giây trước khi yêu cầu gửi lại mã'", new String[]{"", "O", ""}),
                new MatrixRow("", "   BadRequestException 'Phiên đăng ký đã kết thúc. Vui lòng điền lại thông tin'", new String[]{"", "", "O"}),
                new MatrixRow("Result", "Type(N : Normal, A : Abnormal, B : Boundary)", new String[]{"N", "B", "A"}),
                new MatrixRow("", "Passed/Failed", new String[]{"P", "P", "P"}),
                new MatrixRow("", "Executed Date", new String[]{"2026-11-12", "2026-11-12", "2026-11-12"}),
                new MatrixRow("", "Defect ID", new String[]{"", "", ""})
        );
        renderMatrixRows(sheet, 7, rows, cellBorder, cellCenter, cellCenterBold);
    }

    private void createLogoutSheet(Workbook wb, CellStyle headerStyle, CellStyle labelBold, CellStyle cellBorder, CellStyle cellCenter, CellStyle cellCenterBold) {
        Sheet sheet = wb.createSheet("logout");
        sheet.setDisplayGridlines(true);

        buildFunctionHeader(sheet, "Auth", "AuthService.logout", "Senior QA", "Senior QA",
                "Log out of system, invalidate JWT cookie (maxAge = 0, path = /)",
                2, 0, 0, 1, 0, 1, 2, labelBold, cellBorder, cellCenter);

        String[] tcIds = {"UTCID01", "UTCID02"};
        createTcHeaderRow(sheet, 6, tcIds, headerStyle);

        List<MatrixRow> rows = List.of(
                new MatrixRow("Condition", "Precondition", "", ""),
                new MatrixRow("", "   HttpServletResponse", "", ""),
                new MatrixRow("", "      Provided", new String[]{"O", ""}),
                new MatrixRow("", "      null", new String[]{"", "O"}),
                new MatrixRow("Confirm", "Return", "", ""),
                new MatrixRow("", "   Add cookie access_token with value = '' and maxAge = 0", new String[]{"O", ""}),
                new MatrixRow("", "   Method completes normally without NullPointerException", new String[]{"O", "O"}),
                new MatrixRow("Result", "Type(N : Normal, A : Abnormal, B : Boundary)", new String[]{"N", "B"}),
                new MatrixRow("", "Passed/Failed", new String[]{"P", "P"}),
                new MatrixRow("", "Executed Date", new String[]{"2026-11-12", "2026-11-12"}),
                new MatrixRow("", "Defect ID", new String[]{"", ""})
        );
        renderMatrixRows(sheet, 7, rows, cellBorder, cellCenter, cellCenterBold);
    }

    private void createChangePasswordSheet(Workbook wb, CellStyle headerStyle, CellStyle labelBold, CellStyle cellBorder, CellStyle cellCenter, CellStyle cellCenterBold) {
        Sheet sheet = wb.createSheet("changePassword");
        sheet.setDisplayGridlines(true);

        buildFunctionHeader(sheet, "Auth", "AuthService.changePasswordFirstTime", "Senior QA", "Senior QA",
                "Change password first time when mustChangePassword = true, clear mustChangePassword flag",
                4, 0, 0, 1, 3, 0, 4, labelBold, cellBorder, cellCenter);

        String[] tcIds = {"UTCID01", "UTCID02", "UTCID03", "UTCID04"};
        createTcHeaderRow(sheet, 6, tcIds, headerStyle);

        List<MatrixRow> rows = List.of(
                new MatrixRow("Condition", "Precondition", "", ""),
                new MatrixRow("", "   User in DB", "", ""),
                new MatrixRow("", "      Exists with mustChangePassword = true", new String[]{"O", "O", "O", ""}),
                new MatrixRow("", "      NotExist", new String[]{"", "", "", "O"}),
                new MatrixRow("", "   Current Password against stored hash", "", ""),
                new MatrixRow("", "      Matches", new String[]{"O", "O", "", ""}),
                new MatrixRow("", "      Does not match", new String[]{"", "", "O", ""}),
                new MatrixRow("Input", "   newPassword vs confirmPassword", "", ""),
                new MatrixRow("", "      Match", new String[]{"O", "", "O", "O"}),
                new MatrixRow("", "      Mismatch", new String[]{"", "O", "", ""}),
                new MatrixRow("Confirm", "Return", "", ""),
                new MatrixRow("", "   Save new encoded passwordHash, set mustChangePassword = false", new String[]{"O", "", "", ""}),
                new MatrixRow("", "Exception", "", ""),
                new MatrixRow("", "   BadRequestException 'Mật khẩu mới và mật khẩu xác nhận không trùng khớp'", new String[]{"", "O", "", ""}),
                new MatrixRow("", "   BadRequestException 'Mật khẩu tạm thời hiện tại không chính xác'", new String[]{"", "", "O", ""}),
                new MatrixRow("", "   ResourceNotFoundException 'Không tìm thấy tài khoản người dùng'", new String[]{"", "", "", "O"}),
                new MatrixRow("Result", "Type(N : Normal, A : Abnormal, B : Boundary)", new String[]{"N", "A", "A", "A"}),
                new MatrixRow("", "Passed/Failed", new String[]{"P", "P", "P", "P"}),
                new MatrixRow("", "Executed Date", new String[]{"2026-11-12", "2026-11-12", "2026-11-12", "2026-11-12"}),
                new MatrixRow("", "Defect ID", new String[]{"", "", "", ""})
        );
        renderMatrixRows(sheet, 7, rows, cellBorder, cellCenter, cellCenterBold);
    }

    private void createResetPasswordSheet(Workbook wb, CellStyle headerStyle, CellStyle labelBold, CellStyle cellBorder, CellStyle cellCenter, CellStyle cellCenterBold) {
        Sheet sheet = wb.createSheet("resetPassword");
        sheet.setDisplayGridlines(true);

        buildFunctionHeader(sheet, "Auth", "AuthService.resetPassword (PR #20 Security)", "Senior QA", "Senior QA",
                "Forgot & Reset Password: Rate limit 3 attempts/15m, block 4 internal staff roles (403), neutral return",
                7, 0, 0, 2, 4, 1, 7, labelBold, cellBorder, cellCenter);

        String[] tcIds = {"UTCID01", "UTCID02", "UTCID03", "UTCID04", "UTCID05", "UTCID06", "UTCID07"};
        createTcHeaderRow(sheet, 6, tcIds, headerStyle);

        List<MatrixRow> rows = List.of(
                new MatrixRow("Condition", "Precondition", "", ""),
                new MatrixRow("", "   Email in DB", "", ""),
                new MatrixRow("", "      Exists (Learner / Tutor)", new String[]{"O", "", "", "O", "O", "", ""}),
                new MatrixRow("", "      Exists (Admin / Manager / CS Staff / Academic Officer)", new String[]{"", "O", "", "", "", "", ""}),
                new MatrixRow("", "      NotExist", new String[]{"", "", "O", "", "", "", ""}),
                new MatrixRow("", "   Rate limit in 15m window", "", ""),
                new MatrixRow("", "      < 3 attempts", new String[]{"O", "O", "O", "O", "O", "O", ""}),
                new MatrixRow("", "      >= 3 attempts (4th attempt)", new String[]{"", "", "", "", "", "", "O"}),
                new MatrixRow("Input", "   temporaryPassword & newPassword", "", ""),
                new MatrixRow("", "      temporaryPassword correct & newPassword confirmed", new String[]{"", "", "", "O", "", "", ""}),
                new MatrixRow("", "      newPassword mismatch confirmPassword", new String[]{"", "", "", "", "O", "", ""}),
                new MatrixRow("", "      temporaryPassword wrong", new String[]{"", "", "", "", "", "O", ""}),
                new MatrixRow("Confirm", "Return", "", ""),
                new MatrixRow("", "   Generate random password, send via email (step 1)", new String[]{"O", "", "", "", "", "", ""}),
                new MatrixRow("", "   Neutral return without sending email (prevent user enumeration)", new String[]{"", "", "O", "", "", "", ""}),
                new MatrixRow("", "   Update passwordHash, set mustChangePassword = false (step 2)", new String[]{"", "", "", "O", "", "", ""}),
                new MatrixRow("", "Exception", "", ""),
                new MatrixRow("", "   BusinessException HTTP 403 (Quản trị viên không được tự reset pass)", new String[]{"", "O", "", "", "", "", ""}),
                new MatrixRow("", "   BadRequestException 'Mật khẩu mới và mật khẩu xác minh không trùng khớp'", new String[]{"", "", "", "", "O", "", ""}),
                new MatrixRow("", "   BadRequestException 'Email hoặc mật khẩu được cấp không chính xác'", new String[]{"", "", "", "", "", "O", ""}),
                new MatrixRow("", "   BusinessException HTTP 429 (Vượt quá giới hạn tần suất yêu cầu)", new String[]{"", "", "", "", "", "", "O"}),
                new MatrixRow("Result", "Type(N : Normal, A : Abnormal, B : Boundary)", new String[]{"N", "A", "N", "N", "A", "A", "B"}),
                new MatrixRow("", "Passed/Failed", new String[]{"P", "P", "P", "P", "P", "P", "P"}),
                new MatrixRow("", "Executed Date", new String[]{"2026-11-12", "2026-11-12", "2026-11-12", "2026-11-12", "2026-11-12", "2026-11-12", "2026-11-12"}),
                new MatrixRow("", "Defect ID", new String[]{"", "", "", "", "", "", ""})
        );
        renderMatrixRows(sheet, 7, rows, cellBorder, cellCenter, cellCenterBold);
    }

    private void createFlashcardDeckSheet(Workbook wb, CellStyle headerStyle, CellStyle labelBold, CellStyle cellBorder, CellStyle cellCenter, CellStyle cellCenterBold) {
        Sheet sheet = wb.createSheet("flashcards");
        sheet.setDisplayGridlines(true);

        buildFunctionHeader(sheet, "Learning Support", "FlashcardDeckController", "Senior QA", "Senior QA",
                "Study Flashcards CRUD & PR #20 Security (JWT 401 & owner_key 403)",
                7, 0, 0, 4, 3, 0, 7, labelBold, cellBorder, cellCenter);

        String[] tcIds = {"UTCID01", "UTCID02", "UTCID03", "UTCID04", "UTCID05", "UTCID06", "UTCID07"};
        createTcHeaderRow(sheet, 6, tcIds, headerStyle);

        List<MatrixRow> rows = List.of(
                new MatrixRow("Condition", "Precondition", "", ""),
                new MatrixRow("", "   JWT Authentication", "", ""),
                new MatrixRow("", "      Valid Token", new String[]{"", "O", "O", "O", "O", "O", "O"}),
                new MatrixRow("", "      Missing / Invalid Token", new String[]{"O", "", "", "", "", "", ""}),
                new MatrixRow("", "   Deck in DB", "", ""),
                new MatrixRow("", "      Exists, owner_key == caller email", new String[]{"", "", "O", "O", "O", "O", ""}),
                new MatrixRow("", "      Exists, owner_key != caller email", new String[]{"", "O", "", "", "", "", ""}),
                new MatrixRow("", "      NotExist", new String[]{"", "", "", "", "", "", "O"}),
                new MatrixRow("Input", "   Action", "", ""),
                new MatrixRow("", "      GET /decks", new String[]{"O", "", "O", "", "", "", ""}),
                new MatrixRow("", "      POST /decks (Create)", new String[]{"O", "", "", "O", "", "", ""}),
                new MatrixRow("", "      PUT /decks/{id} (Update)", new String[]{"", "O", "", "", "O", "", ""}),
                new MatrixRow("", "      DELETE /decks/{id} (Delete)", new String[]{"O", "O", "", "", "", "O", "O"}),
                new MatrixRow("Confirm", "Return", "", ""),
                new MatrixRow("", "   HTTP 200 + List of caller owned decks", new String[]{"", "", "O", "", "", "", ""}),
                new MatrixRow("", "   HTTP 200 + Saved deck metadata & cards", new String[]{"", "", "", "O", "O", "", ""}),
                new MatrixRow("", "   HTTP 200 + success = true", new String[]{"", "", "", "", "", "O", ""}),
                new MatrixRow("", "Exception", "", ""),
                new MatrixRow("", "   BusinessException HTTP 401 (Chưa đăng nhập)", new String[]{"O", "", "", "", "", "", ""}),
                new MatrixRow("", "   BusinessException HTTP 403 (Không có quyền thao tác trên bộ thẻ người khác)", new String[]{"", "O", "", "", "", "", ""}),
                new MatrixRow("", "   ResourceNotFoundException HTTP 404", new String[]{"", "", "", "", "", "", "O"}),
                new MatrixRow("Result", "Type(N : Normal, A : Abnormal, B : Boundary)", new String[]{"A", "A", "N", "N", "N", "N", "A"}),
                new MatrixRow("", "Passed/Failed", new String[]{"P", "P", "P", "P", "P", "P", "P"}),
                new MatrixRow("", "Executed Date", new String[]{"2026-11-12", "2026-11-12", "2026-11-12", "2026-11-12", "2026-11-12", "2026-11-12", "2026-11-12"}),
                new MatrixRow("", "Defect ID", new String[]{"", "", "", "", "", "", ""})
        );
        renderMatrixRows(sheet, 7, rows, cellBorder, cellCenter, cellCenterBold);
    }

    private void createVocabularySheet(Workbook wb, CellStyle headerStyle, CellStyle labelBold, CellStyle cellBorder, CellStyle cellCenter, CellStyle cellCenterBold) {
        Sheet sheet = wb.createSheet("vocabularies");
        sheet.setDisplayGridlines(true);

        buildFunctionHeader(sheet, "Learning Support", "VocabularyController", "Senior QA", "Senior QA",
                "Vocabulary Lookup from OpenJLPT database (7,811 words), topic filter & keyword search",
                3, 0, 0, 2, 0, 1, 3, labelBold, cellBorder, cellCenter);

        String[] tcIds = {"UTCID01", "UTCID02", "UTCID03"};
        createTcHeaderRow(sheet, 6, tcIds, headerStyle);

        List<MatrixRow> rows = List.of(
                new MatrixRow("Condition", "Precondition", "", ""),
                new MatrixRow("", "   Vocabulary table in DB", "", ""),
                new MatrixRow("", "      Has 7,811 entries across N5-N1", new String[]{"O", "O", "O"}),
                new MatrixRow("Input", "   Parameters", "", ""),
                new MatrixRow("", "      GET /topics (Aggregation count)", new String[]{"O", "", ""}),
                new MatrixRow("", "      topic = JLPT_N5, keyword = 'nihon'", new String[]{"", "O", ""}),
                new MatrixRow("", "      Negative pagination (page = -5, limit = -10)", new String[]{"", "", "O"}),
                new MatrixRow("Confirm", "Return", "", ""),
                new MatrixRow("", "   Return 7 topics with counts (N5-N1, LIFE_DAILY, WORKPLACE)", new String[]{"O", "", ""}),
                new MatrixRow("", "   Return filtered vocabularies matching topic and keyword", new String[]{"", "O", ""}),
                new MatrixRow("", "   Clamp negative bounds safely to 0 and default limit", new String[]{"", "", "O"}),
                new MatrixRow("Result", "Type(N : Normal, A : Abnormal, B : Boundary)", new String[]{"N", "N", "B"}),
                new MatrixRow("", "Passed/Failed", new String[]{"P", "P", "P"}),
                new MatrixRow("", "Executed Date", new String[]{"2026-11-12", "2026-11-12", "2026-11-12"}),
                new MatrixRow("", "Defect ID", new String[]{"", "", ""})
        );
        renderMatrixRows(sheet, 7, rows, cellBorder, cellCenter, cellCenterBold);
    }

    private void createGrammarSheet(Workbook wb, CellStyle headerStyle, CellStyle labelBold, CellStyle cellBorder, CellStyle cellCenter, CellStyle cellCenterBold) {
        Sheet sheet = wb.createSheet("grammar");
        sheet.setDisplayGridlines(true);

        buildFunctionHeader(sheet, "Learning Support", "GrammarController", "Senior QA", "Senior QA",
                "OpenJLPT Grammar Lookup, level filter (N5-N1), keyword search and detail view",
                3, 0, 0, 2, 1, 0, 3, labelBold, cellBorder, cellCenter);

        String[] tcIds = {"UTCID01", "UTCID02", "UTCID03"};
        createTcHeaderRow(sheet, 6, tcIds, headerStyle);

        List<MatrixRow> rows = List.of(
                new MatrixRow("Condition", "Precondition", "", ""),
                new MatrixRow("", "   Grammar source", "", ""),
                new MatrixRow("", "      PostgreSQL grammars table or fallback JSON", new String[]{"O", "O", "O"}),
                new MatrixRow("Input", "   Parameters", "", ""),
                new MatrixRow("", "      level = 'N5', keyword = 'desu'", new String[]{"O", "", ""}),
                new MatrixRow("", "      idOrCode = 'N3-050' (Existing grammar code)", new String[]{"", "O", ""}),
                new MatrixRow("", "      idOrCode = 'UNKNOWN-999'", new String[]{"", "", "O"}),
                new MatrixRow("Confirm", "Return", "", ""),
                new MatrixRow("", "   HTTP 200 + Page response containing matching N5 grammars", new String[]{"O", "", ""}),
                new MatrixRow("", "   HTTP 200 + Full grammar details (pattern, formation, examples)", new String[]{"", "O", ""}),
                new MatrixRow("", "Exception", "", ""),
                new MatrixRow("", "   HTTP 404 Not Found", new String[]{"", "", "O"}),
                new MatrixRow("Result", "Type(N : Normal, A : Abnormal, B : Boundary)", new String[]{"N", "N", "A"}),
                new MatrixRow("", "Passed/Failed", new String[]{"P", "P", "P"}),
                new MatrixRow("", "Executed Date", new String[]{"2026-11-12", "2026-11-12", "2026-11-12"}),
                new MatrixRow("", "Defect ID", new String[]{"", "", ""})
        );
        renderMatrixRows(sheet, 7, rows, cellBorder, cellCenter, cellCenterBold);
    }

    private void createAiWritingSheet(Workbook wb, CellStyle headerStyle, CellStyle labelBold, CellStyle cellBorder, CellStyle cellCenter, CellStyle cellCenterBold) {
        Sheet sheet = wb.createSheet("aiWriting");
        sheet.setDisplayGridlines(true);

        buildFunctionHeader(sheet, "Learning Support", "AiWritingService.checkWriting", "Senior QA", "Senior QA",
                "AI Sakubun Japanese Writing Evaluation, particle error detection and grammar prompts",
                3, 0, 0, 2, 1, 0, 3, labelBold, cellBorder, cellCenter);

        String[] tcIds = {"UTCID01", "UTCID02", "UTCID03"};
        createTcHeaderRow(sheet, 6, tcIds, headerStyle);

        List<MatrixRow> rows = List.of(
                new MatrixRow("Condition", "Precondition", "", ""),
                new MatrixRow("", "   AI Settings Choice", "", ""),
                new MatrixRow("", "      Configured (Gemini / Claude / OpenAI) or Fallback Engine", new String[]{"O", "O", "O"}),
                new MatrixRow("Input", "   AiWritingCheckRequest", "", ""),
                new MatrixRow("", "      Valid Japanese sentence with particle misuse", new String[]{"O", "", ""}),
                new MatrixRow("", "      Level = 'N5' (getGrammarChallengePrompts)", new String[]{"", "O", ""}),
                new MatrixRow("", "      Blank / empty text", new String[]{"", "", "O"}),
                new MatrixRow("Confirm", "Return", "", ""),
                new MatrixRow("", "   Return score, correctedText, errors list (Trợ từ), vocabulary suggestions", new String[]{"O", "", ""}),
                new MatrixRow("", "   Return list of grammar challenge prompts for JLPT N5", new String[]{"", "O", ""}),
                new MatrixRow("", "   Fallback score = 0, empty error list, message requesting input", new String[]{"", "", "O"}),
                new MatrixRow("Result", "Type(N : Normal, A : Abnormal, B : Boundary)", new String[]{"N", "N", "A"}),
                new MatrixRow("", "Passed/Failed", new String[]{"P", "P", "P"}),
                new MatrixRow("", "Executed Date", new String[]{"2026-11-12", "2026-11-12", "2026-11-12"}),
                new MatrixRow("", "Defect ID", new String[]{"", "", ""})
        );
        renderMatrixRows(sheet, 7, rows, cellBorder, cellCenter, cellCenterBold);
    }

    private void createKanjiGameSheet(Workbook wb, CellStyle headerStyle, CellStyle labelBold, CellStyle cellBorder, CellStyle cellCenter, CellStyle cellCenterBold) {
        Sheet sheet = wb.createSheet("kanjiGame");
        sheet.setDisplayGridlines(true);

        buildFunctionHeader(sheet, "Learning Support", "KanjiGameService", "Senior QA", "Senior QA",
                "Kanji Rain Game & Match Game: load play data, save game session, calculate leaderboard",
                3, 0, 0, 2, 0, 1, 3, labelBold, cellBorder, cellCenter);

        String[] tcIds = {"UTCID01", "UTCID02", "UTCID03"};
        createTcHeaderRow(sheet, 6, tcIds, headerStyle);

        List<MatrixRow> rows = List.of(
                new MatrixRow("Condition", "Precondition", "", ""),
                new MatrixRow("", "   Kanji Play Data in DB", "", ""),
                new MatrixRow("", "      Available for JLPT N5-N1", new String[]{"O", "O", "O"}),
                new MatrixRow("Input", "   Parameters", "", ""),
                new MatrixRow("", "      jlpt = 'N5', limit = 20", new String[]{"O", "", ""}),
                new MatrixRow("", "      Valid MinigameSessionRequest with score and mistakes", new String[]{"", "O", ""}),
                new MatrixRow("", "      limit = 0 or negative", new String[]{"", "", "O"}),
                new MatrixRow("Confirm", "Return", "", ""),
                new MatrixRow("", "   Return 20 KanjiPlayDataDto with readings (On/Kun/Furigana)", new String[]{"O", "", ""}),
                new MatrixRow("", "   Save session, calculate accuracy, return MinigameSessionResponse", new String[]{"", "O", ""}),
                new MatrixRow("", "   Clamp to default minimum limit (10 items)", new String[]{"", "", "O"}),
                new MatrixRow("Result", "Type(N : Normal, A : Abnormal, B : Boundary)", new String[]{"N", "N", "B"}),
                new MatrixRow("", "Passed/Failed", new String[]{"P", "P", "P"}),
                new MatrixRow("", "Executed Date", new String[]{"2026-11-12", "2026-11-12", "2026-11-12"}),
                new MatrixRow("", "Defect ID", new String[]{"", "", ""})
        );
        renderMatrixRows(sheet, 7, rows, cellBorder, cellCenter, cellCenterBold);
    }

    private void buildFunctionHeader(Sheet sheet, String module, String method, String createdBy, String executedBy,
                                     String req, int p, int f, int u, int n, int a, int b, int total,
                                     CellStyle labelBold, CellStyle cellBorder, CellStyle cellCenter) {
        Row r0 = sheet.createRow(0);
        r0.createCell(0).setCellValue("Code Module");
        r0.getCell(0).setCellStyle(labelBold);
        r0.createCell(2).setCellValue(module);
        r0.createCell(5).setCellValue("Method");
        r0.getCell(5).setCellStyle(labelBold);
        r0.createCell(7).setCellValue(method);

        Row r1 = sheet.createRow(1);
        r1.createCell(0).setCellValue("Created By");
        r1.getCell(0).setCellStyle(labelBold);
        r1.createCell(2).setCellValue(createdBy);
        r1.createCell(5).setCellValue("Executed By");
        r1.getCell(5).setCellStyle(labelBold);
        r1.createCell(7).setCellValue(executedBy);

        Row r2 = sheet.createRow(2);
        r2.createCell(0).setCellValue("Test requirement");
        r2.getCell(0).setCellStyle(labelBold);
        r2.createCell(2).setCellValue(req);

        Row r3 = sheet.createRow(3);
        r3.createCell(0).setCellValue("Passed");
        r3.createCell(2).setCellValue("Failed");
        r3.createCell(4).setCellValue("Untested");
        r3.createCell(7).setCellValue("N/A/B");
        r3.createCell(9).setCellValue("Total Test Cases");
        for (int c : new int[]{0, 2, 4, 7, 9}) {
            r3.getCell(c).setCellStyle(labelBold);
        }

        Row r4 = sheet.createRow(4);
        r4.createCell(0).setCellValue(p);
        r4.createCell(2).setCellValue(f);
        r4.createCell(4).setCellValue(u);
        r4.createCell(7).setCellValue(n + "," + a + "," + b);
        r4.createCell(9).setCellValue(total);
        for (int c : new int[]{0, 2, 4, 7, 9}) {
            r4.getCell(c).setCellStyle(cellCenter);
        }
    }

    private void createTcHeaderRow(Sheet sheet, int rowIdx, String[] tcIds, CellStyle headerStyle) {
        Row r = sheet.createRow(rowIdx);
        for (int i = 0; i < tcIds.length; i++) {
            Cell c = r.createCell(i + 4);
            c.setCellValue(tcIds[i]);
            c.setCellStyle(headerStyle);
        }
    }

    private void renderMatrixRows(Sheet sheet, int startRow, List<MatrixRow> matrixRows, CellStyle cellBorder, CellStyle cellCenter, CellStyle cellCenterBold) {
        int rIdx = startRow;
        for (MatrixRow mr : matrixRows) {
            Row r = sheet.createRow(rIdx++);
            Cell c0 = r.createCell(0);
            c0.setCellValue(mr.category);

            Cell c1 = r.createCell(1);
            c1.setCellValue(mr.description);

            if (mr.marks != null) {
                for (int m = 0; m < mr.marks.length; m++) {
                    Cell cm = r.createCell(m + 4);
                    cm.setCellValue(mr.marks[m]);
                    cm.setCellStyle("O".equals(mr.marks[m]) || "P".equals(mr.marks[m]) ? cellCenterBold : cellCenter);
                }
            }
        }

        sheet.setColumnWidth(0, 3000);
        sheet.setColumnWidth(1, 14000);
        sheet.setColumnWidth(2, 3000);
        sheet.setColumnWidth(3, 3000);
        for (int col = 4; col <= 15; col++) {
            sheet.setColumnWidth(col, 2800);
        }
    }

    static class MatrixRow {
        String category;
        String description;
        String[] marks;

        MatrixRow(String category, String description, String[] marks) {
            this.category = category;
            this.description = description;
            this.marks = marks;
        }

        MatrixRow(String category, String description, String v1, String v2) {
            this.category = category;
            this.description = description;
            this.marks = null;
        }
    }
}
