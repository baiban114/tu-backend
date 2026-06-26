package com.tu.backend.knowledgerelation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tu.backend.knowledgerelation.dto.CreateKnowledgeRelationRequest;
import com.tu.backend.knowledgerelation.dto.KnowledgeAnchorDto;
import com.tu.backend.knowledgerelation.dto.KnowledgeRelationDto;
import com.tu.backend.knowledgerelation.dto.RelationTypeDefDto;
import com.tu.backend.knowledgerelation.entity.KnowledgeRelationEntity;
import com.tu.backend.knowledgerelation.repository.KnowledgeRelationRepository;
import com.tu.backend.knowledge.repository.KnowledgeBaseRepository;
import com.tu.backend.knowledgerelation.entity.KnowledgePointEntity;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class KnowledgeRelationServiceCreateRelationTest {

    @Test
    void createsRelationWithContentAnchorOnlyWithoutFromPointId() {
        KnowledgeRelationRepository relationRepository = org.mockito.Mockito.mock(KnowledgeRelationRepository.class);
        RelationTypeService relationTypeService = org.mockito.Mockito.mock(RelationTypeService.class);
        KnowledgePointService knowledgePointService = org.mockito.Mockito.mock(KnowledgePointService.class);
        KnowledgeBaseRepository knowledgeBaseRepository = org.mockito.Mockito.mock(KnowledgeBaseRepository.class);
        ObjectMapper objectMapper = new ObjectMapper();

        when(knowledgeBaseRepository.existsById("kb-1")).thenReturn(true);
        when(relationTypeService.resolveType("kb-1", "case")).thenReturn(
            new RelationTypeDefDto("rt-case", "kb-1", "case", "案例", "#1677ff", null, false, true, true)
        );

        KnowledgePointEntity toPoint = new KnowledgePointEntity();
        toPoint.setId("kp-to");
        toPoint.setKbId("kb-1");
        toPoint.setTitle("基础概念");
        when(knowledgePointService.findPointEntity("kp-to")).thenReturn(toPoint);
        when(knowledgePointService.loadTitles(eq("kb-1"), any())).thenReturn(Map.of("kp-to", "基础概念"));
        when(relationRepository.save(any(KnowledgeRelationEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        KnowledgeRelationService service = new KnowledgeRelationService(
            relationRepository,
            relationTypeService,
            knowledgePointService,
            knowledgeBaseRepository,
            objectMapper
        );

        KnowledgeAnchorDto from = new KnowledgeAnchorDto(
            "annotation",
            "page:p-1:selection:1:5",
            Map.of("title", "选区文本")
        );
        KnowledgeRelationDto dto = service.createRelation(
            "kb-1",
            new CreateKnowledgeRelationRequest("case", null, "kp-to", from, null, null)
        );

        assertThat(dto.toPointId()).isEqualTo("kp-to");
        assertThat(dto.fromPointId()).isNull();
        assertThat(dto.from()).isEqualTo(from);

        ArgumentCaptor<KnowledgeRelationEntity> captor = ArgumentCaptor.forClass(KnowledgeRelationEntity.class);
        org.mockito.Mockito.verify(relationRepository).save(captor.capture());
        assertThat(captor.getValue().getFromPointId()).isNull();
        assertThat(captor.getValue().getToPointId()).isEqualTo("kp-to");
    }
}
