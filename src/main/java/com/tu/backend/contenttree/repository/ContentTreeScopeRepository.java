package com.tu.backend.contenttree.repository;

import com.tu.backend.contenttree.entity.ContentTreeScopeEntity;
import com.tu.backend.contenttree.entity.ContentTreeScopeId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContentTreeScopeRepository extends JpaRepository<ContentTreeScopeEntity, ContentTreeScopeId> {
}
