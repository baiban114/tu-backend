package com.tu.backend.externalresource.repository;

import com.tu.backend.externalresource.entity.ResourceTypeEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ResourceTypeRepository extends JpaRepository<ResourceTypeEntity, String> {

    Page<ResourceTypeEntity> findAllByOrderByNameAscCreatedAtAsc(Pageable pageable);

    List<ResourceTypeEntity> findAllByOrderByNameAscCreatedAtAsc();

    boolean existsByCode(String code);

    boolean existsByName(String name);

    Optional<ResourceTypeEntity> findByCode(String code);

    Optional<ResourceTypeEntity> findByName(String name);
}
