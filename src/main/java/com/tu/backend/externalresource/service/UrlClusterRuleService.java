package com.tu.backend.externalresource.service;

import com.tu.backend.common.BusinessException;
import com.tu.backend.common.PageResponse;
import com.tu.backend.externalresource.dto.CreateUrlClusterRuleRequest;
import com.tu.backend.externalresource.dto.UpdateUrlClusterRuleRequest;
import com.tu.backend.externalresource.dto.UrlClusterRuleDto;
import com.tu.backend.externalresource.entity.UrlClusterRuleEntity;
import com.tu.backend.externalresource.repository.UrlClusterRuleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Service
public class UrlClusterRuleService {

    private final UrlClusterRuleRepository ruleRepository;

    public UrlClusterRuleService(UrlClusterRuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<UrlClusterRuleDto> listRules(int page, int pageSize) {
        int safePage = Math.max(0, page);
        int safePageSize = Math.max(1, Math.min(pageSize, 200));
        Pageable pageable = PageRequest.of(
            safePage,
            safePageSize,
            Sort.by(Sort.Order.desc("priority"), Sort.Order.asc("domain"))
        );
        Page<UrlClusterRuleEntity> entityPage = ruleRepository.findAll(pageable);
        List<UrlClusterRuleDto> items = entityPage.getContent().stream().map(this::toDto).toList();
        return PageResponse.of(items, entityPage.getTotalElements(), safePage, safePageSize);
    }

    @Transactional
    public UrlClusterRuleDto createRule(CreateUrlClusterRuleRequest request) {
        validateRegex(request.pathRegex());
        UrlClusterRuleEntity entity = new UrlClusterRuleEntity();
        entity.setId("ucr-" + compactUuid());
        apply(entity, request.domain(), request.pathRegex(), request.clusterKeyFormat(),
            request.variantGroup(), request.priority(), request.enabled() == null || request.enabled(),
            request.description(), false);
        return toDto(ruleRepository.save(entity));
    }

    @Transactional
    public UrlClusterRuleDto updateRule(String id, UpdateUrlClusterRuleRequest request) {
        UrlClusterRuleEntity entity = findRule(id);
        validateRegex(request.pathRegex());
        apply(entity, request.domain(), request.pathRegex(), request.clusterKeyFormat(),
            request.variantGroup(), request.priority(), request.enabled(), request.description(), entity.isBuiltIn());
        return toDto(ruleRepository.save(entity));
    }

    @Transactional
    public void deleteRule(String id) {
        UrlClusterRuleEntity entity = findRule(id);
        if (entity.isBuiltIn()) {
            throw new BusinessException(40009, "built-in url cluster rule cannot be deleted");
        }
        ruleRepository.delete(entity);
    }

    private void apply(
        UrlClusterRuleEntity entity,
        String domain,
        String pathRegex,
        String clusterKeyFormat,
        Integer variantGroup,
        int priority,
        boolean enabled,
        String description,
        boolean builtIn
    ) {
        entity.setDomain(normalizeDomain(domain));
        entity.setPathRegex(pathRegex.trim());
        entity.setClusterKeyFormat(clusterKeyFormat.trim());
        entity.setVariantGroup(variantGroup == null || variantGroup <= 0 ? null : variantGroup);
        entity.setPriority(priority);
        entity.setEnabled(enabled);
        entity.setDescription(blankToNull(description));
        entity.setBuiltIn(builtIn);
    }

    private UrlClusterRuleEntity findRule(String id) {
        return ruleRepository.findById(id)
            .orElseThrow(() -> new BusinessException(40001, "url cluster rule not found"));
    }

    private void validateRegex(String pathRegex) {
        try {
            Pattern.compile(pathRegex.trim());
        } catch (PatternSyntaxException ex) {
            throw new BusinessException(40000, "invalid path regex");
        }
    }

    private String normalizeDomain(String domain) {
        if (domain == null || domain.isBlank()) {
            throw new BusinessException(40000, "domain required");
        }
        return domain.trim().toLowerCase();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private UrlClusterRuleDto toDto(UrlClusterRuleEntity entity) {
        return new UrlClusterRuleDto(
            entity.getId(),
            entity.getDomain(),
            entity.getPathRegex(),
            entity.getClusterKeyFormat(),
            entity.getVariantGroup(),
            entity.getPriority(),
            entity.isEnabled(),
            entity.isBuiltIn(),
            entity.getDescription()
        );
    }

    private String compactUuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
