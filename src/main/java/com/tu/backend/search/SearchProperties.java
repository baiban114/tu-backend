package com.tu.backend.search;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "search")
public class SearchProperties {

    private boolean enabled = true;
    private String index = "tu_pages";
    /**
     * REST API compatibility version sent to Elasticsearch.
     * Use "8" when the server runs Elasticsearch 8.x (default for docker-compose.infra.yml).
     * Set to "9" when the server runs Elasticsearch 9.x to use the client's native headers.
     */
    private String elasticsearchApiVersion = "8";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    public String getElasticsearchApiVersion() {
        return elasticsearchApiVersion;
    }

    public void setElasticsearchApiVersion(String elasticsearchApiVersion) {
        this.elasticsearchApiVersion = elasticsearchApiVersion;
    }
}
