package com.kizuna.operations;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import com.fasterxml.jackson.databind.JsonNode;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.junit.jupiter.api.Assertions.*;
class OperationsIncidentContractTest extends OperationsIntegrationBase {
    JsonNode create() throws Exception {
        return data(mvc.perform(post("/api/admin/operations/incidents").with(admin()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"subject\":\"Mất kết nối\",\"description\":\"Cần kiểm tra kết nối\",\"severity\":\"HIGH\",\"components\":[\"DATABASE\"]}")).andExpect(status().isCreated()).andReturn());
    }
    JsonNode command(JsonNode i,String action,String extra) throws Exception {
        return data(mvc.perform(post("/api/admin/operations/incidents/"+i.get("incidentId").asText()+"/"+action).with(admin()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"expectedVersion\":"+i.get("version").asLong()+extra+"}")).andExpect(status().isOk()).andReturn());
    }
    org.apache.poi.ss.usermodel.Sheet exportSheet(String query) throws Exception {
        byte[] file=mvc.perform(get("/api/admin/operations/incidents/export"+query).with(admin())).andExpect(status().isOk())
            .andExpect(header().string("Content-Type","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")).andReturn().getResponse().getContentAsByteArray();
        return org.apache.poi.ss.usermodel.WorkbookFactory.create(new java.io.ByteArrayInputStream(file)).getSheetAt(0);
    }
    org.apache.poi.ss.usermodel.Row rowOf(org.apache.poi.ss.usermodel.Sheet sheet,JsonNode i) {
        for(var row:sheet) if(i.get("incidentId").asText().equals(row.getCell(0).getStringCellValue())) return row;
        return null;
    }
    @Test void exportContainsAllFilteredRowsAcrossPagesWithLatestEffectiveResult() throws Exception {
        String formula="=HYPERLINK(\\\"http://x.invalid\\\")";
        var first=data(mvc.perform(post("/api/admin/operations/incidents").with(admin()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"subject\":\""+formula+"\",\"description\":\"Kiểm tra công thức\",\"severity\":\"CRITICAL\",\"components\":[\"DATABASE\"]}")).andExpect(status().isCreated()).andReturn());
        var resolved=command(first,"claim","");
        resolved=command(resolved,"resolve",",\"result\":\"=1+1\",\"verification\":\"Đã kiểm tra\"");
        long versionBefore=resolved.get("version").asLong();
        var resumed=command(command(command(create(),"claim",""),"resolve",",\"result\":\"Kết quả cũ\",\"verification\":\"Đã kiểm tra\""),"resume",",\"reason\":\"Chưa ổn định\"");
        var unassigned=create();
        for(int n=0;n<55;n++) create();

        var sheet=exportSheet("");
        for(int c=0;c<7;c++) assertFalse(sheet.getRow(0).getCell(c).getStringCellValue().isBlank());
        assertEquals("Mã sự cố",sheet.getRow(0).getCell(0).getStringCellValue());
        assertEquals("Kết quả xử lý",sheet.getRow(0).getCell(6).getStringCellValue());
        assertTrue(sheet.getLastRowNum()>=58,"Phải gồm toàn bộ kết quả, không chỉ một trang");
        var row=rowOf(sheet,resolved);
        assertEquals(org.apache.poi.ss.usermodel.CellType.STRING,row.getCell(1).getCellType());
        assertEquals("=HYPERLINK(\"http://x.invalid\")",row.getCell(1).getStringCellValue());
        assertEquals("Nghiêm trọng",row.getCell(2).getStringCellValue());
        assertEquals("Đã giải quyết",row.getCell(3).getStringCellValue());
        assertEquals(org.apache.poi.ss.usermodel.CellType.STRING,row.getCell(6).getCellType());
        assertEquals("=1+1",row.getCell(6).getStringCellValue());
        assertNull(rowOf(sheet,resumed).getCell(6),"Sự cố đã mở lại không còn kết quả hiệu lực");
        assertEquals("Chưa phân công",rowOf(sheet,unassigned).getCell(4).getStringCellValue());

        var filtered=exportSheet("?severity=CRITICAL&status=RESOLVED");
        assertNotNull(rowOf(filtered,resolved));assertNull(rowOf(filtered,unassigned));
        var after=data(mvc.perform(get("/api/admin/operations/incidents/"+resolved.get("incidentId").asText()).with(admin())).andExpect(status().isOk()).andReturn());
        assertEquals(versionBefore,after.get("version").asLong(),"Xuất tệp không được sửa sự cố");

        mvc.perform(get("/api/admin/operations/incidents/export?fromDate=2000-01-01&toDate=2000-01-02").with(admin())).andExpect(status().isNoContent());
        mvc.perform(get("/api/admin/operations/incidents/export?status=SAI").with(admin())).andExpect(status().isBadRequest());
        mvc.perform(get("/api/admin/operations/incidents/export")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/operations/incidents/export").with(user("ROLE_MANAGER@test.invalid"))).andExpect(status().isForbidden());
    }
    @Test void lifecycleAndImmutableClosedHistory() throws Exception {
        var i=create(); i=command(i,"claim",""); i=command(i,"progress",",\"detail\":\"Đã kiểm tra\"");
        i=command(i,"resolve",",\"result\":\"Đã sửa\",\"verification\":\"Kiểm tra lại thành công\"");
        i=command(i,"resume",",\"reason\":\"Chưa ổn định\"");
        i=command(i,"resolve",",\"result\":\"Đã phục hồi\",\"verification\":\"Kiểm chứng độc lập\"");
        i=command(i,"close",""); assertEquals("Đã đóng",i.get("statusLabel").asText());
        assertEquals(0,i.get("allowedActions").size());
        String path="/api/admin/operations/incidents/"+i.get("incidentId").asText();
        var history=data(mvc.perform(get(path+"/events").with(admin())).andExpect(status().isOk()).andReturn());
        assertEquals(7,history.get("totalElements").asInt());
        for(int n=0;n<7;n++) assertEquals(n+1,history.get("items").get(n).get("sequenceNo").asInt());
        mvc.perform(post(path+"/resume").with(admin()).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"expectedVersion\":"+i.get("version").asLong()+",\"reason\":\"Lặp lại\"}")).andExpect(status().isConflict());
    }
    @Test void onlyOwnerWritesAndInvalidInputRejected() throws Exception {
        var i=command(create(),"claim","");
        mvc.perform(post("/api/admin/operations/incidents/"+i.get("incidentId").asText()+"/progress").with(user("second@test.invalid")).with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"expectedVersion\":"+i.get("version").asLong()+",\"detail\":\"Không được ghi\"}")).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/operations/incidents?size=51").with(admin())).andExpect(status().isBadRequest());
        mvc.perform(post("/api/admin/operations/incidents").with(admin()).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"subject\":\" \"}")).andExpect(status().isBadRequest());
    }
    @Test void commandsRejectFieldsOutsideTheirActionWithoutWritingHistory() throws Exception {
        var i=create();String path="/api/admin/operations/incidents/"+i.get("incidentId").asText();long version=i.get("version").asLong();
        for(String bad:new String[]{"claim\",\"detail\":\"Ghi nhầm","claim\",\"verification\":\"Ghi nhầm"}) {
            String[] parts=bad.split("\",",2);
            mvc.perform(post(path+"/"+parts[0]).with(admin()).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"expectedVersion\":"+version+","+parts[1]+"\"}")).andExpect(status().isBadRequest());
        }
        i=command(i,"claim","");
        mvc.perform(post(path+"/progress").with(admin()).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"expectedVersion\":"+i.get("version").asLong()+",\"detail\":\"Đúng\",\"reason\":\"Thừa\"}")).andExpect(status().isBadRequest());
        var history=data(mvc.perform(get(path+"/events").with(admin())).andExpect(status().isOk()).andReturn());
        assertEquals(2,history.get("totalElements").asInt());assertFalse(history.toString().contains("Ghi nhầm"));assertFalse(history.toString().contains("Thừa"));
    }
    @Test void unicodeLimitsAndRecurrenceRespectClosedState() throws Exception {
        String subject="𠮷".repeat(200);
        String request=json.writeValueAsString(java.util.Map.of("subject",subject,"description","Kiểm Unicode","severity","NORMAL","components",java.util.List.of("DATABASE")));
        var i=data(mvc.perform(post("/api/admin/operations/incidents").with(admin()).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(request)).andExpect(status().isCreated()).andReturn());
        String recurrence=json.writeValueAsString(java.util.Map.of("subject","Tái diễn","description","Kiểm thử","severity","NORMAL","components",java.util.List.of("DATABASE"),"recurrenceOf",i.get("incidentId").asText()));
        mvc.perform(post("/api/admin/operations/incidents").with(admin()).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(recurrence)).andExpect(status().isBadRequest());
        i=command(i,"claim","");i=command(i,"resolve",",\"result\":\"Đã sửa\",\"verification\":\"Đã kiểm tra\"");i=command(i,"close","");
        mvc.perform(post("/api/admin/operations/incidents").with(admin()).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(recurrence)).andExpect(status().isCreated());
    }
}
