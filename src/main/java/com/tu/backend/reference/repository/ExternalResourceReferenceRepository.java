package com.tu.backend.reference.repository;

import com.tu.backend.reference.entity.ExternalResourceReferenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ExternalResourceReferenceRepository extends JpaRepository<ExternalResourceReferenceEntity, String> {

    List<ExternalResourceReferenceEntity> findByPageIdInOrderByUpdatedAtDescCreatedAtDesc(Collection<String> pageIds);

    List<ExternalResourceReferenceEntity> findAllByOrderByUpdatedAtDescCreatedAtDesc();

    List<ExternalResourceReferenceEntity> findByResourceItemId(String resourceItemId);

    List<ExternalResourceReferenceEntity> findByResourceExcerptId(String resourceExcerptId);

    void deleteByPageId(String pageId);

    void deleteByPageIdIn(Collection<String> pageIds);
}
