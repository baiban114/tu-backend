package com.tu.backend.reference.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tu.backend.content.repository.PageContentRepository;
import com.tu.backend.externalresource.repository.ResourceExcerptRepository;
import com.tu.backend.externalresource.repository.ResourceItemRepository;
import com.tu.backend.externalresource.repository.ResourceTypeRepository;
import com.tu.backend.externalresource.repository.ResourceWorkRepository;
import com.tu.backend.page.repository.PageRepository;
import com.tu.backend.reference.entity.ExternalResourceReferenceEntity;
import com.tu.backend.reference.repository.ExternalReferenceOccurrenceRepository;
import com.tu.backend.reference.repository.ExternalResourceReferenceRepository;
import com.tu.backend.reference.repository.InternalReferenceRecordRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ReferenceServiceExternalResourceTest {

    @Test
    void rebuildPageReferencesIndexesExternalResourceEmbed() {
        InternalReferenceRecordRepository internalRepository = mock(InternalReferenceRecordRepository.class);
        ExternalReferenceOccurrenceRepository externalRepository = mock(ExternalReferenceOccurrenceRepository.class);
        ExternalResourceReferenceRepository externalResourceRepository = mock(ExternalResourceReferenceRepository.class);
        when(externalRepository.findByPageIdInOrderByUpdatedAtDescCreatedAtDesc(List.of("p1"))).thenReturn(List.of());
        when(externalResourceRepository.findByPageIdInOrderByUpdatedAtDescCreatedAtDesc(List.of("p1"))).thenReturn(List.of());

        ReferenceService service = new ReferenceService(
            internalRepository,
            externalRepository,
            externalResourceRepository,
            mock(PageRepository.class),
            mock(PageContentRepository.class),
            mock(ResourceItemRepository.class),
            mock(ResourceExcerptRepository.class),
            mock(ResourceTypeRepository.class),
            mock(ResourceWorkRepository.class),
            new ObjectMapper()
        );

        service.rebuildPageReferences("p1", """
            [
              {
                "id": "block-resource",
                "type": "externalResource",
                "externalResource": {
                  "resourceItemId": "ri-book",
                  "resourceExcerptId": "re-book-1",
                  "mode": "excerpt",
                  "snapshot": {
                    "resourceTitle": "示例图书",
                    "excerptTitle": "示例节选"
                  }
                }
              }
            ]
            """);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ExternalResourceReferenceEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(externalResourceRepository).saveAll(captor.capture());
        List<ExternalResourceReferenceEntity> saved = captor.getValue();
        assertThat(saved).hasSize(1);
        assertThat(saved.getFirst().getPageId()).isEqualTo("p1");
        assertThat(saved.getFirst().getBlockId()).isEqualTo("block-resource");
        assertThat(saved.getFirst().getSourceKind()).isEqualTo("externalResource");
        assertThat(saved.getFirst().getSourceLocator()).isEqualTo("blocks[0].externalResource");
        assertThat(saved.getFirst().getResourceItemId()).isEqualTo("ri-book");
        assertThat(saved.getFirst().getResourceExcerptId()).isEqualTo("re-book-1");
    }

    @Test
    void rebuildPageReferencesIndexesHeadingSourceComment() {
        InternalReferenceRecordRepository internalRepository = mock(InternalReferenceRecordRepository.class);
        ExternalReferenceOccurrenceRepository externalRepository = mock(ExternalReferenceOccurrenceRepository.class);
        ExternalResourceReferenceRepository externalResourceRepository = mock(ExternalResourceReferenceRepository.class);
        when(externalRepository.findByPageIdInOrderByUpdatedAtDescCreatedAtDesc(List.of("p1"))).thenReturn(List.of());
        when(externalResourceRepository.findByPageIdInOrderByUpdatedAtDescCreatedAtDesc(List.of("p1"))).thenReturn(List.of());

        ReferenceService service = new ReferenceService(
            internalRepository,
            externalRepository,
            externalResourceRepository,
            mock(PageRepository.class),
            mock(PageContentRepository.class),
            mock(ResourceItemRepository.class),
            mock(ResourceExcerptRepository.class),
            mock(ResourceTypeRepository.class),
            mock(ResourceWorkRepository.class),
            new ObjectMapper()
        );

        service.rebuildPageReferences("p1", """
            [
              {
                "id": "page-content",
                "type": "richtext",
                "content": "<!--tu:heading-source id=\\"hs-abc\\" item=\\"ri-book\\" excerpt=\\"re-book-1\\" title=\\"示例节选\\"-->\\n## 本节标题"
              }
            ]
            """);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ExternalResourceReferenceEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(externalResourceRepository).saveAll(captor.capture());
        List<ExternalResourceReferenceEntity> saved = captor.getValue();
        assertThat(saved).hasSize(1);
        assertThat(saved.getFirst().getPageId()).isEqualTo("p1");
        assertThat(saved.getFirst().getBlockId()).isEqualTo("page-content");
        assertThat(saved.getFirst().getSourceKind()).isEqualTo("headingSource");
        assertThat(saved.getFirst().getSourceLocator()).isEqualTo("content:heading:hs-abc");
        assertThat(saved.getFirst().getResourceItemId()).isEqualTo("ri-book");
        assertThat(saved.getFirst().getResourceExcerptId()).isEqualTo("re-book-1");
    }
}
