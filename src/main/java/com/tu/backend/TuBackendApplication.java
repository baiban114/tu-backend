package com.tu.backend;

import com.tu.backend.index.IndexProperties;
import com.tu.backend.rag.RagProperties;
import com.tu.backend.search.SearchProperties;
import com.tu.backend.secret.SecretProperties;
import com.tu.backend.taskintegration.IntegrationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
@EnableConfigurationProperties({RagProperties.class, IntegrationProperties.class, SecretProperties.class, SearchProperties.class, IndexProperties.class})
public class TuBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(TuBackendApplication.class, args);
    }
}
