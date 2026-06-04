package com.tu.backend.externalresource.config;

import com.tu.backend.externalresource.entity.UrlClusterRuleEntity;
import com.tu.backend.externalresource.repository.UrlClusterRuleRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class UrlClusterRuleBootstrapRunner implements ApplicationRunner {

    private final UrlClusterRuleRepository ruleRepository;

    public UrlClusterRuleBootstrapRunner(UrlClusterRuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (ruleRepository.existsByBuiltInTrue()) {
            return;
        }
        seed(rule("ucr-github", "github.com", "^/([^/]+)/([^/]+)(?:/.*)?$",
            "github.com|{1}|{2}", 0, 100, "GitHub 仓库路径"));
        seed(rule("ucr-gitlab", "gitlab.com", "^/([^/]+)/([^/]+)(?:/.*)?$",
            "gitlab.com|{1}|{2}", 0, 90, "GitLab 项目路径"));
        seed(rule("ucr-numeric-id", "*", "^/(?:[^/]+/)*([0-9]{4,})(?:/([^/]+))?/?$",
            "{domain}|id|{1}", 2, 10, "路径中含 4 位以上数字 ID 的通用规则"));
    }

    private void seed(UrlClusterRuleEntity entity) {
        ruleRepository.save(entity);
    }

    private static UrlClusterRuleEntity rule(
        String id,
        String domain,
        String pathRegex,
        String clusterKeyFormat,
        int variantGroup,
        int priority,
        String description
    ) {
        UrlClusterRuleEntity entity = new UrlClusterRuleEntity();
        entity.setId(id);
        entity.setDomain(domain);
        entity.setPathRegex(pathRegex);
        entity.setClusterKeyFormat(clusterKeyFormat);
        entity.setVariantGroup(variantGroup == 0 ? null : variantGroup);
        entity.setPriority(priority);
        entity.setEnabled(true);
        entity.setBuiltIn(true);
        entity.setDescription(description);
        return entity;
    }
}
