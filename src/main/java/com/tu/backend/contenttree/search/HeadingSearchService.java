package com.tu.backend.contenttree.search;

import co.elastic.clients.elasticsearch.core.search.Hit;
import com.tu.backend.contenttree.search.dto.HeadingSearchHitDto;
import com.tu.backend.contenttree.search.dto.HeadingSearchResponseDto;
import com.tu.backend.search.SearchProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class HeadingSearchService {

    private final SearchProperties searchProperties;
    private final HeadingSearchElasticsearchClient headingClient;

    public HeadingSearchService(
        SearchProperties searchProperties,
        @Autowired(required = false) HeadingSearchElasticsearchClient headingClient
    ) {
        this.searchProperties = searchProperties;
        this.headingClient = headingClient;
    }

    public HeadingSearchResponseDto search(String query, String kbId, int limit) {
        if (!isActive() || query == null || query.isBlank()) {
            return new HeadingSearchResponseDto(List.of());
        }
        try {
            List<Hit<HeadingSearchDocument>> hits = headingClient.search(query.trim(), kbId, limit);
            List<HeadingSearchHitDto> items = new ArrayList<>();
            for (Hit<HeadingSearchDocument> hit : hits) {
                HeadingSearchDocument source = hit.source();
                if (source == null) {
                    continue;
                }
                items.add(new HeadingSearchHitDto(
                    source.nodeId(),
                    source.pageId(),
                    source.pageTitle(),
                    source.kbId(),
                    source.sourceBlockId(),
                    source.level(),
                    source.text(),
                    highlight(hit.highlight()),
                    source.previewText(),
                    source.estimatedHours(),
                    source.totalEstimatedHours()
                ));
            }
            return new HeadingSearchResponseDto(items);
        } catch (Exception ex) {
            return new HeadingSearchResponseDto(List.of());
        }
    }

    private String highlight(Map<String, List<String>> highlight) {
        if (highlight == null || highlight.isEmpty()) {
            return null;
        }
        for (List<String> values : highlight.values()) {
            if (values != null && !values.isEmpty()) {
                return values.get(0);
            }
        }
        return null;
    }

    private boolean isActive() {
        return searchProperties.isEnabled() && headingClient != null;
    }
}
