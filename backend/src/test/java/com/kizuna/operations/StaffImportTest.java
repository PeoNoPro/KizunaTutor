package com.kizuna.operations;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

class StaffImportTest extends OperationsIntegrationBase {
    private static final String BASE="/api/admin/operations/staff/import";
    @org.springframework.beans.factory.annotation.Autowired org.springframework.mail.javamail.JavaMailSender mail;
    private static final String ACADEMIC="Cán bộ học vụ",SERVICE="Chăm sóc khách hàng";
    @BeforeEach void schema() throws Exception {jdbc.execute(Files.readString(Path.of("../database/seeds/fe11_operations_schema.sql")));cleanup();}
    @AfterEach void cleanup() {
        jdbc.execute("ALTER TABLE staff_account_audit_events DROP CONSTRAINT IF EXISTS staff_test_audit_failure");
        jdbc.update("DELETE FROM staff_account_audit_events WHERE target_user_id IN (SELECT user_id FROM users WHERE email LIKE 'import-%@test.invalid')");
        jdbc.update("DELETE FROM users WHERE email LIKE 'import-%@test.invalid'");
    }
    private byte[] workbook(String[] header,String[]... rows) throws IOException {
        try(var book=new XSSFWorkbook();var out=new ByteArrayOutputStream()) {
            var sheet=book.createSheet("Nhân viên");var head=sheet.createRow(0);
            for(int c=0;c<header.length;c++)head.createCell(c).setCellValue(header[c]);
            for(int n=0;n<rows.length;n++){var row=sheet.createRow(n+1);for(int c=0;c<rows[n].length;c++)row.createCell(c).setCellValue(rows[n][c]);}
            book.write(out);return out.toByteArray();
        }
    }
    private static final String[] HEADER={"Họ và tên","Email","Vai trò","Số điện thoại"};
    private org.springframework.test.web.servlet.ResultActions upload(byte[] bytes,String name) throws Exception {
        return mvc.perform(multipart(BASE+"/preview").file(new MockMultipartFile("file",name,"application/octet-stream",bytes)).with(admin()).with(csrf()));
    }
    private com.fasterxml.jackson.databind.JsonNode preview(String[]... rows) throws Exception {return data(upload(workbook(HEADER,rows),"nhan-vien.xlsx").andExpect(status().isOk()).andReturn());}
    private long imported() {return jdbc.queryForObject("SELECT count(*) FROM users WHERE email LIKE 'import-%@test.invalid'",Long.class);}
    private org.springframework.test.web.servlet.ResultActions confirm(com.fasterxml.jackson.databind.JsonNode rows) throws Exception {
        return mvc.perform(post(BASE+"/confirm").with(admin()).with(csrf()).contentType("application/json").content(json.writeValueAsString(Map.of("rows",rows))));
    }

