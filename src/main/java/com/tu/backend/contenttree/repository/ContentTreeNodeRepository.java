package com.tu.backend.contenttree.repository;

import com.tu.backend.contenttree.entity.ContentTreeNodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ContentTreeNodeRepository extends JpaRepository<ContentTreeNodeEntity, String> {

    List<ContentTreeNodeEntity> findByScopeTypeAndScopeIdOrderBySortOrderAscCreatedAtAsc(
        String scopeType,
        String scopeId
    );

    List<ContentTreeNodeEntity> findByScopeTypeAndScopeIdInOrderBySortOrderAscCreatedAtAsc(
        String scopeType,
        Collection<String> scopeIds
    );

    void deleteByScopeTypeAndScopeId(String scopeType, String scopeId);

    void deleteByScopeTypeAndScopeIdIn(String scopeType, Collection<String> scopeIds);
}
