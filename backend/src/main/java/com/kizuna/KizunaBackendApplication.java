package com.kizuna;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class KizunaBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(KizunaBackendApplication.class, args);
    }

}
