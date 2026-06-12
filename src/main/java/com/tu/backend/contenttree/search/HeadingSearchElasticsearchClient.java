package com.tu.backend.contenttree.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HighlightField;
import co.elastic.clients.util.NamedValue;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import com.tu.backend.search.SearchProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "search.enabled", havingValue = "true", matchIfMissing = true)
public class HeadingSearchElasticsearchClient {

    private static final Logger log = LoggerFactory.getLogger(HeadingSearchElasticsearchClient.class);

    private final ElasticsearchClient client;
    private final SearchProperties properties;

    public HeadingSearchElasticsearchClient(ElasticsearchClient client, SearchProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    public String headingsIndex() {
        return properties.getHeadingsIndex();
    }

    public void ensureIndex() throws IOException {
        if (!properties.isEnabled()) {
            return;
        }
        String index = headingsIndex();
        BooleanResponse exists = client.indices().exists(e -> e.index(index));
        if (exists.value()) {
            return;
        }
        client.indices().create(c -> c
            .index(index)
            .mappings(m -> m.properties(headingIndexProperties()))
        );
        log.info("Created Elasticsearch headings index {}", index);
    }

    private Map<String, Property> headingIndexProperties() {
        Map<String, Property> mapping = new HashMap<>();
        mapping.put("kbId", keywordProperty());
        mapping.put("pageId", keywordProperty());
        mapping.put("pageTitle", textProperty());
        mapping.put("nodeId", keywordProperty());
        mapping.put("sourceBlockId", keywordProperty());
        mapping.put("level", integerProperty());
        mapping.put("sortOrder", integerProperty());
        mapping.put("text", textProperty());
        mapping.put("previewText", textProperty());
        mapping.put("sourceType", keywordProperty());
        mapping.put("estimatedHours", doubleProperty());
        mapping.put("totalEstimatedHours", doubleProperty());
        mapping.put("updatedAt", keywordProperty());
        return mapping;
    }

    public void bulkIndex(List<HeadingSearchDocument> documents) throws IOException {
        if (!properties.isEnabled() || documents.isEmpty()) {
            return;
        }
        String index = headingsIndex();
        BulkRequest.Builder bulk = new BulkRequest.Builder();
        for (HeadingSearchDocument document : documents) {
            bulk.operations(op -> op.index(idx -> idx
                .index(index)
                .id(document.id())
                .document(document)
            ));
        }
        client.bulk(bulk.build());
    }

    public void deleteByPageId(String pageId) throws IOException {
        if (!properties.isEnabled()) {
            return;
        }
        client.deleteByQuery(d -> d
            .index(headingsIndex())
            .query(q -> q.term(t -> t.field("pageId").value(pageId)))
        );
    }

    public List<Hit<HeadingSearchDocument>> search(String query, String kbId, int limit) throws IOException {
        if (!properties.isEnabled()) {
            return List.of();
        }
        int safeLimit = Math.max(1, Math.min(limit, 100));
        SearchResponse<HeadingSearchDocument> response = client.search(s -> s
            .index(headingsIndex())
            .size(safeLimit)
            .query(q -> q.bool(b -> {
                b.must(m -> m.multiMatch(mm -> mm
                    .query(query)
                    .fields("text^3", "previewText", "pageTitle")
                ));
                if (kbId != null && !kbId.isBlank()) {
                    b.filter(f -> f.term(t -> t.field("kbId").value(kbId)));
                }
                return b;
            }))
            .highlight(h -> h.fields(
                NamedValue.of("text", highlightField()),
                NamedValue.of("previewText", highlightField())
            )),
            HeadingSearchDocument.class
        );
        return response.hits().hits();
    }

    private HighlightField highlightField() {
        return HighlightField.of(h -> h);
    }

    private Property keywordProperty() {
        return Property.of(p -> p.keyword(k -> k));
    }

    private Property textProperty() {
        return Property.of(p -> p.text(t -> t));
    }

    private Property integerProperty() {
        return Property.of(p -> p.integer(i -> i));
    }

    private Property doubleProperty() {
        return Property.of(p -> p.double_(d -> d));
    }
}
