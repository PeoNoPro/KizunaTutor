package com.kizuna.operations;
import org.junit.jupiter.api.Test;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class OperationsSplitSecurityTest extends OperationsIntegrationBase {
    @Test void credentialedCorsIsLimitedToFrontendOrigin() throws Exception {
        mvc.perform(options("/api/admin/operations/booking-policy").header("Origin","http://localhost:3000")
            .header("Access-Control-Request-Method","PUT").header("Access-Control-Request-Headers","content-type,x-csrf-token"))
            .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin","http://localhost:3000"))
            .andExpect(header().string("Access-Control-Allow-Credentials","true"));
        mvc.perform(options("/api/admin/operations/booking-policy").header("Origin","https://other.invalid")
            .header("Access-Control-Request-Method","PUT")).andExpect(status().isForbidden());
    }
    @Test void authenticatedClientCanObtainCsrfWithoutAnSsrPage() throws Exception {
        mvc.perform(get("/api/admin/operations/csrf").with(admin())).andExpect(status().isOk())
            .andExpect(jsonPath("$.data.token").isNotEmpty()).andExpect(jsonPath("$.data.headerName").value("X-CSRF-TOKEN"));
        mvc.perform(get("/api/admin/operations/csrf")).andExpect(status().isUnauthorized());
    }
}
