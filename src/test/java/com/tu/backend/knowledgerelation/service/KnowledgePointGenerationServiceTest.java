package com.tu.backend.knowledgerelation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tu.backend.content.entity.PageContentEntity;
import com.tu.backend.content.repository.PageContentRepository;
import com.tu.backend.knowledgerelation.dto.GenerateKnowledgePointsRequest;
import com.tu.backend.knowledgerelation.dto.KnowledgeAnchorDto;
import com.tu.backend.knowledgerelation.dto.KnowledgePointDto;
import com.tu.backend.knowledgerelation.dto.KnowledgePointGenerationResultDto;
import com.tu.backend.knowledge.repository.KnowledgeBaseRepository;
import com.tu.backend.page.entity.PageEntity;
import com.tu.backend.page.repository.PageRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class KnowledgePointGenerationServiceTest {

    @Test
    void generatesPageTreeAndSkipsExistingLocator() {
        PageRepository pageRepository = org.mockito.Mockito.mock(PageRepository.class);
        PageContentRepository pageContentRepository = org.mockito.Mockito.mock(PageContentRepository.class);
        KnowledgePointService knowledgePointService = org.mockito.Mockito.mock(KnowledgePointService.class);
        KnowledgeBaseRepository knowledgeBaseRepository = org.mockito.Mockito.mock(KnowledgeBaseRepository.class);
        ObjectMapper objectMapper = new ObjectMapper();

        PageEntity page = new PageEntity();
        page.setId("p-1");
        page.setKbId("kb-1");
        page.setTitle("第一章");
        page.setPageType("document");

        when(knowledgeBaseRepository.existsById("kb-1")).thenReturn(true);
        when(pageRepository.findByKbIdOrderBySortOrderAscCreatedAtAsc("kb-1")).thenReturn(List.of(page));
        when(knowledgePointService.findPointsByLocator("kb-1", "page:p-1"))
            .thenReturn(List.of())
            .thenReturn(List.of(pointDto("kp-1", "第一章")));
        when(knowledgePointService.ensurePointForAnchor(eq("kb-1"), any(KnowledgeAnchorDto.class), eq("第一章"), eq(null)))
            .thenReturn("kp-1");
        when(knowledgePointService.getPoint("kp-1")).thenReturn(pointDto("kp-1", "第一章"));

        KnowledgePointGenerationService service = new KnowledgePointGenerationService(
            pageRepository,
            pageContentRepository,
            knowledgePointService,
            knowledgeBaseRepository,
            objectMapper
        );

        KnowledgePointGenerationResultDto first = service.generate(
            "kb-1",
            new GenerateKnowledgePointsRequest(List.of("pageTree"), null)
        );
        assertThat(first.created()).isEqualTo(1);
        assertThat(first.skipped()).isZero();
        assertThat(first.items()).hasSize(1);
        assertThat(first.items().getFirst().status()).isEqualTo("created");

        KnowledgePointGenerationResultDto second = service.generate(
            "kb-1",
            new GenerateKnowledgePointsRequest(List.of("pageTree"), null)
        );
        assertThat(second.created()).isZero();
        assertThat(second.skipped()).isEqualTo(1);
        assertThat(second.items().getFirst().status()).isEqualTo("skipped");
    }

    @Test
    void generatesDocumentHeadingsFromRichtextBlocks() {
        PageRepository pageRepository = org.mockito.Mockito.mock(PageRepository.class);
        PageContentRepository pageContentRepository = org.mockito.Mockito.mock(PageContentRepository.class);
        KnowledgePointService knowledgePointService = org.mockito.Mockito.mock(KnowledgePointService.class);
        KnowledgeBaseRepository knowledgeBaseRepository = org.mockito.Mockito.mock(KnowledgeBaseRepository.class);
        ObjectMapper objectMapper = new ObjectMapper();

        PageEntity page = new PageEntity();
        page.setId("p-2");
        page.setKbId("kb-1");
        page.setTitle("文档页");
        page.setPageType("document");

        PageContentEntity content = new PageContentEntity();
        content.setPageId("p-2");
        content.setBlocksJson("""
            [
              {
                "id": "b-1",
                "type": "richtext",
                "document": {
                  "type": "doc",
                  "content": [
                    {
                      "type": "heading",
                      "attrs": { "level": 2, "blockId": "h-1" },
                      "content": [{ "type": "text", "text": "二级标题" }]
                    }
                  ]
                }
              }
            ]
            """);

        when(knowledgeBaseRepository.existsById("kb-1")).thenReturn(true);
        when(pageRepository.findByKbIdOrderBySortOrderAscCreatedAtAsc("kb-1")).thenReturn(List.of(page));
        when(pageContentRepository.findById("p-2")).thenReturn(Optional.of(content));
        when(knowledgePointService.findPointsByLocator(eq("kb-1"), eq("page:p-2:heading:h-1"))).thenReturn(List.of());
        when(knowledgePointService.ensurePointForAnchor(eq("kb-1"), any(KnowledgeAnchorDto.class), eq("二级标题"), eq(null)))
            .thenReturn("kp-2");

        KnowledgePointGenerationService service = new KnowledgePointGenerationService(
            pageRepository,
            pageContentRepository,
            knowledgePointService,
            knowledgeBaseRepository,
            objectMapper
        );

        KnowledgePointGenerationResultDto result = service.generate(
            "kb-1",
            new GenerateKnowledgePointsRequest(List.of("documentHeadings"), null)
        );

        assertThat(result.created()).isEqualTo(1);
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().getFirst().locator()).isEqualTo("page:p-2:heading:h-1");
        verify(knowledgePointService).ensurePointForAnchor(eq("kb-1"), any(KnowledgeAnchorDto.class), eq("二级标题"), eq(null));
    }

    private static KnowledgePointDto pointDto(String id, String title) {
        KnowledgePointDto dto = new KnowledgePointDto();
        dto.setId(id);
        dto.setTitle(title);
        return dto;
    }
}
