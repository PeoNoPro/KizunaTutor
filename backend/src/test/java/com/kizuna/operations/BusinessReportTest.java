package com.kizuna.operations;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.CellType;
import org.junit.jupiter.api.*;
import java.io.ByteArrayInputStream;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

class BusinessReportTest extends OperationsIntegrationBase {
    private static final String BASE="/api/admin/operations/reports/business";
    private static final String PERIOD="?startDate=2026-03-10&endDate=2026-03-11";
    private UUID learner() {return UUID.nameUUIDFromBytes("ROLE_LEARNER".getBytes(java.nio.charset.StandardCharsets.UTF_8));}
    private UUID tutorUser() {return UUID.nameUUIDFromBytes("ROLE_TUTOR".getBytes(java.nio.charset.StandardCharsets.UTF_8));}
    @BeforeEach void schema() {
        // Chỉ dựng bảng cần cho test trong DB kiểm thử riêng; cột theo schema DatabasePgAdmin4.txt.
        jdbc.execute("DO $$ BEGIN CREATE TYPE payment_transaction_status_enum AS ENUM ('PENDING','SUCCESS','FAILED','REFUNDED'); EXCEPTION WHEN duplicate_object THEN NULL; END $$");
        jdbc.execute("DO $$ BEGIN CREATE TYPE tutor_verification_status_enum AS ENUM ('UNVERIFIED','PENDING','APPROVED','REJECTED','ADDITIONAL_INFO_REQUIRED'); EXCEPTION WHEN duplicate_object THEN NULL; END $$");
        jdbc.execute("DO $$ BEGIN CREATE TYPE tutor_credential_status_enum AS ENUM ('PENDING','VERIFIED','REJECTED'); EXCEPTION WHEN duplicate_object THEN NULL; END $$");
        jdbc.execute("CREATE TABLE IF NOT EXISTS membership_plans(plan_id uuid PRIMARY KEY,plan_name varchar(100) NOT NULL)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS payment_transactions(transaction_id uuid PRIMARY KEY,user_id uuid NOT NULL REFERENCES users(user_id),plan_id uuid REFERENCES membership_plans(plan_id) ON DELETE SET NULL,booking_id uuid,amount numeric(12,2) NOT NULL,payment_gateway varchar(50) NOT NULL DEFAULT 'VNPay',gateway_transaction_id varchar(100),status payment_transaction_status_enum NOT NULL DEFAULT 'PENDING',transaction_time timestamptz NOT NULL)");
        jdbc.execute("ALTER TABLE tutors ADD COLUMN IF NOT EXISTS highest_jlpt_level varchar(10)");
        jdbc.execute("ALTER TABLE tutors ADD COLUMN IF NOT EXISTS verification_status tutor_verification_status_enum NOT NULL DEFAULT 'UNVERIFIED'");
        jdbc.execute("ALTER TABLE tutors ADD COLUMN IF NOT EXISTS submitted_at timestamptz");
        jdbc.execute("ALTER TABLE tutors ADD COLUMN IF NOT EXISTS verified_at timestamptz");
        jdbc.execute("CREATE TABLE IF NOT EXISTS tutor_credentials(credential_id uuid PRIMARY KEY,tutor_id uuid NOT NULL REFERENCES tutors(tutor_id),verified_status tutor_credential_status_enum NOT NULL DEFAULT 'PENDING')");
        cleanup();
    }
    @AfterEach void cleanup() {
        jdbc.execute("DELETE FROM payment_transactions");jdbc.execute("DELETE FROM membership_plans");
        jdbc.execute("DELETE FROM tutor_credentials");jdbc.update("DELETE FROM tutors WHERE user_id=?",tutorUser());
    }
    private void payment(UUID plan,UUID booking,String amount,String status,String time) {
        jdbc.update("INSERT INTO payment_transactions(transaction_id,user_id,plan_id,booking_id,amount,gateway_transaction_id,status,transaction_time) VALUES (?,?,?,?,?::numeric,'gateway-secret',?::payment_transaction_status_enum,?::timestamptz)",UUID.randomUUID(),learner(),plan,booking,amount,status,time);
    }
    private UUID plan(String name) {UUID id=UUID.randomUUID();jdbc.update("INSERT INTO membership_plans(plan_id,plan_name) VALUES (?,?)",id,name);return id;}

