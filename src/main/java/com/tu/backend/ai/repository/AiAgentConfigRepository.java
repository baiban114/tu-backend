package com.tu.backend.ai.repository;

import com.tu.backend.ai.entity.AiAgentConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiAgentConfigRepository extends JpaRepository<AiAgentConfigEntity, String> {
}
