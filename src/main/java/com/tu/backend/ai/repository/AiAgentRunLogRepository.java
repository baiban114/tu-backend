package com.tu.backend.ai.repository;

import com.tu.backend.ai.entity.AiAgentRunLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AiAgentRunLogRepository extends JpaRepository<AiAgentRunLogEntity, String>, JpaSpecificationExecutor<AiAgentRunLogEntity> {
}
