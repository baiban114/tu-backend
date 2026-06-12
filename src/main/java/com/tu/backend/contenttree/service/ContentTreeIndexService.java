package com.tu.backend.contenttree.service;

import com.tu.backend.contenttree.search.HeadingSearchIndexService;
import com.tu.backend.index.IndexProperties;
import com.tu.backend.page.repository.PageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class ContentTreeIndexService {

    private static final Logger log = LoggerFactory.getLogger(ContentTreeIndexService.class);

    private final ContentTreeNodeService contentTreeNodeService;
    private final PageRepository pageRepository;
    private final HeadingSearchIndexService headingSearchIndexService;
    private final IndexProperties indexProperties;

    public ContentTreeIndexService(
        ContentTreeNodeService contentTreeNodeService,
        PageRepository pageRepository,
        HeadingSearchIndexService headingSearchIndexService,
        IndexProperties indexProperties
    ) {
        this.contentTreeNodeService = contentTreeNodeService;
        this.pageRepository = pageRepository;
        this.headingSearchIndexService = headingSearchIndexService;
        this.indexProperties = indexProperties;
    }

    public void rebuildPageBestEffort(String pageId, String fingerprint) {
        if (pageId == null || pageId.isBlank()) {
            return;
        }
        try {
            if (shouldSkip(pageId, fingerprint)) {
                return;
            }
            contentTreeNodeService.rebuildPageOutline(pageId, fingerprint);
            headingSearchIndexService.indexPageBestEffort(pageId);
        } catch (Exception ex) {
            log.warn("failed to rebuild content tree outline for page {}", pageId, ex);
        }
    }

    public void deletePagesBestEffort(List<String> pageIds) {
        if (pageIds == null || pageIds.isEmpty()) {
            return;
        }
        try {
            contentTreeNodeService.deletePageOutlines(pageIds);
            headingSearchIndexService.deletePagesBestEffort(pageIds);
        } catch (Exception ex) {
            log.warn("failed to delete content tree outlines for pages {}", pageIds, ex);
        }
    }

    public void reindexAll() {
        pageRepository.findAll().forEach(page -> rebuildPageBestEffort(page.getId(), null));
    }

    private boolean shouldSkip(String pageId, String fingerprint) {
        if (!indexProperties.isFingerprintEnabled() || fingerprint == null || fingerprint.isBlank()) {
            return false;
        }
        return Objects.equals(fingerprint, contentTreeNodeService.getStoredFingerprint(pageId));
    }
}
