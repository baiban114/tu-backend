package com.tu.backend.taskintegration.repository;

import com.tu.backend.taskintegration.entity.ExternalResourceRelationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExternalResourceRelationRepository extends JpaRepository<ExternalResourceRelationEntity, String> {

    List<ExternalResourceRelationEntity> findByProviderAndExternalIdOrderByUpdatedAtDescCreatedAtDesc(String provider, String externalId);

    Optional<ExternalResourceRelationEntity> findByProviderAndExternalIdAndPageIdAndBlockIdAndRelationType(
        String provider,
        String externalId,
        String pageId,
        String blockId,
        String relationType
    );
}
