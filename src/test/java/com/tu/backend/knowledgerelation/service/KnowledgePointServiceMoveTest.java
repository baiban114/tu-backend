package com.tu.backend.knowledgerelation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tu.backend.common.BusinessException;
import com.tu.backend.knowledgerelation.dto.UpdateKnowledgePointRequest;
import com.tu.backend.knowledgerelation.entity.KnowledgePointEntity;
import com.tu.backend.knowledgerelation.repository.KnowledgePointAliasRepository;
import com.tu.backend.knowledgerelation.repository.KnowledgePointAnchorRepository;
import com.tu.backend.knowledgerelation.repository.KnowledgePointRepository;
import com.tu.backend.knowledge.repository.KnowledgeBaseRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class KnowledgePointServiceMoveTest {

  @Test
  void movesPointToNewParentAndReordersSiblings() {
    KnowledgePointRepository pointRepository = org.mockito.Mockito.mock(KnowledgePointRepository.class);
    KnowledgePointAnchorRepository anchorRepository = org.mockito.Mockito.mock(KnowledgePointAnchorRepository.class);
    KnowledgePointAliasRepository aliasRepository = org.mockito.Mockito.mock(KnowledgePointAliasRepository.class);
    KnowledgeBaseRepository kbRepository = org.mockito.Mockito.mock(KnowledgeBaseRepository.class);
    ObjectMapper objectMapper = new ObjectMapper();

    KnowledgePointEntity root = point("kp-root", "kb-1", null, "Root", 0);
    KnowledgePointEntity child = point("kp-child", "kb-1", "kp-root", "Child", 0);
    KnowledgePointEntity sibling = point("kp-sibling", "kb-1", "kp-root", "Sibling", 1);
    KnowledgePointEntity moving = point("kp-moving", "kb-1", null, "Moving", 1);
    List<KnowledgePointEntity> all = new ArrayList<>(List.of(root, child, sibling, moving));

    when(pointRepository.findById("kp-moving")).thenReturn(Optional.of(moving));
    when(pointRepository.findById("kp-root")).thenReturn(Optional.of(root));
    when(pointRepository.findByKbIdOrderBySortOrderAscTitleAsc("kb-1")).thenAnswer(invocation -> List.copyOf(all));
    when(pointRepository.save(any(KnowledgePointEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(aliasRepository.findByKnowledgePointIdOrderByAliasAsc(any())).thenReturn(List.of());

    KnowledgePointService service = new KnowledgePointService(
        pointRepository,
        anchorRepository,
        aliasRepository,
        kbRepository,
        objectMapper
    );

    var updated = service.updatePoint("kp-moving", moveRequest("kp-root", 1));
    assertThat(updated.getParentId()).isEqualTo("kp-root");
    assertThat(updated.getSortOrder()).isEqualTo(1);

    ArgumentCaptor<KnowledgePointEntity> captor = ArgumentCaptor.forClass(KnowledgePointEntity.class);
    org.mockito.Mockito.verify(pointRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
    assertThat(captor.getAllValues())
        .anyMatch(entity -> "kp-sibling".equals(entity.getId()) && entity.getSortOrder() == 2);
  }

  @Test
  void movesChildPointToRootLevel() {
    KnowledgePointRepository pointRepository = org.mockito.Mockito.mock(KnowledgePointRepository.class);
    KnowledgePointAnchorRepository anchorRepository = org.mockito.Mockito.mock(KnowledgePointAnchorRepository.class);
    KnowledgePointAliasRepository aliasRepository = org.mockito.Mockito.mock(KnowledgePointAliasRepository.class);
    KnowledgeBaseRepository kbRepository = org.mockito.Mockito.mock(KnowledgeBaseRepository.class);
    ObjectMapper objectMapper = new ObjectMapper();

    KnowledgePointEntity root = point("kp-root", "kb-1", null, "Root", 0);
    KnowledgePointEntity child = point("kp-child", "kb-1", "kp-root", "Child", 0);
    KnowledgePointEntity otherRoot = point("kp-other", "kb-1", null, "Other", 1);
    List<KnowledgePointEntity> all = new ArrayList<>(List.of(root, child, otherRoot));

    when(pointRepository.findById("kp-child")).thenReturn(Optional.of(child));
    when(pointRepository.findByKbIdOrderBySortOrderAscTitleAsc("kb-1")).thenAnswer(invocation -> List.copyOf(all));
    when(pointRepository.save(any(KnowledgePointEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(aliasRepository.findByKnowledgePointIdOrderByAliasAsc(any())).thenReturn(List.of());

    KnowledgePointService service = new KnowledgePointService(
        pointRepository,
        anchorRepository,
        aliasRepository,
        kbRepository,
        objectMapper
    );

    UpdateKnowledgePointRequest request = new UpdateKnowledgePointRequest();
    request.setParentId(null);
    request.setSortOrder(1);

    var updated = service.updatePoint("kp-child", request);
    assertThat(updated.getParentId()).isNull();
    assertThat(updated.getSortOrder()).isEqualTo(1);
  }

  @Test
  void rejectsMoveUnderDescendant() {
    KnowledgePointRepository pointRepository = org.mockito.Mockito.mock(KnowledgePointRepository.class);
    KnowledgePointAnchorRepository anchorRepository = org.mockito.Mockito.mock(KnowledgePointAnchorRepository.class);
    KnowledgePointAliasRepository aliasRepository = org.mockito.Mockito.mock(KnowledgePointAliasRepository.class);
    KnowledgeBaseRepository kbRepository = org.mockito.Mockito.mock(KnowledgeBaseRepository.class);
    ObjectMapper objectMapper = new ObjectMapper();

    KnowledgePointEntity root = point("kp-root", "kb-1", null, "Root", 0);
    KnowledgePointEntity child = point("kp-child", "kb-1", "kp-root", "Child", 0);
    List<KnowledgePointEntity> all = List.of(root, child);

    when(pointRepository.findById("kp-root")).thenReturn(Optional.of(root));
    when(pointRepository.findByKbIdOrderBySortOrderAscTitleAsc("kb-1")).thenReturn(all);

    KnowledgePointService service = new KnowledgePointService(
        pointRepository,
        anchorRepository,
        aliasRepository,
        kbRepository,
        objectMapper
    );

    assertThatThrownBy(() -> service.updatePoint("kp-root", moveRequest("kp-child", 0)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("descendant");
  }

  @Test
  void rejectsDeleteWhenPointHasChildren() {
    KnowledgePointRepository pointRepository = org.mockito.Mockito.mock(KnowledgePointRepository.class);
    KnowledgePointAnchorRepository anchorRepository = org.mockito.Mockito.mock(KnowledgePointAnchorRepository.class);
    KnowledgePointAliasRepository aliasRepository = org.mockito.Mockito.mock(KnowledgePointAliasRepository.class);
    KnowledgeBaseRepository kbRepository = org.mockito.Mockito.mock(KnowledgeBaseRepository.class);
    ObjectMapper objectMapper = new ObjectMapper();

    KnowledgePointEntity root = point("kp-root", "kb-1", null, "Root", 0);
    KnowledgePointEntity child = point("kp-child", "kb-1", "kp-root", "Child", 0);

    when(pointRepository.findById("kp-root")).thenReturn(Optional.of(root));
    when(pointRepository.findByKbIdOrderBySortOrderAscTitleAsc("kb-1")).thenReturn(List.of(root, child));

    KnowledgePointService service = new KnowledgePointService(
        pointRepository,
        anchorRepository,
        aliasRepository,
        kbRepository,
        objectMapper
    );

    assertThatThrownBy(() -> service.deletePoint("kp-root"))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("children");
  }

  private static UpdateKnowledgePointRequest moveRequest(String parentId, Integer sortOrder) {
    UpdateKnowledgePointRequest request = new UpdateKnowledgePointRequest();
    request.setParentId(parentId);
    request.setSortOrder(sortOrder);
    return request;
  }

  private static KnowledgePointEntity point(
      String id,
      String kbId,
      String parentId,
      String title,
      int sortOrder
  ) {
    KnowledgePointEntity entity = new KnowledgePointEntity();
    entity.setId(id);
    entity.setKbId(kbId);
    entity.setParentId(parentId);
    entity.setTitle(title);
    entity.setSortOrder(sortOrder);
    entity.setStatus("active");
    return entity;
  }
}
