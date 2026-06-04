package com.tu.backend.externalresource.repository;

import com.tu.backend.externalresource.entity.UrlClusterRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UrlClusterRuleRepository extends JpaRepository<UrlClusterRuleEntity, String> {

    List<UrlClusterRuleEntity> findByEnabledTrueOrderByPriorityDescDomainAsc();

    List<UrlClusterRuleEntity> findByDomainOrderByPriorityDesc(String domain);

    boolean existsByBuiltInTrue();
}
