package com.tu.backend.knowledgerelation.repository;

import com.tu.backend.knowledgerelation.entity.RelationTypeDefEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RelationTypeDefRepository extends JpaRepository<RelationTypeDefEntity, String> {

    List<RelationTypeDefEntity> findByKbIdIsNullOrderByTypeKeyAsc();

    List<RelationTypeDefEntity> findByKbIdOrderByTypeKeyAsc(String kbId);

    Optional<RelationTypeDefEntity> findByKbIdAndTypeKey(String kbId, String typeKey);

    Optional<RelationTypeDefEntity> findByKbIdIsNullAndTypeKey(String typeKey);
}
