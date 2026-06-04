package com.tu.backend.externalresource.service;

import com.tu.backend.common.BusinessException;
import com.tu.backend.externalresource.dto.RegisterResourceUrlRequest;
import com.tu.backend.externalresource.dto.RegisterResourceUrlResult;
import com.tu.backend.externalresource.dto.ResourceExcerptDto;
import com.tu.backend.externalresource.dto.ResourceItemDto;
import com.tu.backend.externalresource.entity.ResourceExcerptEntity;
import com.tu.backend.externalresource.entity.ResourceItemEntity;
import com.tu.backend.externalresource.entity.ResourceTypeEntity;
import com.tu.backend.externalresource.entity.ResourceWorkEntity;
import com.tu.backend.externalresource.model.FieldSource;
import com.tu.backend.externalresource.model.VariantKind;
import com.tu.backend.externalresource.repository.ResourceExcerptRepository;
import com.tu.backend.externalresource.repository.ResourceItemRepository;
import com.tu.backend.externalresource.repository.ResourceTypeRepository;
import com.tu.backend.externalresource.repository.ResourceWorkRepository;
import com.tu.backend.externalresource.util.ExternalUrlNormalizer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class WebLinkRegistrationService {

    private static final String WEB_LINK_TYPE_CODE = "web-link";

    private final ResourceTypeRepository typeRepository;
    private final ResourceWorkRepository workRepository;
    private final ResourceItemRepository itemRepository;
    private final ResourceExcerptRepository excerptRepository;
    private final UrlClusterMatcherService clusterMatcherService;
    private final PageTitleFetcherService pageTitleFetcherService;
    private final ExternalResourceService externalResourceService;

    public WebLinkRegistrationService(
        ResourceTypeRepository typeRepository,
        ResourceWorkRepository workRepository,
        ResourceItemRepository itemRepository,
        ResourceExcerptRepository excerptRepository,
        UrlClusterMatcherService clusterMatcherService,
        PageTitleFetcherService pageTitleFetcherService,
        ExternalResourceService externalResourceService
    ) {
        this.typeRepository = typeRepository;
        this.workRepository = workRepository;
        this.itemRepository = itemRepository;
        this.excerptRepository = excerptRepository;
        this.clusterMatcherService = clusterMatcherService;
        this.pageTitleFetcherService = pageTitleFetcherService;
        this.externalResourceService = externalResourceService;
    }

    @Transactional
    public RegisterResourceUrlResult registerFromUrl(RegisterResourceUrlRequest request) {
        ExternalUrlNormalizer.ParsedExternalUrl parsed = ExternalUrlNormalizer.parse(request.url());
        if (parsed == null) {
            throw new BusinessException(40000, "invalid resource URL");
        }

        ResourceTypeEntity linkType = ensureWebLinkType();
        ResolveItemResult resolved = resolveItem(linkType, parsed, request.label());
        ResourceItemEntity item = resolved.item();
        boolean createdItem = resolved.created();

        if (!parsed.excerptMode()) {
            return new RegisterResourceUrlResult(
                "resource",
                externalResourceService.getItem(item.getId()),
                null,
                createdItem,
                false
            );
        }

        ExcerptUpsert excerptUpsert = upsertExcerpt(item, parsed, request);
        return new RegisterResourceUrlResult(
            "excerpt",
            externalResourceService.getItem(item.getId()),
            excerptUpsert.dto(),
            createdItem,
            excerptUpsert.created()
        );
    }

    @Transactional(readOnly = true)
    public Optional<String> fetchPageTitle(String url) {
        String baseUrl = ExternalUrlNormalizer.toBasePageUrl(url);
        if (baseUrl == null) {
            return Optional.empty();
        }
        return pageTitleFetcherService.fetchTitle(baseUrl);
    }

    private ResolveItemResult resolveItem(ResourceTypeEntity linkType, ExternalUrlNormalizer.ParsedExternalUrl parsed, String label) {
        Optional<ResourceItemEntity> existing = itemRepository.findByTypeIdAndIdentityValue(linkType.getId(), parsed.baseUrl());
        if (existing.isPresent()) {
            ResourceItemEntity item = existing.get();
            maybeUpdateAutoTitle(item, parsed.baseUrl(), label);
            return new ResolveItemResult(itemRepository.save(item), false);
        }

        Optional<ResourceItemEntity> legacy = itemRepository.findByTypeIdAndIdentityValue(linkType.getId(), parsed.href());
        if (legacy.isPresent()) {
            ResourceItemEntity item = legacy.get();
            if (!FieldSource.isManual(item.getTitleSource()) || item.getIdentityValue() == null) {
                item.setIdentityValue(parsed.baseUrl());
                item.setSourceUrl(parsed.baseUrl());
            }
            maybeUpdateAutoTitle(item, parsed.baseUrl(), label);
            return new ResolveItemResult(itemRepository.save(item), false);
        }

        return new ResolveItemResult(createWebLinkItem(linkType, parsed, label), true);
    }

    private ResourceItemEntity createWebLinkItem(ResourceTypeEntity linkType, ExternalUrlNormalizer.ParsedExternalUrl parsed, String label) {
        String baseUrl = parsed.baseUrl();
        String title = deriveTitle(label, baseUrl);
        Optional<UrlClusterMatcherService.ClusterMatch> clusterMatch = clusterMatcherService.match(baseUrl);
        ResourceWorkEntity work = resolveWork(linkType, baseUrl, title, clusterMatch);

        ResourceItemEntity item = new ResourceItemEntity();
        item.setId("ri-" + compactUuid());
        item.setTypeId(linkType.getId());
        item.setWorkId(work.getId());
        item.setTitle(title);
        item.setTitleSource(FieldSource.AUTO);
        item.setWorkIdSource(FieldSource.AUTO);
        item.setIdentityValue(baseUrl);
        item.setSourceUrl(baseUrl);
        item.setEdition(clusterMatch.map(UrlClusterMatcherService.ClusterMatch::variantHint).orElse(null));
        if (clusterMatch.map(UrlClusterMatcherService.ClusterMatch::variantHint).orElse(null) != null) {
            item.setVariantKind(VariantKind.OTHER.code());
        }
        item.setNote("由 URL 自动登记");
        return itemRepository.save(item);
    }

    private ResourceWorkEntity resolveWork(
        ResourceTypeEntity linkType,
        String baseUrl,
        String title,
        Optional<UrlClusterMatcherService.ClusterMatch> clusterMatch
    ) {
        if (clusterMatch.isPresent()) {
            String clusterKey = clusterMatch.get().clusterKey();
            Optional<ResourceWorkEntity> existing = workRepository.findByTypeIdAndClusterKey(linkType.getId(), clusterKey);
            if (existing.isPresent()) {
                ResourceWorkEntity work = existing.get();
                maybeUpdateWorkTitle(work, title);
                return work;
            }
            ResourceWorkEntity work = new ResourceWorkEntity();
            work.setId("rw-" + compactUuid());
            work.setTypeId(linkType.getId());
            work.setTitle(title);
            work.setTitleSource(FieldSource.AUTO);
            work.setClusterKey(clusterKey);
            work.setDescription("URL 聚类：" + clusterKey);
            return workRepository.save(work);
        }

        ResourceWorkEntity work = new ResourceWorkEntity();
        work.setId("rw-" + compactUuid());
        work.setTypeId(linkType.getId());
        work.setTitle(title);
        work.setTitleSource(FieldSource.AUTO);
        work.setClusterKey("single|" + baseUrl);
        work.setDescription("待归并：" + baseUrl);
        return workRepository.save(work);
    }

    private void maybeUpdateWorkTitle(ResourceWorkEntity work, String title) {
        if (FieldSource.isManual(work.getTitleSource())) {
            return;
        }
        if (title != null && !title.isBlank() && !title.equals(work.getTitle())) {
            work.setTitle(title);
        }
    }

    private void maybeUpdateAutoTitle(ResourceItemEntity item, String baseUrl, String label) {
        if (FieldSource.isManual(item.getTitleSource())) {
            return;
        }
        String title = deriveTitle(label, baseUrl);
        if (!title.equals(item.getTitle())) {
            item.setTitle(title);
        }
    }

    private ExcerptUpsert upsertExcerpt(ResourceItemEntity item, ExternalUrlNormalizer.ParsedExternalUrl parsed, RegisterResourceUrlRequest request) {
        String anchorKey = parsed.anchor().trim();
        String locator = anchorKey.startsWith("#") ? anchorKey : "#" + anchorKey;
        List<ResourceExcerptEntity> excerpts = excerptRepository.findByResourceItemIdOrderBySortOrderAscCreatedAtAsc(item.getId());
        Optional<ResourceExcerptEntity> existing = excerpts.stream()
            .filter(excerpt -> normalizeLocatorKey(excerpt.getLocator()).equals(anchorKey))
            .findFirst();

        String excerptTitle = buildExcerptTitle(anchorKey, request.label());
        String excerptBody = blankToNull(request.excerptText());

        if (existing.isPresent()) {
            ResourceExcerptEntity entity = existing.get();
            boolean updated = false;
            if (excerptBody != null) {
                entity.setExcerptText(excerptBody);
                updated = true;
            }
            if (updated) {
                entity = excerptRepository.save(entity);
            }
            return new ExcerptUpsert(externalResourceService.getExcerpt(entity.getId()), false);
        }

        ResourceExcerptEntity entity = new ResourceExcerptEntity();
        entity.setId("re-" + compactUuid());
        entity.setResourceItemId(item.getId());
        entity.setTitle(excerptTitle);
        entity.setLocator(locator);
        entity.setExcerptText(excerptBody);
        entity.setNote("由 URL 自动登记");
        entity.setSortOrder(excerpts.size());
        entity = excerptRepository.save(entity);
        return new ExcerptUpsert(externalResourceService.getExcerpt(entity.getId()), true);
    }

    private ResourceTypeEntity ensureWebLinkType() {
        return typeRepository.findAll().stream()
            .filter(type -> WEB_LINK_TYPE_CODE.equals(type.getCode()))
            .findFirst()
            .orElseGet(() -> {
                ResourceTypeEntity entity = new ResourceTypeEntity();
                entity.setId("rt-" + compactUuid());
                entity.setCode(WEB_LINK_TYPE_CODE);
                entity.setName("网络链接");
                entity.setIcon("link");
                entity.setDescription("由 URL 自动登记的外部网络链接");
                entity.setIdentityFieldKey("sourceUrl");
                entity.setIdentityFieldLabel("源 URL");
                return typeRepository.save(entity);
            });
    }

    private String deriveTitle(String label, String url) {
        if (label != null && !label.isBlank()) {
            return label.trim().length() > 255 ? label.trim().substring(0, 255) : label.trim();
        }
        Optional<String> fetched = pageTitleFetcherService.fetchTitle(url);
        if (fetched.isPresent()) {
            return fetched.get();
        }
        try {
            return new URI(url).getHost();
        } catch (Exception ex) {
            return url.length() > 255 ? url.substring(0, 255) : url;
        }
    }

    private String buildExcerptTitle(String anchor, String label) {
        if (label != null && !label.isBlank()) {
            String trimmed = label.trim();
            return trimmed.length() > 255 ? trimmed.substring(0, 255) : trimmed;
        }
        if (anchor.startsWith(":~:text=")) {
            String snippet = anchor.substring(":~:text=".length()).split(",")[0].trim();
            if (!snippet.isBlank()) {
                return snippet.length() > 255 ? snippet.substring(0, 255) : snippet;
            }
            return "文本片段";
        }
        if (anchor.isBlank()) {
            return "页面锚点";
        }
        return anchor.length() > 255 ? anchor.substring(0, 255) : anchor;
    }

    private String normalizeLocatorKey(String locator) {
        if (locator == null) {
            return "";
        }
        return locator.trim().replaceFirst("^#", "");
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String compactUuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private record ExcerptUpsert(ResourceExcerptDto dto, boolean created) {
    }

    private record ResolveItemResult(ResourceItemEntity item, boolean created) {
    }
}
