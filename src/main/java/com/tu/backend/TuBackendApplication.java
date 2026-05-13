package com.tu.backend;

import com.tu.backend.rag.RagProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(RagProperties.class)
public class TuBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(TuBackendApplication.class, args);
    }
}
