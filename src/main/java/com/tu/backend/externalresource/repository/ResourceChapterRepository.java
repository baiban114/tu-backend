package com.tu.backend.externalresource.repository;

import com.tu.backend.externalresource.entity.ResourceChapterEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResourceChapterRepository extends JpaRepository<ResourceChapterEntity, String> {

    List<ResourceChapterEntity> findByResourceItemIdOrderBySortOrderAscCreatedAtAsc(String resourceItemId);

    void deleteByResourceItemId(String resourceItemId);
}
