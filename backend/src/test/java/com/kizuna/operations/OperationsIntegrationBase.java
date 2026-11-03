package com.kizuna.operations;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.junit.jupiter.api.BeforeEach;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import java.util.UUID;

@SpringJUnitConfig(OperationsIntegrationConfig.class) @WebAppConfiguration
abstract class OperationsIntegrationBase {
    @Autowired WebApplicationContext context;
    @Autowired JdbcTemplate jdbc;
    final com.fasterxml.jackson.databind.ObjectMapper json=new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules();
    MockMvc mvc;
    static final UUID ADMIN=UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final UUID ADMIN2=UUID.fromString("00000000-0000-0000-0000-000000000002");
    @BeforeEach void prepare() {
        mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        jdbc.execute("TRUNCATE technical_incident_events,incident_components,technical_incidents,policy_document_versions");
        String[] roles={"ROLE_ADMIN","ROLE_LEARNER","ROLE_TUTOR","ROLE_MANAGER","ROLE_CS_STAFF","ROLE_ACADEMIC_OFFICER"};
        for(int i=0;i<roles.length;i++) {
            jdbc.update("INSERT INTO roles(role_id,role_name,is_active) VALUES (?,?,true) ON CONFLICT(role_id) DO UPDATE SET role_name=excluded.role_name,is_active=true",i+1,roles[i]);
            UUID id=i==0?ADMIN:UUID.nameUUIDFromBytes(roles[i].getBytes(java.nio.charset.StandardCharsets.UTF_8));
            jdbc.update("INSERT INTO users(user_id,role_id,email,full_name,password_hash) VALUES (?,?,?,?,?) ON CONFLICT(user_id) DO UPDATE SET role_id=excluded.role_id,account_status='ACTIVE',deleted_at=NULL",id,i+1,roles[i]+"@test.invalid","Người kiểm thử "+i,"not-a-login-password");
        }
        jdbc.update("INSERT INTO users(user_id,role_id,email,full_name,password_hash) VALUES (?,1,'second@test.invalid','Người kiểm thử thứ hai','unused') ON CONFLICT(user_id) DO UPDATE SET role_id=1,account_status='ACTIVE',deleted_at=NULL",ADMIN2);
    }
    org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor admin() {
        return org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("ROLE_ADMIN@test.invalid").authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"));
    }
    com.fasterxml.jackson.databind.JsonNode data(org.springframework.test.web.servlet.MvcResult result) throws Exception { return json.readTree(result.getResponse().getContentAsByteArray()).get("data"); }
}
