package com.tu.backend.index;

import com.tu.backend.content.entity.PageContentEntity;
import com.tu.backend.content.repository.PageContentRepository;
import com.tu.backend.page.entity.PageEntity;
import com.tu.backend.page.repository.PageRepository;
import com.tu.backend.contenttree.service.ContentTreeIndexService;
import com.tu.backend.rag.RagIndexService;
import com.tu.backend.search.SearchIndexService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PageIndexCoordinator {

    private static final Logger log = LoggerFactory.getLogger(PageIndexCoordinator.class);

    private final PageRepository pageRepository;
    private final PageContentRepository pageContentRepository;
    private final RagIndexService ragIndexService;
    private final SearchIndexService searchIndexService;
    private final ContentTreeIndexService contentTreeIndexService;
    private final IndexProperties indexProperties;

    private final Set<String> dirtyPageIds = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<String, String> lastRagIndexedFingerprints = new ConcurrentHashMap<>();

    public PageIndexCoordinator(
        PageRepository pageRepository,
        PageContentRepository pageContentRepository,
        RagIndexService ragIndexService,
        SearchIndexService searchIndexService,
        ContentTreeIndexService contentTreeIndexService,
        IndexProperties indexProperties
    ) {
        this.pageRepository = pageRepository;
        this.pageContentRepository = pageContentRepository;
        this.ragIndexService = ragIndexService;
        this.searchIndexService = searchIndexService;
        this.contentTreeIndexService = contentTreeIndexService;
        this.indexProperties = indexProperties;
    }

    public void onPageContentChanged(String pageId) {
        if (pageId == null || pageId.isBlank()) {
            return;
        }
        dirtyPageIds.add(pageId);
        String fingerprint = computeFingerprint(pageId);
        searchIndexService.indexPageBestEffort(pageId);
        contentTreeIndexService.rebuildPageBestEffort(pageId, fingerprint);
    }

    public void markDirty(String pageId) {
        onPageContentChanged(pageId);
    }

    public void flushPage(String pageId) {
        if (pageId == null || pageId.isBlank() || !dirtyPageIds.contains(pageId)) {
            return;
        }
        flushPageIfDirty(pageId);
    }

    public void flushAllDirty() {
        for (String pageId : Set.copyOf(dirtyPageIds)) {
            flushPageIfDirty(pageId);
        }
    }

    public void indexPageNow(String pageId) {
        if (pageId == null || pageId.isBlank()) {
            return;
        }
        ragIndexService.indexPageBestEffort(pageId);
        searchIndexService.indexPageBestEffort(pageId);
        contentTreeIndexService.rebuildPageBestEffort(pageId, computeFingerprint(pageId));
        lastRagIndexedFingerprints.put(pageId, computeFingerprint(pageId));
        dirtyPageIds.remove(pageId);
    }

    public void cancel(String pageId) {
        if (pageId == null || pageId.isBlank()) {
            return;
        }
        dirtyPageIds.remove(pageId);
        lastRagIndexedFingerprints.remove(pageId);
    }

    public void cancelAll(List<String> pageIds) {
        for (String pageId : pageIds) {
            cancel(pageId);
        }
    }

    public void deletePages(String kbId, List<String> pageIds) {
        cancelAll(pageIds);
        ragIndexService.deletePagesBestEffort(kbId, pageIds);
        searchIndexService.deletePagesBestEffort(kbId, pageIds);
        contentTreeIndexService.deletePagesBestEffort(pageIds);
    }

    public void deleteKnowledgeBase(String kbId) {
        List<String> pageIds = pageRepository.findByKbIdOrderBySortOrderAscCreatedAtAsc(kbId)
            .stream()
            .map(PageEntity::getId)
            .toList();
        cancelAll(pageIds);
        ragIndexService.deleteKnowledgeBaseBestEffort(kbId);
        searchIndexService.deleteKnowledgeBaseBestEffort(kbId);
    }

    public void reindexKnowledgeBaseNow(String kbId) {
        for (PageEntity page : pageRepository.findByKbIdOrderBySortOrderAscCreatedAtAsc(kbId)) {
            indexPageNow(page.getId());
        }
    }

    boolean isDirty(String pageId) {
        return dirtyPageIds.contains(pageId);
    }

    String getLastRagIndexedFingerprint(String pageId) {
        return lastRagIndexedFingerprints.get(pageId);
    }

    private void flushPageIfDirty(String pageId) {
        if (!dirtyPageIds.contains(pageId)) {
            return;
        }

        if (indexProperties.isFingerprintEnabled()) {
            String fingerprint = computeFingerprint(pageId);
            String previous = lastRagIndexedFingerprints.get(pageId);
            if (fingerprint.equals(previous)) {
                dirtyPageIds.remove(pageId);
                return;
            }
        }

        ragIndexService.indexPageBestEffort(pageId);
        lastRagIndexedFingerprints.put(pageId, computeFingerprint(pageId));
        dirtyPageIds.remove(pageId);
    }

    String computeFingerprint(String pageId) {
        String title = pageRepository.findById(pageId)
            .map(PageEntity::getTitle)
            .map(value -> value == null ? "" : value)
            .orElse("");
        String blocksJson = pageContentRepository.findById(pageId)
            .map(PageContentEntity::getBlocksJson)
            .orElse("");
        return sha256(title + "\0" + blocksJson);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            log.warn("SHA-256 unavailable, skipping fingerprint comparison");
            return value;
        }
    }
}
