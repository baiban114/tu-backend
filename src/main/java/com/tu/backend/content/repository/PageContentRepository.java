package com.tu.backend.content.repository;

import com.tu.backend.content.entity.PageContentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

public interface PageContentRepository extends JpaRepository<PageContentEntity, String> {

    void deleteByPageIdIn(Collection<String> pageIds);
}

