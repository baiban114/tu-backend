package com.tu.backend.externalresource.repository;

import com.tu.backend.externalresource.entity.ResourceItemEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ResourceItemRepository extends JpaRepository<ResourceItemEntity, String> {

    Page<ResourceItemEntity> findAllByOrderByUpdatedAtDescCreatedAtDesc(Pageable pageable);

    Page<ResourceItemEntity> findByTypeIdOrderByUpdatedAtDescCreatedAtDesc(String typeId, Pageable pageable);

    Page<ResourceItemEntity> findByWorkIdOrderByUpdatedAtDescCreatedAtDesc(String workId, Pageable pageable);

    Page<ResourceItemEntity> findByTypeIdAndWorkIdOrderByUpdatedAtDescCreatedAtDesc(String typeId, String workId, Pageable pageable);

    List<ResourceItemEntity> findAllByOrderByUpdatedAtDescCreatedAtDesc();

    List<ResourceItemEntity> findByTypeIdOrderByUpdatedAtDescCreatedAtDesc(String typeId);

    List<ResourceItemEntity> findByWorkIdOrderByUpdatedAtDescCreatedAtDesc(String workId);

    List<ResourceItemEntity> findByTypeIdAndWorkIdOrderByUpdatedAtDescCreatedAtDesc(String typeId, String workId);

    Optional<ResourceItemEntity> findByTypeIdAndIdentityValue(String typeId, String identityValue);

    boolean existsByTypeId(String typeId);

    boolean existsByWorkId(String workId);
}
