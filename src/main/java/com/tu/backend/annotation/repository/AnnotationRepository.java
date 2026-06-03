package com.tu.backend.annotation.repository;

import com.tu.backend.annotation.entity.AnnotationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

public interface AnnotationRepository extends JpaRepository<AnnotationEntity, String> {

    Page<AnnotationEntity> findByStatusOrderByOrphanedAtDesc(String status, Pageable pageable);

    long countByStatus(String status);

    void deleteByPageIdIn(Collection<String> pageIds);
}
