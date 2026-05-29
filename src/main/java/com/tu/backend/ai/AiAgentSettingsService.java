package com.tu.backend.ai;

import com.tu.backend.ai.dto.AiAgentSettingsDto;
import com.tu.backend.ai.dto.UpdateAiAgentSettingsRequest;
import com.tu.backend.ai.entity.AiAgentConfigEntity;
import com.tu.backend.ai.repository.AiAgentConfigRepository;
import com.tu.backend.common.BusinessException;
import com.tu.backend.secret.ManagedSecretService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiAgentSettingsService implements AiAgentRuntimeConfigResolver {

    public static final String CONFIG_ID = "default";
    public static final String SECRET_SCOPE = "ai-agent";
    public static final String API_KEY_SECRET_KEY = "default-api-key";
    private static final String API_KEY_SECRET_ID = SECRET_SCOPE + ":" + API_KEY_SECRET_KEY;

    private final AiAgentConfigRepository repository;
    private final ManagedSecretService secretService;

    public AiAgentSettingsService(
        AiAgentConfigRepository repository,
        ManagedSecretService secretService
    ) {
        this.repository = repository;
        this.secretService = secretService;
    }

    @Transactional(readOnly = true)
    public AiAgentSettingsDto getSettings() {
        return repository.findById(CONFIG_ID)
            .map(this::toDto)
            .orElseGet(this::emptySettingsDto);
    }

    @Transactional
    public AiAgentSettingsDto updateSettings(UpdateAiAgentSettingsRequest request) {
        AiAgentConfigEntity entity = repository.findById(CONFIG_ID).orElseGet(() -> {
            AiAgentConfigEntity created = new AiAgentConfigEntity();
            created.setId(CONFIG_ID);
            return created;
        });
        entity.setEnabled(request.enabled());
        entity.setBaseUrl(normalizeRequired(request.baseUrl(), "baseUrl is required"));
        entity.setModel(normalizeRequired(request.model(), "model is required"));
        if (request.apiKey() != null && !request.apiKey().isBlank()) {
            secretService.save(SECRET_SCOPE, API_KEY_SECRET_KEY, "AI Agent API Key", request.apiKey().trim());
            entity.setApiKeySecretId(API_KEY_SECRET_ID);
        } else if (entity.getApiKeySecretId() == null && secretService.exists(SECRET_SCOPE, API_KEY_SECRET_KEY)) {
            entity.setApiKeySecretId(API_KEY_SECRET_ID);
        }
        return toDto(repository.save(entity));
    }

    @Transactional
    public AiAgentSettingsDto deleteApiKey() {
        secretService.delete(SECRET_SCOPE, API_KEY_SECRET_KEY);
        AiAgentConfigEntity entity = repository.findById(CONFIG_ID).orElse(null);
        if (entity != null) {
            entity.setApiKeySecretId(null);
            return toDto(repository.save(entity));
        }
        return emptySettingsDto();
    }

    @Transactional(readOnly = true)
    public AiAgentRuntimeConfig runtimeConfig() {
        return repository.findById(CONFIG_ID)
            .map(this::runtimeFromEntity)
            .orElseGet(() -> new AiAgentRuntimeConfig(false, "", "", ""));
    }

    private AiAgentRuntimeConfig runtimeFromEntity(AiAgentConfigEntity entity) {
        return new AiAgentRuntimeConfig(
            entity.isEnabled(),
            entity.getBaseUrl(),
            secretService.getValue(SECRET_SCOPE, API_KEY_SECRET_KEY).orElse(""),
            entity.getModel()
        );
    }

    private AiAgentSettingsDto toDto(AiAgentConfigEntity entity) {
        return new AiAgentSettingsDto(
            entity.isEnabled(),
            entity.getBaseUrl(),
            entity.getModel(),
            secretService.exists(SECRET_SCOPE, API_KEY_SECRET_KEY)
        );
    }

    private AiAgentSettingsDto emptySettingsDto() {
        return new AiAgentSettingsDto(false, "", "", false);
    }

    private String normalizeRequired(String value, String message) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isBlank()) {
            throw new BusinessException(40000, message);
        }
        return normalized;
    }
}
