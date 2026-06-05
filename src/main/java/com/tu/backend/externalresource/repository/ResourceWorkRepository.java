package com.tu.backend.externalresource.repository;

import com.tu.backend.externalresource.entity.ResourceWorkEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ResourceWorkRepository extends JpaRepository<ResourceWorkEntity, String> {

    Page<ResourceWorkEntity> findAllByOrderByUpdatedAtDescCreatedAtDesc(Pageable pageable);

    Page<ResourceWorkEntity> findByTypeIdOrderByUpdatedAtDescCreatedAtDesc(String typeId, Pageable pageable);

    List<ResourceWorkEntity> findAllByOrderByUpdatedAtDescCreatedAtDesc();

    List<ResourceWorkEntity> findByTypeIdOrderByUpdatedAtDescCreatedAtDesc(String typeId);

    Optional<ResourceWorkEntity> findByTypeIdAndTitle(String typeId, String title);

    Optional<ResourceWorkEntity> findByTypeIdAndClusterKey(String typeId, String clusterKey);

    boolean existsByTypeId(String typeId);
}
