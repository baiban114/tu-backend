package com.tu.backend.knowledgerelation.repository;

import com.tu.backend.knowledgerelation.entity.KnowledgePointEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KnowledgePointRepository extends JpaRepository<KnowledgePointEntity, String> {

    List<KnowledgePointEntity> findByKbIdOrderBySortOrderAscTitleAsc(String kbId);

    void deleteByKbId(String kbId);
}
