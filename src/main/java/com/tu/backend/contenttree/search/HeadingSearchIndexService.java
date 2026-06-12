package com.tu.backend.contenttree.search;

import com.tu.backend.contenttree.ContentTreeHoursRollup;
import com.tu.backend.contenttree.dto.ContentTreeNodeDto;
import com.tu.backend.contenttree.entity.ScopeType;
import com.tu.backend.contenttree.repository.ContentTreeNodeRepository;
import com.tu.backend.page.entity.PageEntity;
import com.tu.backend.page.repository.PageRepository;
import com.tu.backend.search.SearchProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class HeadingSearchIndexService {

    private static final Logger log = LoggerFactory.getLogger(HeadingSearchIndexService.class);

    private final ContentTreeNodeRepository nodeRepository;
    private final PageRepository pageRepository;
    private final SearchProperties searchProperties;
    private final HeadingSearchElasticsearchClient headingClient;

    public HeadingSearchIndexService(
        ContentTreeNodeRepository nodeRepository,
        PageRepository pageRepository,
        SearchProperties searchProperties,
        @Autowired(required = false) HeadingSearchElasticsearchClient headingClient
    ) {
        this.nodeRepository = nodeRepository;
        this.pageRepository = pageRepository;
        this.searchProperties = searchProperties;
        this.headingClient = headingClient;
    }

    public void indexPageBestEffort(String pageId) {
        if (!isActive() || pageId == null || pageId.isBlank()) {
            return;
        }
        try {
            PageEntity page = pageRepository.findById(pageId).orElse(null);
            if (page == null) {
                return;
            }
            var nodes = nodeRepository.findByScopeTypeAndScopeIdOrderBySortOrderAscCreatedAtAsc(ScopeType.PAGE, pageId);
            List<ContentTreeNodeDto> rolledUp = ContentTreeHoursRollup.withRollup(nodes);
            headingClient.deleteByPageId(pageId);
            List<HeadingSearchDocument> documents = new ArrayList<>();
            for (ContentTreeNodeDto node : rolledUp) {
                documents.add(new HeadingSearchDocument(
                    pageId + ":" + node.id(),
                    page.getKbId(),
                    pageId,
                    page.getTitle(),
                    node.id(),
                    node.sourceBlockId(),
                    node.level(),
                    node.sortOrder(),
                    node.title(),
                    node.previewText(),
                    node.sourceType(),
                    node.estimatedHours(),
                    node.totalEstimatedHours(),
                    page.getUpdatedAt() == null ? null : page.getUpdatedAt().toString()
                ));
            }
            headingClient.bulkIndex(documents);
        } catch (Exception ex) {
            log.warn("failed to index headings for page {}", pageId, ex);
        }
    }

    public void deletePagesBestEffort(List<String> pageIds) {
        if (!isActive() || pageIds == null) {
            return;
        }
        for (String pageId : pageIds) {
            try {
                headingClient.deleteByPageId(pageId);
            } catch (Exception ex) {
                log.warn("failed to delete heading index for page {}", pageId, ex);
            }
        }
    }

    private boolean isActive() {
        return searchProperties.isEnabled() && headingClient != null;
    }
}
