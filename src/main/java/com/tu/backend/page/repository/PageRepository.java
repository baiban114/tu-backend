package com.tu.backend.page.repository;

import com.tu.backend.page.entity.PageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface PageRepository extends JpaRepository<PageEntity, String> {

    List<PageEntity> findByKbIdOrderBySortOrderAscCreatedAtAsc(String kbId);

    List<PageEntity> findByKbIdAndParentIdOrderBySortOrderAscCreatedAtAsc(String kbId, String parentId);

    long countByKbIdAndParentId(String kbId, String parentId);

    long countByKbIdAndParentIdIsNull(String kbId);

    List<PageEntity> findByParentIdIn(Collection<String> parentIds);

    void deleteByKbId(String kbId);
}

