package com.tu.backend.ai;

import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AiAgentProperties.class)
public class AiAgentConfiguration {

    @Bean
    ToolCallingManager aiAgentToolCallingManager() {
        return ToolCallingManager.builder().build();
    }
}
