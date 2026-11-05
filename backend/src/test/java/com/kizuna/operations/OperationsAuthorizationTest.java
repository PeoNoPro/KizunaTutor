package com.kizuna.operations;
import org.junit.jupiter.api.Test;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import org.springframework.http.MediaType;
class OperationsAuthorizationTest extends OperationsIntegrationBase {
    @Test void rolesAndCurrentAccountAreEnforced() throws Exception {
        mvc.perform(get("/api/admin/operations/dashboard/accounts")).andExpect(status().isUnauthorized());
        for(String role:new String[]{"LEARNER","TUTOR","MANAGER","CS_STAFF","ACADEMIC_OFFICER"}) {
            mvc.perform(get("/api/admin/operations/dashboard/accounts").with(user("ROLE_"+role+"@test.invalid").authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_"+role)))).andExpect(status().isForbidden());
        }
        mvc.perform(get("/api/admin/operations/dashboard/accounts").with(admin())).andExpect(status().isOk());
        jdbc.update("UPDATE users SET role_id=2 WHERE user_id=?",ADMIN);
        mvc.perform(get("/api/admin/operations/dashboard/accounts").with(admin())).andExpect(status().isForbidden());
        jdbc.update("UPDATE users SET role_id=1,deleted_at=now() WHERE user_id=?",ADMIN);
        mvc.perform(get("/api/admin/operations/dashboard/accounts").with(admin())).andExpect(status().isForbidden());
        jdbc.update("UPDATE users SET deleted_at=NULL,account_status='SUSPENDED' WHERE user_id=?",ADMIN);
        mvc.perform(get("/api/admin/operations/dashboard/accounts").with(admin())).andExpect(status().isForbidden());
    }
    @Test void writesRequireCsrfAndOtherFeaturesRetainTheirPolicy() throws Exception {
        String body="{\"subject\":\"Lỗi kiểm thử\",\"description\":\"Mô tả\",\"severity\":\"NORMAL\",\"components\":[\"DATABASE\"]}";
        mvc.perform(post("/api/admin/operations/incidents").with(admin()).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/operations/incidents").with(admin()).with(csrf().useInvalidToken()).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/operations/incidents").with(admin()).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated());
        mvc.perform(post("/api/auth/unmapped-fe11-regression").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isNotFound());
    }
}
