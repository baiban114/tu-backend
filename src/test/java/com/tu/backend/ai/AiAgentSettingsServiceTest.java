package com.tu.backend.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tu.backend.ai.entity.AiAgentConfigEntity;
import com.tu.backend.ai.dto.UpdateAiAgentSettingsRequest;
import com.tu.backend.ai.repository.AiAgentConfigRepository;
import com.tu.backend.secret.ManagedSecretService;
import com.tu.backend.secret.SecretCryptoService;
import com.tu.backend.secret.SecretProperties;
import com.tu.backend.secret.entity.ManagedSecretEntity;
import com.tu.backend.secret.repository.ManagedSecretRepository;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AiAgentSettingsServiceTest {

    @Test
    void savesEncryptedApiKeyAndDoesNotExposePlaintext() {
        TestContext context = new TestContext();
        AiAgentSettingsService service = context.service();

        var dto = service.updateSettings(new UpdateAiAgentSettingsRequest(
            true,
            "https://api.example.com/v1",
            "gpt-test",
            "sk-secret",
            30,
            300,
            300
        ));
        var secret = context.secretRepository.findByScopeAndKey(
            AiAgentSettingsService.SECRET_SCOPE,
            AiAgentSettingsService.API_KEY_SECRET_KEY
        ).orElseThrow();

        assertThat(dto.apiKeyConfigured()).isTrue();
        assertThat(secret.getEncryptedValue()).doesNotContain("sk-secret");
        assertThat(service.runtimeConfig().apiKey()).isEqualTo("sk-secret");
    }

    @Test
    void retainsExistingApiKeyWhenUpdateOmitsKey() {
        TestContext context = new TestContext();
        AiAgentSettingsService service = context.service();
        service.updateSettings(new UpdateAiAgentSettingsRequest(
            true, "https://one.example/v1", "model-a", "sk-secret", 30, 300, 300
        ));

        var dto = service.updateSettings(new UpdateAiAgentSettingsRequest(
            false, "https://two.example/v1", "model-b", "", 30, 600, 600
        ));

        assertThat(dto.apiKeyConfigured()).isTrue();
        assertThat(service.runtimeConfig().enabled()).isFalse();
        assertThat(service.runtimeConfig().baseUrl()).isEqualTo("https://two.example/v1");
        assertThat(service.runtimeConfig().model()).isEqualTo("model-b");
        assertThat(service.runtimeConfig().apiKey()).isEqualTo("sk-secret");
    }

    @Test
    void deletesApiKey() {
        TestContext context = new TestContext();
        AiAgentSettingsService service = context.service();
        service.updateSettings(new UpdateAiAgentSettingsRequest(
            true, "https://api.example.com/v1", "model", "sk-secret", 30, 300, 300
        ));

        var dto = service.deleteApiKey();

        assertThat(dto.apiKeyConfigured()).isFalse();
        assertThat(service.runtimeConfig().apiKey()).isEmpty();
    }

    @Test
    void returnsEmptyRuntimeConfigWhenSystemConfigIsMissing() {
        AiAgentConfigRepository configRepository = mock(AiAgentConfigRepository.class);
        when(configRepository.findById(AiAgentSettingsService.CONFIG_ID)).thenReturn(Optional.empty());
        AiAgentSettingsService service = new AiAgentSettingsService(
            configRepository,
            mock(ManagedSecretService.class),
            new AiAgentProperties()
        );

        AiAgentRuntimeConfig config = service.runtimeConfig();

        assertThat(config.enabled()).isFalse();
        assertThat(config.baseUrl()).isEmpty();
        assertThat(config.model()).isEmpty();
        assertThat(config.apiKey()).isEmpty();
        assertThat(config.readTimeoutSeconds()).isEqualTo(300);
    }

    @Test
    void persistsHttpTimeoutsInSettings() {
        TestContext context = new TestContext();
        AiAgentSettingsService service = context.service();

        var dto = service.updateSettings(new UpdateAiAgentSettingsRequest(
            true, "https://api.example.com/v1", "model", "sk-secret", 15, 600, 480
        ));

        assertThat(dto.connectTimeoutSeconds()).isEqualTo(15);
        assertThat(dto.readTimeoutSeconds()).isEqualTo(600);
        assertThat(dto.requestTimeoutSeconds()).isEqualTo(480);
        assertThat(service.runtimeConfig().readTimeoutSeconds()).isEqualTo(600);
    }

    private static final class TestContext {
        private final Map<String, AiAgentConfigEntity> configs = new HashMap<>();
        private final Map<String, ManagedSecretEntity> secrets = new HashMap<>();
        private final AiAgentConfigRepository configRepository = mock(AiAgentConfigRepository.class);
        private final ManagedSecretRepository secretRepository = mock(ManagedSecretRepository.class);

        TestContext() {
            when(configRepository.findById(anyString())).thenAnswer(invocation -> {
                String id = invocation.getArgument(0);
                return Optional.ofNullable(configs.get(id));
            });
            when(configRepository.save(any(AiAgentConfigEntity.class))).thenAnswer(invocation -> {
                AiAgentConfigEntity entity = invocation.getArgument(0);
                configs.put(entity.getId(), entity);
                return entity;
            });
            when(secretRepository.findByScopeAndKey(anyString(), anyString())).thenAnswer(invocation -> {
                String scope = invocation.getArgument(0);
                String key = invocation.getArgument(1);
                return Optional.ofNullable(secrets.get(scope + ":" + key));
            });
            when(secretRepository.save(any(ManagedSecretEntity.class))).thenAnswer(invocation -> {
                ManagedSecretEntity entity = invocation.getArgument(0);
                secrets.put(entity.getScope() + ":" + entity.getKey(), entity);
                return entity;
            });
            org.mockito.Mockito.doAnswer(invocation -> {
                String scope = invocation.getArgument(0);
                String key = invocation.getArgument(1);
                secrets.remove(scope + ":" + key);
                return null;
            }).when(secretRepository).deleteByScopeAndKey(anyString(), anyString());
        }

        AiAgentSettingsService service() {
            SecretProperties secretProperties = new SecretProperties();
            secretProperties.setEncryptionKey(Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes()));
            ManagedSecretService secretService = new ManagedSecretService(
                secretRepository,
                new SecretCryptoService(secretProperties)
            );
            return new AiAgentSettingsService(configRepository, secretService, new AiAgentProperties());
        }
    }
}
