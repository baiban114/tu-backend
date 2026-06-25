package com.tu.backend.content.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tu.backend.content.dto.SavePageContentRequest;
import com.tu.backend.content.entity.PageContentEntity;
import com.tu.backend.content.repository.PageContentRepository;
import com.tu.backend.index.PageIndexCoordinator;
import com.tu.backend.page.entity.PageEntity;
import com.tu.backend.page.repository.PageRepository;
import com.tu.backend.knowledgerelation.service.KnowledgeRelationRebuildService;
import com.tu.backend.knowledgerelation.service.KnowledgeRelationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PageContentServiceDocumentTest {

    @Mock
    private PageContentRepository pageContentRepository;
    @Mock
    private PageRepository pageRepository;
    @Mock
    private PageIndexCoordinator pageIndexCoordinator;
    @Mock
    private ReferenceService referenceService;
    @Mock
    private KnowledgeRelationRebuildService knowledgeRelationRebuildService;
    @Mock
    private KnowledgeRelationService knowledgeRelationService;

    private PageContentService service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        service = new PageContentService(
            pageContentRepository,
            pageRepository,
            objectMapper,
            pageIndexCoordinator,
            referenceService,
            knowledgeRelationRebuildService,
            knowledgeRelationService
        );
    }

    @Test
    void saveContentStoresDocumentInRichtextBlock() throws Exception {
        String pageId = "p-1";
        when(pageRepository.findById(pageId)).thenReturn(Optional.of(new PageEntity()));
        when(pageContentRepository.findById(pageId)).thenReturn(Optional.empty());
        when(pageContentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> document = Map.of(
            "type", "doc",
            "content", List.of(Map.of("type", "paragraph"))
        );

        service.saveContent(pageId, new SavePageContentRequest(
            "",
            List.of(),
            List.of(),
            Map.of(),
            null,
            document,
            2
        ));

        ArgumentCaptor<PageContentEntity> captor = ArgumentCaptor.forClass(PageContentEntity.class);
        verify(pageContentRepository).save(captor.capture());
        var blocks = objectMapper.readTree(captor.getValue().getBlocksJson());
        assertThat(blocks.isArray()).isTrue();
        assertThat(blocks.get(0).path("type").asText()).isEqualTo("richtext");
        assertThat(blocks.get(0).path("document").path("type").asText()).isEqualTo("doc");
        assertThat(blocks.get(0).path("metadata").path("schemaVersion").asInt()).isEqualTo(2);
        verify(referenceService).rebuildPageReferences(eq(pageId), any());
        verify(knowledgeRelationRebuildService).rebuildPageRelations(eq(pageId), any());
    }
}
