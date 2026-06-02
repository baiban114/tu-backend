package com.tu.backend.externalresource.repository;

import com.tu.backend.externalresource.entity.ResourceExcerptEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResourceExcerptRepository extends JpaRepository<ResourceExcerptEntity, String> {

    List<ResourceExcerptEntity> findByResourceItemIdOrderBySortOrderAscCreatedAtAsc(String resourceItemId);

    List<ResourceExcerptEntity> findByResourceItemId(String resourceItemId);

    List<ResourceExcerptEntity> findAllByOrderByUpdatedAtDescCreatedAtDesc();

    void deleteByResourceItemId(String resourceItemId);
}
