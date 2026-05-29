package com.tu.backend.ai;

import com.tu.backend.ai.dto.AiAgentConnectionTestResultDto;
import com.tu.backend.ai.dto.AiAgentSettingsDto;
import com.tu.backend.ai.dto.UpdateAiAgentSettingsRequest;
import com.tu.backend.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/settings")
public class AiAgentSettingsController {

    private final AiAgentSettingsService settingsService;
    private final AiAgentConnectionTester connectionTester;

    public AiAgentSettingsController(
        AiAgentSettingsService settingsService,
        AiAgentConnectionTester connectionTester
    ) {
        this.settingsService = settingsService;
        this.connectionTester = connectionTester;
    }

    @GetMapping
    public ApiResponse<AiAgentSettingsDto> settings() {
        return ApiResponse.success(settingsService.getSettings());
    }

    @PutMapping
    public ApiResponse<AiAgentSettingsDto> updateSettings(@Valid @RequestBody UpdateAiAgentSettingsRequest request) {
        return ApiResponse.success(settingsService.updateSettings(request));
    }

    @DeleteMapping("/api-key")
    public ApiResponse<AiAgentSettingsDto> deleteApiKey() {
        return ApiResponse.success(settingsService.deleteApiKey());
    }

    @PostMapping("/test")
    public ApiResponse<AiAgentConnectionTestResultDto> testConnection() {
        connectionTester.completeJson(
            settingsService.runtimeConfig(),
            "You are a connection checker. Return JSON only. " + AiAgentPrompts.DEFAULT_CHINESE_OUTPUT_CONSTRAINT,
            "Return {\"ok\":true}."
        );
        return ApiResponse.success(new AiAgentConnectionTestResultDto(true, "连接成功"));
    }
}
