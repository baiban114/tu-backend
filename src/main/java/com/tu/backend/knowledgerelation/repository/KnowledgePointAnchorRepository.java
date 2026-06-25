package com.tu.backend.knowledgerelation.repository;

import com.tu.backend.knowledgerelation.entity.KnowledgePointAnchorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface KnowledgePointAnchorRepository extends JpaRepository<KnowledgePointAnchorEntity, String> {

    List<KnowledgePointAnchorEntity> findByKnowledgePointIdOrderByPrimaryAnchorDescCreatedAtAsc(String knowledgePointId);

    List<KnowledgePointAnchorEntity> findByLocatorOrderByUpdatedAtDesc(String locator);

    Optional<KnowledgePointAnchorEntity> findFirstByKnowledgePointIdAndLocator(String knowledgePointId, String locator);

    @Modifying
    @Query("""
        DELETE FROM KnowledgePointAnchorEntity a
        WHERE a.locator = :pageLocator OR a.locator LIKE CONCAT(:pagePrefix, '%')
        """)
    void deleteByPageLocator(@Param("pageLocator") String pageLocator, @Param("pagePrefix") String pagePrefix);

    @Modifying
    void deleteByKnowledgePointId(String knowledgePointId);
}
