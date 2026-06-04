package com.tu.backend.externalresource.repository;

import com.tu.backend.externalresource.entity.ResourceItemRelationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResourceItemRelationRepository extends JpaRepository<ResourceItemRelationEntity, String> {

    List<ResourceItemRelationEntity> findByFromItemIdOrToItemIdOrderByCreatedAtAsc(String fromItemId, String toItemId);

    void deleteByFromItemIdOrToItemId(String fromItemId, String toItemId);
}
