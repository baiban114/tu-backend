package com.tu.backend.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest5_client.Rest5ClientOptions;
import co.elastic.clients.transport.rest5_client.Rest5ClientTransport;
import co.elastic.clients.transport.rest5_client.low_level.RequestOptions;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@ConditionalOnProperty(prefix = "search", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnProperty(prefix = "search", name = "elasticsearch-api-version", havingValue = "8", matchIfMissing = true)
public class SearchElasticsearchClientConfiguration {

    private static String compatibilityMediaType(String majorVersion) {
        return "application/vnd.elasticsearch+json;compatible-with=" + majorVersion;
    }

    @Bean
    @Primary
    ElasticsearchClient searchCompatibleElasticsearchClient(Rest5Client rest5Client, ObjectMapper objectMapper) {
        String mediaType = compatibilityMediaType("8");
        RequestOptions requestOptions = RequestOptions.DEFAULT.toBuilder()
            .addHeader("Accept", mediaType)
            .addHeader("Content-Type", mediaType)
            .build();
        Rest5ClientOptions options = new Rest5ClientOptions(requestOptions, false);

        ElasticsearchTransport transport = new Rest5ClientTransport(
            rest5Client,
            new JacksonJsonpMapper(objectMapper),
            options
        );
        return new ElasticsearchClient(transport);
    }
}
