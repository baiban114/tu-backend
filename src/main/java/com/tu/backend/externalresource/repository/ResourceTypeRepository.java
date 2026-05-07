package com.tu.backend.externalresource.repository;

import com.tu.backend.externalresource.entity.ResourceTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResourceTypeRepository extends JpaRepository<ResourceTypeEntity, String> {

    List<ResourceTypeEntity> findAllByOrderByNameAscCreatedAtAsc();

    boolean existsByCode(String code);

    boolean existsByName(String name);
}
