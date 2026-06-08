package com.tu.backend.search;

import co.elastic.clients.elasticsearch.core.search.Hit;
import com.tu.backend.search.dto.SearchHitDto;
import com.tu.backend.search.dto.SearchResponseDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class SearchService {

    private final SearchElasticsearchClient searchClient;
    private final SearchProperties searchProperties;

    public SearchService(
        @Autowired(required = false) SearchElasticsearchClient searchClient,
        SearchProperties searchProperties
    ) {
        this.searchClient = searchClient;
        this.searchProperties = searchProperties;
    }

    public SearchResponseDto search(String query, int limit) {
        if (!searchProperties.isEnabled()) {
            return new SearchResponseDto(List.of(), false, "search disabled");
        }

        String trimmed = query == null ? "" : query.trim();
        if (trimmed.length() < 2) {
            return new SearchResponseDto(List.of(), true, null);
        }

        if (searchClient == null) {
            return new SearchResponseDto(List.of(), true, "search unavailable");
        }

        try {
            var response = searchClient.search(trimmed, limit);
            List<SearchHitDto> hits = new ArrayList<>();
            for (Hit<SearchDocument> hit : searchClient.parseHits(response)) {
                SearchDocument source = hit.source();
                if (source == null) {
                    continue;
                }
                hits.add(new SearchHitDto(
                    source.kbId(),
                    source.kbName(),
                    source.pageId(),
                    source.pageTitle(),
                    source.blockId(),
                    source.blockType(),
                    source.title(),
                    buildSnippet(source, hit.highlight())
                ));
            }
            return new SearchResponseDto(hits, true, null);
        } catch (IOException ex) {
            return new SearchResponseDto(List.of(), true, "search unavailable");
        }
    }

    private String buildSnippet(SearchDocument source, Map<String, List<String>> highlight) {
        if (highlight != null) {
            for (String field : List.of("body", "pageTitle", "title")) {
                List<String> fragments = highlight.get(field);
                if (fragments != null && !fragments.isEmpty() && fragments.get(0) != null && !fragments.get(0).isBlank()) {
                    return fragments.get(0);
                }
            }
        }
        if (source.body() != null && !source.body().isBlank()) {
            return truncate(source.body(), 120);
        }
        return source.pageTitle() != null ? source.pageTitle() : source.title();
    }

    private String truncate(String text, int maxLen) {
        String normalized = text.replace('\n', ' ').trim();
        if (normalized.length() <= maxLen) {
            return normalized;
        }
        return normalized.substring(0, maxLen) + "…";
    }
}
