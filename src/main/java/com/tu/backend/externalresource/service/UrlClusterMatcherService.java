package com.tu.backend.externalresource.service;

import com.tu.backend.externalresource.entity.UrlClusterRuleEntity;
import com.tu.backend.externalresource.repository.UrlClusterRuleRepository;
import com.tu.backend.externalresource.util.ExternalUrlNormalizer;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class UrlClusterMatcherService {

    public record ClusterMatch(String clusterKey, String variantHint) {
    }

    private final UrlClusterRuleRepository ruleRepository;

    public UrlClusterMatcherService(UrlClusterRuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }

    public Optional<ClusterMatch> match(String url) {
        ExternalUrlNormalizer.ParsedExternalUrl parsed = ExternalUrlNormalizer.parse(url);
        if (parsed == null) {
            return Optional.empty();
        }
        URI uri;
        try {
            uri = new URI(parsed.baseUrl());
        } catch (Exception ex) {
            return Optional.empty();
        }
        String host = normalizeHost(uri.getHost());
        if (host == null) {
            return Optional.empty();
        }
        String path = ExternalUrlNormalizer.normalizePathname(uri.getPath());

        List<UrlClusterRuleEntity> rules = ruleRepository.findByEnabledTrueOrderByPriorityDescDomainAsc()
            .stream()
            .filter(rule -> matchesDomain(rule, host))
            .sorted(Comparator.comparingInt(UrlClusterRuleEntity::getPriority).reversed()
                .thenComparing((UrlClusterRuleEntity rule) -> rule.getPathRegex().length()).reversed())
            .toList();

        for (UrlClusterRuleEntity rule : rules) {
            Optional<ClusterMatch> match = applyRule(rule, host, path);
            if (match.isPresent()) {
                return match;
            }
        }
        return Optional.empty();
    }

    private Optional<ClusterMatch> applyRule(UrlClusterRuleEntity rule, String host, String path) {
        Pattern pattern;
        try {
            pattern = Pattern.compile(rule.getPathRegex());
        } catch (Exception ex) {
            return Optional.empty();
        }
        Matcher matcher = pattern.matcher(path);
        if (!matcher.matches() && !matcher.find()) {
            return Optional.empty();
        }

        String clusterKey = formatClusterKey(rule.getClusterKeyFormat(), host, matcher);
        if (clusterKey.isBlank()) {
            return Optional.empty();
        }

        String variantHint = null;
        if (rule.getVariantGroup() != null && rule.getVariantGroup() > 0) {
            try {
                variantHint = matcher.group(rule.getVariantGroup());
            } catch (IndexOutOfBoundsException ignored) {
                variantHint = null;
            }
        }
        if (variantHint != null) {
            variantHint = variantHint.trim();
            if (variantHint.isBlank()) {
                variantHint = null;
            }
        }
        return Optional.of(new ClusterMatch(clusterKey, variantHint));
    }

    private String formatClusterKey(String format, String host, Matcher matcher) {
        String result = format.replace("{domain}", host);
        for (int group = 1; group <= matcher.groupCount(); group++) {
            String value = matcher.group(group);
            if (value == null) {
                value = "";
            }
            result = result.replace("{" + group + "}", value);
        }
        return result.trim();
    }

    private boolean matchesDomain(UrlClusterRuleEntity rule, String host) {
        String ruleDomain = normalizeHost(rule.getDomain());
        if (ruleDomain == null || "*".equals(ruleDomain)) {
            return true;
        }
        return host.equals(ruleDomain) || host.endsWith("." + ruleDomain);
    }

    private String normalizeHost(String host) {
        if (host == null) {
            return null;
        }
        String normalized = host.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("www.")) {
            normalized = normalized.substring(4);
        }
        return normalized;
    }
}
