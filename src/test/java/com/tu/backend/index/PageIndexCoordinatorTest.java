package com.tu.backend.index;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tu.backend.content.entity.PageContentEntity;
import com.tu.backend.content.repository.PageContentRepository;
import com.tu.backend.page.entity.PageEntity;
import com.tu.backend.page.repository.PageRepository;
import com.tu.backend.contenttree.service.ContentTreeIndexService;
import com.tu.backend.rag.RagIndexService;
import com.tu.backend.search.SearchIndexService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PageIndexCoordinatorTest {

    @Mock
    private PageRepository pageRepository;

    @Mock
    private PageContentRepository pageContentRepository;

    @Mock
    private RagIndexService ragIndexService;

    @Mock
    private SearchIndexService searchIndexService;

    @Mock
    private ContentTreeIndexService contentTreeIndexService;

    private IndexProperties indexProperties;
    private PageIndexCoordinator coordinator;

    @BeforeEach
    void setUp() {
        indexProperties = new IndexProperties();
        indexProperties.setFingerprintEnabled(true);
        coordinator = new PageIndexCoordinator(
            pageRepository,
            pageContentRepository,
            ragIndexService,
            searchIndexService,
            contentTreeIndexService,
            indexProperties
        );
    }

    @Test
    void onPageContentChangedIndexesSearchImmediately() {
        coordinator.onPageContentChanged("p-1");

        verify(searchIndexService, times(1)).indexPageBestEffort("p-1");
        verify(contentTreeIndexService, times(1)).rebuildPageBestEffort(eq("p-1"), any());
        verify(ragIndexService, never()).indexPageBestEffort("p-1");
        assertThat(coordinator.isDirty("p-1")).isTrue();
    }

    @Test
    void flushPageIndexesRagOnlyWhenDirty() {
        stubPage("p-1", "Title", "{\"blocks\":[]}");

        coordinator.flushPage("p-1");
        verify(ragIndexService, never()).indexPageBestEffort("p-1");
        verify(searchIndexService, never()).indexPageBestEffort("p-1");

        coordinator.onPageContentChanged("p-1");
        verify(searchIndexService, times(1)).indexPageBestEffort("p-1");

        coordinator.flushPage("p-1");

        verify(ragIndexService, times(1)).indexPageBestEffort("p-1");
        verify(searchIndexService, times(1)).indexPageBestEffort("p-1");
        assertThat(coordinator.isDirty("p-1")).isFalse();
    }

    @Test
    void skipsRagIndexingWhenFingerprintUnchanged() {
        stubPage("p-1", "Title", "{\"blocks\":[]}");

        coordinator.onPageContentChanged("p-1");
        coordinator.flushPage("p-1");
        verify(ragIndexService, times(1)).indexPageBestEffort("p-1");

        coordinator.onPageContentChanged("p-1");
        coordinator.flushPage("p-1");
        verify(ragIndexService, times(1)).indexPageBestEffort("p-1");
        verify(searchIndexService, times(2)).indexPageBestEffort("p-1");
    }

    @Test
    void cancelPreventsRagIndexingOnFlush() {
        coordinator.onPageContentChanged("p-1");
        coordinator.cancel("p-1");
        coordinator.flushPage("p-1");

        verify(ragIndexService, never()).indexPageBestEffort("p-1");
        verify(searchIndexService, times(1)).indexPageBestEffort("p-1");
    }

    @Test
    void deletePagesRemovesIndexImmediately() {
        coordinator.deletePages("kb-1", List.of("p-1", "p-2"));

        verify(ragIndexService).deletePagesBestEffort("kb-1", List.of("p-1", "p-2"));
        verify(searchIndexService).deletePagesBestEffort("kb-1", List.of("p-1", "p-2"));
        verify(contentTreeIndexService).deletePagesBestEffort(List.of("p-1", "p-2"));
    }

    @Test
    void indexPageNowAlwaysIndexesBoth() {
        stubPage("p-1", "Title", "{\"blocks\":[]}");

        coordinator.indexPageNow("p-1");

        verify(ragIndexService).indexPageBestEffort("p-1");
        verify(searchIndexService).indexPageBestEffort("p-1");
        assertThat(coordinator.isDirty("p-1")).isFalse();
    }

    private void stubPage(String pageId, String title, String blocksJson) {
        PageEntity page = new PageEntity();
        page.setId(pageId);
        page.setTitle(title);
        when(pageRepository.findById(pageId)).thenReturn(Optional.of(page));

        PageContentEntity content = new PageContentEntity();
        content.setPageId(pageId);
        content.setBlocksJson(blocksJson);
        when(pageContentRepository.findById(pageId)).thenReturn(Optional.of(content));
    }
}
