package com.tu.backend.knowledgerelation.repository;

import com.tu.backend.knowledgerelation.entity.KnowledgeRelationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface KnowledgeRelationRepository extends JpaRepository<KnowledgeRelationEntity, String> {

    List<KnowledgeRelationEntity> findByKbIdOrderByUpdatedAtDescCreatedAtDesc(String kbId);

    List<KnowledgeRelationEntity> findByKbIdAndFromLocatorOrderByUpdatedAtDesc(String kbId, String fromLocator);

    List<KnowledgeRelationEntity> findByKbIdAndToLocatorOrderByUpdatedAtDesc(String kbId, String toLocator);

    List<KnowledgeRelationEntity> findByKbIdAndFromPointIdOrderByUpdatedAtDesc(String kbId, String fromPointId);

    List<KnowledgeRelationEntity> findByKbIdAndToPointIdOrderByUpdatedAtDesc(String kbId, String toPointId);

    @Modifying
    @Query("""
        DELETE FROM KnowledgeRelationEntity e
        WHERE e.kbId = :kbId
          AND e.sourceProvenance = 'migrated'
          AND (e.fromLocator = :pageLocator OR e.fromLocator LIKE CONCAT(:pagePrefix, '%'))
        """)
    void deleteMigratedByPage(@Param("kbId") String kbId, @Param("pageLocator") String pageLocator, @Param("pagePrefix") String pagePrefix);

    @Modifying
    void deleteByKbIdAndIdIn(String kbId, List<String> ids);

    void deleteByKbId(String kbId);
}
