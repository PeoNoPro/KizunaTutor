package com.kizuna.features.admin.config;
import com.kizuna.features.admin.service.OperationsAccessPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;
@Configuration @RequiredArgsConstructor
public class OperationsWebConfig implements WebMvcConfigurer {
    private final OperationsAccessPolicy policy;
    @Override public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(policy).addPathPatterns("/api/admin/operations/**","/api/policies","/api/policies/**");
    }
}
