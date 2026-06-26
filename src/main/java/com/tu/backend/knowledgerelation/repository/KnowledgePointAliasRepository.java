package com.tu.backend.knowledgerelation.repository;

import com.tu.backend.knowledgerelation.entity.KnowledgePointAliasEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface KnowledgePointAliasRepository extends JpaRepository<KnowledgePointAliasEntity, String> {

    List<KnowledgePointAliasEntity> findByKnowledgePointIdOrderByAliasAsc(String knowledgePointId);

    List<KnowledgePointAliasEntity> findByKnowledgePointIdIn(Collection<String> knowledgePointIds);

    void deleteByKnowledgePointId(String knowledgePointId);

    @Query("""
        select a from KnowledgePointAliasEntity a
        join KnowledgePointEntity p on p.id = a.knowledgePointId
        where p.kbId = :kbId
        """)
    List<KnowledgePointAliasEntity> findByKbId(@Param("kbId") String kbId);
}
