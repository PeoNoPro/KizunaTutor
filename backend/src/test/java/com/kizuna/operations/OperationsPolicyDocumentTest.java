package com.kizuna.operations;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
class OperationsPolicyDocumentTest extends OperationsIntegrationBase {
    void insert(String state,String from,String to,String version) {
        jdbc.update("INSERT INTO policy_document_versions(document_version_id,document_type,title,version_label,approved_content,publication_state,effective_from,effective_to,source_reference,approved_at) VALUES (?,'terms','Điều khoản kiểm thử',?,'<script>không được thực thi</script>',?,?::timestamptz,?::timestamptz,'test fixture only',now())",UUID.randomUUID(),version,state,from,to);
    }
    org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor reader(String role) {
        return user("ROLE_"+role+"@test.invalid").authorities(new SimpleGrantedAuthority("ROLE_"+role));
    }
    @Test void onlyPublishedEffectiveDocumentsAreReadable() throws Exception {
        insert("DRAFT","2020-01-01T00:00:00Z",null,"draft");
        insert("PUBLISHED","2099-01-01T00:00:00Z",null,"future");
        insert("WITHDRAWN","2020-01-01T00:00:00Z",null,"withdrawn");
        mvc.perform(get("/api/policies").with(reader("LEARNER"))).andExpect(status().isOk()).andExpect(jsonPath("$.data").isEmpty());
        mvc.perform(get("/api/policies/terms").with(reader("TUTOR"))).andExpect(status().isNotFound());
        insert("PUBLISHED","2020-01-01T00:00:00Z",null,"current");
        for(String role:new String[]{"LEARNER","TUTOR"}) mvc.perform(get("/api/policies/terms").with(reader(role))).andExpect(status().isOk()).andExpect(jsonPath("$.data.versionLabel").value("current"));
        mvc.perform(get("/api/policies").with(admin())).andExpect(status().isForbidden());
        insert("PUBLISHED","2020-01-01T00:00:00Z",null,"overlap");
        mvc.perform(get("/api/policies/terms").with(reader("LEARNER"))).andExpect(status().isConflict());
    }
}