    @Test void templateOpensWithExactHeadersAndContainsNoData() throws Exception {
        byte[] file=mvc.perform(get(BASE+"/template").with(admin())).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        try(var book=new XSSFWorkbook(new ByteArrayInputStream(file))) {
            var sheet=book.getSheetAt(0);
            for(int c=0;c<HEADER.length;c++)assertEquals(HEADER[c],sheet.getRow(0).getCell(c).getStringCellValue());
            assertEquals(0,sheet.getLastRowNum());
            assertEquals(1,sheet.getDataValidations().size());
        }
        upload(file,"mau.xlsx").andExpect(status().isBadRequest());
    }
    @Test void previewReportsEveryProblemByRowWithoutWriting() throws Exception {
        var data=preview(new String[]{"Hợp lệ","Import-OK@test.invalid",ACADEMIC,"0901234567"},new String[]{"","sai-email",ACADEMIC},new String[]{"Trùng tệp","import-ok@test.invalid",SERVICE},
            new String[]{"Đã có","ROLE_ADMIN@test.invalid",SERVICE},new String[]{"Vai trò cao","import-manager@test.invalid","Quản lý vận hành"},new String[]{"Vai trò Admin","import-admin@test.invalid","Quản trị viên hệ thống"});
        assertEquals(6,data.get("total").asInt());assertEquals(5,data.get("errorCount").asInt());
        var rows=data.get("rows");
        assertEquals(2,rows.get(0).get("rowNumber").asInt());assertEquals("import-ok@test.invalid",rows.get(0).get("email").asText());assertEquals(0,rows.get(0).get("errors").size());
        assertEquals(3,rows.get(1).get("rowNumber").asInt());assertTrue(rows.get(1).get("errors").size()>=2);
        assertEquals("Email trùng với dòng 2 trong tệp.",rows.get(2).get("errors").get(0).get("message").asText());
        assertEquals("Email này đã được sử dụng.",rows.get(3).get("errors").get(0).get("message").asText());
        assertEquals("role",rows.get(4).get("errors").get(0).get("field").asText());assertEquals("role",rows.get(5).get("errors").get(0).get("field").asText());
        assertEquals(0,imported());
    }
    @Test void emailOfLockedStaffPointsAdminToUnlockInsteadOfCreating() throws Exception {
        jdbc.update("INSERT INTO users(user_id,role_id,email,full_name,password_hash,account_status) VALUES (?,5,'import-locked@test.invalid','Nhân viên đang khóa','unused','SUSPENDED')",UUID.randomUUID());
        String message="Email này thuộc tài khoản nhân viên đang bị khóa. Hãy mở lại tài khoản đó thay vì tạo mới.";
        var rows=preview(new String[]{"Tạo lại","IMPORT-LOCKED@test.invalid",ACADEMIC},new String[]{"Đã có","ROLE_ADMIN@test.invalid",ACADEMIC}).get("rows");
        assertEquals(message,rows.get(0).get("errors").get(0).get("message").asText());
        assertEquals("Email này đã được sử dụng.",rows.get(1).get("errors").get(0).get("message").asText());
        mvc.perform(post("/api/admin/operations/staff").with(admin()).with(csrf()).contentType("application/json").content(json.writeValueAsString(Map.of("fullName","Tạo lại","email","import-locked@test.invalid","roleId",5))))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(message));
        assertEquals(1,imported());
    }
    @Test void rejectsFilesThatAreNotUsable() throws Exception {
        upload("không phải excel".getBytes(java.nio.charset.StandardCharsets.UTF_8),"tep.xlsx").andExpect(status().isBadRequest());
        upload(workbook(HEADER,new String[]{"A","import-a@test.invalid",ACADEMIC}),"tep.xls").andExpect(status().isBadRequest());
        upload(workbook(new String[]{"Tên","Email","Vai trò","SĐT"},new String[]{"A","import-a@test.invalid",ACADEMIC}),"tep.xlsx").andExpect(status().isBadRequest());
        upload(new byte[2_000_001],"tep.xlsx").andExpect(status().isBadRequest());
        var many=new String[101][];for(int n=0;n<101;n++)many[n]=new String[]{"A","import-"+n+"@test.invalid",ACADEMIC};
        upload(workbook(HEADER,many),"tep.xlsx").andExpect(status().isBadRequest());
        try(var book=new XSSFWorkbook();var out=new ByteArrayOutputStream()) {
            var sheet=book.createSheet();var head=sheet.createRow(0);for(int c=0;c<4;c++)head.createCell(c).setCellValue(HEADER[c]);
            sheet.createRow(1).createCell(0).setCellFormula("1+1");book.write(out);
            upload(out.toByteArray(),"tep.xlsx").andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Dòng 2 có công thức. Vui lòng nhập giá trị trực tiếp."));
        }
        mvc.perform(multipart(BASE+"/preview").with(admin()).with(csrf())).andExpect(status().isBadRequest());
        assertEquals(0,imported());
    }
    @Test void confirmCreatesAllRowsAndEmailsEachPersonOwnPassword() throws Exception {
        org.mockito.Mockito.clearInvocations(mail);
        var rows=preview(new String[]{"Nguyễn Văn A","import-a@test.invalid",ACADEMIC,"0901234567"},new String[]{"Trần Thị B","IMPORT-B@test.invalid",SERVICE}).get("rows");
        String response=confirm(rows).andExpect(status().isCreated()).andExpect(jsonPath("$.data.created").value(2)).andReturn().getResponse().getContentAsString();
        assertEquals(2,imported());
        var captor=org.mockito.ArgumentCaptor.forClass(jakarta.mail.internet.MimeMessage.class);
        org.mockito.Mockito.verify(mail,org.mockito.Mockito.timeout(5000).atLeast(2)).send(captor.capture());
        var passwords=new HashMap<String,String>();
        for(var message:captor.getAllValues()){if(!message.getAllRecipients()[0].toString().startsWith("import-"))continue;var matcher=java.util.regex.Pattern.compile("data-temporary-password>([^<]+)<").matcher(message.getContent().toString());assertTrue(matcher.find());passwords.put(message.getAllRecipients()[0].toString(),org.springframework.web.util.HtmlUtils.htmlUnescape(matcher.group(1)));}
        assertNotEquals(passwords.get("import-a@test.invalid"),passwords.get("import-b@test.invalid"),"Mỗi người phải có mật khẩu riêng");
        assertFalse(response.contains(passwords.get("import-a@test.invalid")));assertFalse(response.contains("$2a$"));
        var user=jdbc.queryForMap("SELECT u.password_hash,u.must_change_password,u.phone,r.role_name FROM users u JOIN roles r USING(role_id) WHERE u.email='import-a@test.invalid'");
        assertTrue(new BCryptPasswordEncoder().matches(passwords.get("import-a@test.invalid"),(String)user.get("password_hash")));assertEquals(true,user.get("must_change_password"));assertEquals("0901234567",user.get("phone"));assertEquals("ROLE_ACADEMIC_OFFICER",user.get("role_name"));
        assertEquals("ROLE_CS_STAFF",jdbc.queryForObject("SELECT r.role_name FROM users u JOIN roles r USING(role_id) WHERE u.email='import-b@test.invalid'",String.class));
        assertEquals(2,jdbc.queryForObject("SELECT count(*) FROM staff_account_audit_events e JOIN users u ON u.user_id=e.target_user_id WHERE u.email LIKE 'import-%@test.invalid' AND e.action='CREATE' AND e.actor_user_id=?",Integer.class,ADMIN));
    }
    @Test void fullBatchOfHundredRowsFitsTransactionLimit() throws Exception {
        var many=new String[100][];for(int n=0;n<100;n++)many[n]=new String[]{"Nhân viên "+n,"import-full-"+n+"@test.invalid",n%2==0?ACADEMIC:SERVICE};
        var rows=preview(many).get("rows");
        long start=System.nanoTime();
        confirm(rows).andExpect(status().isCreated()).andExpect(jsonPath("$.data.created").value(100));
        long millis=(System.nanoTime()-start)/1_000_000;
        System.out.println("FE11-IMPORT-100-MS="+millis);
        assertEquals(100,imported());assertTrue(millis<15000,"Lô đủ phải xong trong giới hạn transaction, thực tế "+millis+" ms");
    }
    @Test void confirmCreatesNothingWhenDataChangedOrInvalid() throws Exception {
        var rows=preview(new String[]{"Một","import-1@test.invalid",ACADEMIC},new String[]{"Hai","import-2@test.invalid",SERVICE}).get("rows");
        jdbc.update("INSERT INTO users(user_id,role_id,email,full_name,password_hash) VALUES (?,6,'import-2@test.invalid','Vừa tạo','unused')",UUID.randomUUID());
        org.mockito.Mockito.clearInvocations(mail);
        confirm(rows).andExpect(status().isConflict()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Dòng 3")));
        assertEquals(1,imported(),"Dòng hợp lệ cũng không được tạo");
        var tampered=json.readTree("[{\"rowNumber\":2,\"fullName\":\"Nâng quyền\",\"email\":\"import-x@test.invalid\",\"role\":\"Quản trị viên hệ thống\"}]");
        confirm(tampered).andExpect(status().isConflict());
        assertEquals(1,imported());

    }
    @Test void auditFailureRollsBackWholeBatch() throws Exception {
        var rows=preview(new String[]{"Một","import-1@test.invalid",ACADEMIC},new String[]{"Hai","import-2@test.invalid",SERVICE}).get("rows");
        jdbc.execute("ALTER TABLE staff_account_audit_events ADD CONSTRAINT staff_test_audit_failure CHECK (reason <> 'Nhập tài khoản từ tệp Excel') NOT VALID");
        org.mockito.Mockito.clearInvocations(mail);
        confirm(rows).andExpect(status().isServiceUnavailable());
        assertEquals(0,imported());

    }
    @Test void onlyCurrentAdminWithCsrfCanImport() throws Exception {
        var manager=user("ROLE_MANAGER@test.invalid");var file=new MockMultipartFile("file","tep.xlsx","application/octet-stream",workbook(HEADER,new String[]{"A","import-a@test.invalid",ACADEMIC}));
        var body=json.writeValueAsString(Map.of("rows",List.of(Map.of("rowNumber",2,"fullName","A","email","import-a@test.invalid","role",ACADEMIC))));
        mvc.perform(get(BASE+"/template")).andExpect(status().isUnauthorized());
        mvc.perform(get(BASE+"/template").with(manager)).andExpect(status().isForbidden());
        mvc.perform(multipart(BASE+"/preview").file(file).with(manager).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(post(BASE+"/confirm").with(manager).with(csrf()).contentType("application/json").content(body)).andExpect(status().isForbidden());
        mvc.perform(multipart(BASE+"/preview").file(file).with(admin())).andExpect(status().isForbidden());
        mvc.perform(post(BASE+"/confirm").with(admin()).contentType("application/json").content(body)).andExpect(status().isForbidden());
        assertEquals(0,imported());
    }
}
