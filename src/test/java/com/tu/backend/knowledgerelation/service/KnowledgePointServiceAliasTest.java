package com.tu.backend.knowledgerelation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tu.backend.common.BusinessException;
import com.tu.backend.knowledgerelation.dto.CreateKnowledgePointAliasRequest;
import com.tu.backend.knowledgerelation.entity.KnowledgePointAliasEntity;
import com.tu.backend.knowledgerelation.entity.KnowledgePointEntity;
import com.tu.backend.knowledgerelation.repository.KnowledgePointAliasRepository;
import com.tu.backend.knowledgerelation.repository.KnowledgePointAnchorRepository;
import com.tu.backend.knowledgerelation.repository.KnowledgePointRepository;
import com.tu.backend.knowledge.repository.KnowledgeBaseRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class KnowledgePointServiceAliasTest {

    @Test
    void addsListsAndDeletesAliases() {
        KnowledgePointRepository pointRepository = org.mockito.Mockito.mock(KnowledgePointRepository.class);
        KnowledgePointAnchorRepository anchorRepository = org.mockito.Mockito.mock(KnowledgePointAnchorRepository.class);
        KnowledgePointAliasRepository aliasRepository = org.mockito.Mockito.mock(KnowledgePointAliasRepository.class);
        KnowledgeBaseRepository kbRepository = org.mockito.Mockito.mock(KnowledgeBaseRepository.class);
        ObjectMapper objectMapper = new ObjectMapper();

        KnowledgePointEntity point = new KnowledgePointEntity();
        point.setId("kp-1");
        point.setKbId("kb-1");
        point.setTitle("基础概念");

        when(pointRepository.findById("kp-1")).thenReturn(Optional.of(point));
        when(aliasRepository.findByKnowledgePointIdOrderByAliasAsc("kp-1")).thenReturn(List.of());
        when(aliasRepository.save(any(KnowledgePointAliasEntity.class))).thenAnswer(invocation -> {
            KnowledgePointAliasEntity entity = invocation.getArgument(0);
            entity.setAlias(entity.getAlias());
            return entity;
        });

        KnowledgePointService service = new KnowledgePointService(
            pointRepository,
            anchorRepository,
            aliasRepository,
            kbRepository,
            objectMapper
        );

        var created = service.addAlias("kp-1", new CreateKnowledgePointAliasRequest("核心概念"));
        assertThat(created.alias()).isEqualTo("核心概念");

        ArgumentCaptor<KnowledgePointAliasEntity> captor = ArgumentCaptor.forClass(KnowledgePointAliasEntity.class);
        verify(aliasRepository).save(captor.capture());
        assertThat(captor.getValue().getKnowledgePointId()).isEqualTo("kp-1");

        KnowledgePointAliasEntity stored = new KnowledgePointAliasEntity();
        stored.setId("kpal-1");
        stored.setKnowledgePointId("kp-1");
        stored.setAlias("核心概念");
        when(aliasRepository.findByKnowledgePointIdOrderByAliasAsc("kp-1")).thenReturn(List.of(stored));
        assertThat(service.listAliases("kp-1")).hasSize(1);

        when(aliasRepository.findById("kpal-1")).thenReturn(Optional.of(stored));
        service.deleteAlias("kpal-1");
        verify(aliasRepository).delete(stored);
    }

    @Test
    void rejectsDuplicateAlias() {
        KnowledgePointRepository pointRepository = org.mockito.Mockito.mock(KnowledgePointRepository.class);
        KnowledgePointAnchorRepository anchorRepository = org.mockito.Mockito.mock(KnowledgePointAnchorRepository.class);
        KnowledgePointAliasRepository aliasRepository = org.mockito.Mockito.mock(KnowledgePointAliasRepository.class);
        KnowledgeBaseRepository kbRepository = org.mockito.Mockito.mock(KnowledgeBaseRepository.class);
        ObjectMapper objectMapper = new ObjectMapper();

        KnowledgePointEntity point = new KnowledgePointEntity();
        point.setId("kp-1");
        point.setKbId("kb-1");
        point.setTitle("基础概念");

        KnowledgePointAliasEntity existing = new KnowledgePointAliasEntity();
        existing.setAlias("核心概念");

        when(pointRepository.findById("kp-1")).thenReturn(Optional.of(point));
        when(aliasRepository.findByKnowledgePointIdOrderByAliasAsc("kp-1")).thenReturn(List.of(existing));

        KnowledgePointService service = new KnowledgePointService(
            pointRepository,
            anchorRepository,
            aliasRepository,
            kbRepository,
            objectMapper
        );

        assertThatThrownBy(() -> service.addAlias("kp-1", new CreateKnowledgePointAliasRequest("核心概念")))
            .isInstanceOf(BusinessException.class);
    }
}
