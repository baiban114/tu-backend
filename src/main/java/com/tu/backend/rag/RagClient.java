package com.tu.backend.rag;

import com.tu.backend.common.BusinessException;
import com.tu.backend.rag.dto.RagDeleteRequest;
import com.tu.backend.rag.dto.RagIndexRequest;
import com.tu.backend.rag.dto.RagQueryRequest;
import com.tu.backend.rag.dto.RagQueryResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class RagClient {

    private final RagProperties properties;
    private final RestClient.Builder restClientBuilder;

    public RagClient(RagProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClientBuilder = restClientBuilder;
    }

    public RagQueryResponse query(RagQueryRequest request) {
        return post("/internal/rag/query", request, RagQueryResponse.class);
    }

    public void index(RagIndexRequest request) {
        post("/internal/rag/index", request, Void.class);
    }

    public void delete(RagDeleteRequest request) {
        post("/internal/rag/delete", request, Void.class);
    }

    private <T> T post(String path, Object body, Class<T> responseType) {
        if (!properties.isEnabled()) {
            throw new BusinessException(50301, "rag service disabled");
        }
        try {
            RestClient client = restClientBuilder.baseUrl(properties.getServiceUrl()).build();
            return client.post()
                .uri(path)
                .body(body)
                .retrieve()
                .body(responseType);
        } catch (RestClientException ex) {
            throw new BusinessException(50300, "rag service unavailable");
        }
    }
}