    @Test void revenueCountsOnlySuccessfulMembershipPaymentsInVietnamPeriod() throws Exception {
        UUID standard=plan("Gói tiêu chuẩn"),premium=plan("Gói cao cấp");
        payment(standard,null,"100000","SUCCESS","2026-03-10T00:30:00+07:00");
        payment(standard,null,"100000","SUCCESS","2026-03-11T23:30:00+07:00");
        payment(premium,null,"300000","SUCCESS","2026-03-11T10:00:00+07:00");
        payment(premium,null,"300000","REFUNDED","2026-03-11T11:00:00+07:00");
        payment(premium,null,"300000","PENDING","2026-03-11T12:00:00+07:00");
        payment(premium,null,"300000","FAILED","2026-03-11T13:00:00+07:00");
        // Ngoài kỳ theo giờ Việt Nam: 17:30 UTC ngày 11 đã là ngày 12.
        payment(premium,null,"900000","SUCCESS","2026-03-11T17:30:00Z");
        payment(premium,null,"900000","SUCCESS","2026-03-09T23:59:00+07:00");
        mvc.perform(get(BASE+"/revenue"+PERIOD).with(admin())).andExpect(status().isOk())
            .andExpect(jsonPath("$.data.revenue").value(500000.0)).andExpect(jsonPath("$.data.successCount").value(3))
            .andExpect(jsonPath("$.data.refunded").doesNotExist())
            .andExpect(jsonPath("$.data.pendingCount").value(1)).andExpect(jsonPath("$.data.failedCount").value(1))
            .andExpect(jsonPath("$.data.byPlan[0].label").value("Gói cao cấp")).andExpect(jsonPath("$.data.byPlan[0].amount").value(300000.0))
            .andExpect(jsonPath("$.data.byPlan[1].count").value(2))
            .andExpect(jsonPath("$.data.daily.length()").value(2))
            .andExpect(jsonPath("$.data.daily[0].date").value("2026-03-10")).andExpect(jsonPath("$.data.daily[0].count").value(100000.0))
            .andExpect(jsonPath("$.data.daily[1].count").value(400000.0));
    }
    @Test void emptyPeriodIsZeroAndExportHasNoFile() throws Exception {
        mvc.perform(get(BASE+"/revenue"+PERIOD).with(admin())).andExpect(status().isOk()).andExpect(jsonPath("$.data.revenue").value(0)).andExpect(jsonPath("$.data.byPlan.length()").value(0));
        mvc.perform(get(BASE+"/revenue/export"+PERIOD).with(admin())).andExpect(status().isNoContent());
        mvc.perform(get(BASE+"/revenue?startDate=2026-03-11&endDate=2026-03-10").with(admin())).andExpect(status().isBadRequest());
    }
    @Test void refundedPaymentsStayOutOfRowsAndExport() throws Exception {
        var plan=plan("Gói hoàn");
        payment(plan,null,"100000","REFUNDED","2026-03-10T09:00:00+07:00");
        mvc.perform(get(BASE+"/revenue/rows"+PERIOD).with(admin())).andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(0));
        mvc.perform(get(BASE+"/revenue/export"+PERIOD).with(admin())).andExpect(status().isNoContent());
    }
    @Test void revenueExportIsRealWorkbookWithoutGatewayData() throws Exception {
        payment(plan("=1+1"),null,"100000","SUCCESS","2026-03-10T09:00:00+07:00");
        var response=mvc.perform(get(BASE+"/revenue/export"+PERIOD).with(user("ROLE_MANAGER@test.invalid"))).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        try(var book=new XSSFWorkbook(new ByteArrayInputStream(response))) {
            var sheet=book.getSheetAt(0);assertEquals(1,sheet.getLastRowNum());
            var row=sheet.getRow(1);
            assertEquals(CellType.STRING,row.getCell(3).getCellType());assertEquals("=1+1",row.getCell(3).getStringCellValue());
            assertEquals(100000d,row.getCell(4).getNumericCellValue());assertEquals("Thành công",row.getCell(6).getStringCellValue());
            for(var cell:row)assertFalse(cell.toString().contains("gateway-secret"));
        }
    }
    @Test void tutorReportUsesVietnameseLabelsAndCountsCredentials() throws Exception {
        UUID tutor=UUID.randomUUID();
        jdbc.update("INSERT INTO tutors(tutor_id,user_id,highest_jlpt_level,verification_status,submitted_at) VALUES (?,?,'N2','PENDING'::tutor_verification_status_enum,'2026-03-10T09:00:00+07:00')",tutor,tutorUser());
        jdbc.update("INSERT INTO tutor_credentials(credential_id,tutor_id,verified_status) VALUES (?,?,'VERIFIED'),(?,?,'PENDING'),(?,?,'PENDING')",UUID.randomUUID(),tutor,UUID.randomUUID(),tutor,UUID.randomUUID(),tutor);
        mvc.perform(get(BASE+"/tutors/rows").with(admin())).andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.rows[0][2]").value("N2")).andExpect(jsonPath("$.data.rows[0][3]").value("Chờ thẩm định"))
            .andExpect(jsonPath("$.data.rows[0][4]").value(1)).andExpect(jsonPath("$.data.rows[0][5]").value(2));
        var file=mvc.perform(get(BASE+"/tutors/export").with(admin())).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        try(var book=new XSSFWorkbook(new ByteArrayInputStream(file))) {assertEquals("Chờ thẩm định",book.getSheetAt(0).getRow(1).getCell(3).getStringCellValue());}
    }
    @Test void onlyAdminAndManagerCanRead() throws Exception {
        for(String path:List.of("/revenue","/revenue/rows","/revenue/export","/tutors/rows","/tutors/export")) {
            mvc.perform(get(BASE+path)).andExpect(status().isUnauthorized());
            for(String role:List.of("ROLE_CS_STAFF","ROLE_ACADEMIC_OFFICER","ROLE_TUTOR","ROLE_LEARNER"))mvc.perform(get(BASE+path).with(user(role+"@test.invalid"))).andExpect(status().isForbidden());
        }
        mvc.perform(get(BASE+"/revenue").with(user("ROLE_MANAGER@test.invalid"))).andExpect(status().isOk());
    }
}
