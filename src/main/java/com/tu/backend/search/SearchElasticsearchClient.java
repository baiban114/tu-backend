package com.tu.backend.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HighlightField;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import co.elastic.clients.util.NamedValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "search.enabled", havingValue = "true", matchIfMissing = true)
public class SearchElasticsearchClient {

    private static final Logger log = LoggerFactory.getLogger(SearchElasticsearchClient.class);

    private final ElasticsearchClient client;
    private final SearchProperties properties;

    public SearchElasticsearchClient(ElasticsearchClient client, SearchProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    public void ensureIndex() throws IOException {
        if (!properties.isEnabled()) {
            return;
        }
        String index = properties.getIndex();
        BooleanResponse exists = client.indices().exists(e -> e.index(index));
        if (exists.value()) {
            return;
        }
        client.indices().create(c -> c
            .index(index)
            .mappings(m -> m.properties(Map.of(
                "kbId", keywordProperty(),
                "kbName", textProperty(),
                "pageId", keywordProperty(),
                "pageTitle", textProperty(),
                "blockId", keywordProperty(),
                "blockType", keywordProperty(),
                "title", textProperty(),
                "body", textProperty(),
                "updatedAt", keywordProperty()
            )))
        );
        log.info("Created Elasticsearch index {}", index);
    }

    public void bulkIndex(List<SearchDocument> documents) throws IOException {
        if (!properties.isEnabled() || documents.isEmpty()) {
            return;
        }
        String index = properties.getIndex();
        BulkRequest.Builder bulk = new BulkRequest.Builder();
        for (SearchDocument document : documents) {
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
            .index(properties.getIndex())
            .query(q -> q.term(t -> t.field("pageId").value(pageId)))
        );
    }

    public void deleteByKbId(String kbId) throws IOException {
        if (!properties.isEnabled()) {
            return;
        }
        client.deleteByQuery(d -> d
            .index(properties.getIndex())
            .query(q -> q.term(t -> t.field("kbId").value(kbId)))
        );
    }

    public SearchResponse<SearchDocument> search(String query, int limit) throws IOException {
        return client.search(s -> s
            .index(properties.getIndex())
            .size(Math.clamp(limit, 1, 50))
            .query(q -> q.multiMatch(m -> m
                .query(query)
                .fields("title^3", "pageTitle^2", "body")
            ))
            .highlight(h -> h
                .fields(
                    NamedValue.of("body", HighlightField.of(hf -> hf
                        .preTags("<em>")
                        .postTags("</em>")
                        .fragmentSize(120)
                        .numberOfFragments(1)
                    )),
                    NamedValue.of("pageTitle", HighlightField.of(hf -> hf
                        .preTags("<em>")
                        .postTags("</em>")
                        .fragmentSize(80)
                        .numberOfFragments(1)
                    )),
                    NamedValue.of("title", HighlightField.of(hf -> hf
                        .preTags("<em>")
                        .postTags("</em>")
                        .fragmentSize(80)
                        .numberOfFragments(1)
                    ))
                )
            ),
            SearchDocument.class
        );
    }

    public List<Hit<SearchDocument>> parseHits(SearchResponse<SearchDocument> response) {
        if (response.hits() == null || response.hits().hits() == null) {
            return List.of();
        }
        return new ArrayList<>(response.hits().hits());
    }

    private Property keywordProperty() {
        return Property.of(p -> p.keyword(k -> k));
    }

    private Property textProperty() {
        return Property.of(p -> p.text(t -> t));
    }
}
