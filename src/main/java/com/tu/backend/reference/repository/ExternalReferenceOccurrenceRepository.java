package com.tu.backend.reference.repository;

import com.tu.backend.reference.entity.ExternalReferenceOccurrenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ExternalReferenceOccurrenceRepository extends JpaRepository<ExternalReferenceOccurrenceEntity, String> {

    List<ExternalReferenceOccurrenceEntity> findByPageIdInOrderByUpdatedAtDescCreatedAtDesc(Collection<String> pageIds);

    List<ExternalReferenceOccurrenceEntity> findAllByOrderByUpdatedAtDescCreatedAtDesc();

    List<ExternalReferenceOccurrenceEntity> findByResourceItemId(String resourceItemId);

    void deleteByPageId(String pageId);

    void deleteByPageIdIn(Collection<String> pageIds);
}
