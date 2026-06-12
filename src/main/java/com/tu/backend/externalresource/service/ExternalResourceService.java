package com.tu.backend.externalresource.service;

import com.tu.backend.common.BusinessException;
import com.tu.backend.common.PageResponse;
import com.tu.backend.externalresource.dto.CreateResourceItemRelationRequest;
import com.tu.backend.externalresource.dto.CreateResourceItemRequest;
import com.tu.backend.externalresource.dto.CreateResourceChapterRequest;
import com.tu.backend.externalresource.dto.CreateResourceExcerptRequest;
import com.tu.backend.externalresource.dto.CreateResourceTypeRequest;
import com.tu.backend.externalresource.dto.CreateResourceWorkRequest;
import com.tu.backend.externalresource.dto.MergeResourceWorksRequest;
import com.tu.backend.externalresource.dto.ResourceChapterDto;
import com.tu.backend.externalresource.dto.ResourceExcerptDto;
import com.tu.backend.externalresource.dto.ResourceItemDto;
import com.tu.backend.externalresource.dto.ResourceItemRelationDto;
import com.tu.backend.externalresource.dto.ResourceTypeDto;
import com.tu.backend.externalresource.dto.ResourceWorkDto;
import com.tu.backend.externalresource.dto.UpdateResourceChapterRequest;
import com.tu.backend.externalresource.dto.UpdateResourceExcerptRequest;
import com.tu.backend.externalresource.dto.UpdateResourceItemRequest;
import com.tu.backend.externalresource.dto.UpdateResourceTypeRequest;
import com.tu.backend.externalresource.dto.UpdateResourceWorkRequest;
import com.tu.backend.contenttree.entity.ContentTreeNodeEntity;
import com.tu.backend.contenttree.entity.ScopeType;
import com.tu.backend.contenttree.service.ContentTreeNodeService;
import com.tu.backend.externalresource.entity.ResourceExcerptEntity;
import com.tu.backend.externalresource.entity.ResourceItemEntity;
import com.tu.backend.externalresource.entity.ResourceItemRelationEntity;
import com.tu.backend.externalresource.entity.ResourceTypeEntity;
import com.tu.backend.externalresource.entity.ResourceWorkEntity;
import com.tu.backend.externalresource.model.FieldSource;
import com.tu.backend.externalresource.model.VariantKind;
import com.tu.backend.externalresource.repository.ResourceExcerptRepository;
import com.tu.backend.externalresource.repository.ResourceItemRelationRepository;
import com.tu.backend.externalresource.repository.ResourceItemRepository;
import com.tu.backend.externalresource.repository.ResourceTypeRepository;
import com.tu.backend.externalresource.repository.ResourceWorkRepository;
import com.tu.backend.externalresource.util.ExternalUrlNormalizer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ExternalResourceService {

    private static final String BOOK_TYPE_CODE = "book";
    private static final String WEB_LINK_TYPE_CODE = "web-link";

    private final ResourceTypeRepository typeRepository;
    private final ResourceWorkRepository workRepository;
    private final ResourceItemRepository itemRepository;
    private final ResourceExcerptRepository excerptRepository;
    private final ContentTreeNodeService contentTreeNodeService;
    private final ResourceItemRelationRepository itemRelationRepository;
    private final UrlClusterMatcherService clusterMatcherService;

    public ExternalResourceService(
        ResourceTypeRepository typeRepository,
        ResourceWorkRepository workRepository,
        ResourceItemRepository itemRepository,
        ResourceExcerptRepository excerptRepository,
        ContentTreeNodeService contentTreeNodeService,
        ResourceItemRelationRepository itemRelationRepository,
        UrlClusterMatcherService clusterMatcherService
    ) {
        this.typeRepository = typeRepository;
        this.workRepository = workRepository;
        this.itemRepository = itemRepository;
        this.excerptRepository = excerptRepository;
        this.contentTreeNodeService = contentTreeNodeService;
        this.itemRelationRepository = itemRelationRepository;
        this.clusterMatcherService = clusterMatcherService;
    }

    @Transactional(readOnly = true)
    public PageResponse<ResourceTypeDto> listTypes(int page, int pageSize) {
        int safePage = Math.max(0, page);
        int safePageSize = Math.max(1, Math.min(pageSize, 200));
        Pageable pageable = PageRequest.of(
            safePage,
            safePageSize,
            Sort.by(Sort.Order.asc("name"), Sort.Order.asc("createdAt"))
        );
        Page<ResourceTypeEntity> entityPage = typeRepository.findAllByOrderByNameAscCreatedAtAsc(pageable);
        List<ResourceTypeDto> items = entityPage.getContent().stream().map(this::toTypeDto).toList();
        return PageResponse.of(items, entityPage.getTotalElements(), safePage, safePageSize);
    }

    @Transactional
    public ResourceTypeDto createType(CreateResourceTypeRequest request) {
        String code = normalizeCode(request.code());
        String name = normalizeRequired(request.name(), "resource type name required");
        String identityFieldKey = normalizeCode(request.identityFieldKey());
        String identityFieldLabel = normalizeRequired(request.identityFieldLabel(), "identity field label required");

        if (typeRepository.existsByCode(code)) {
            throw new BusinessException(40009, "resource type code already exists");
        }
        if (typeRepository.existsByName(name)) {
            throw new BusinessException(40009, "resource type name already exists");
        }

        ResourceTypeEntity entity = new ResourceTypeEntity();
        entity.setId("rt-" + compactUuid());
        entity.setCode(code);
        entity.setName(name);
        entity.setIcon(blankToNull(request.icon()));
        entity.setDescription(blankToNull(request.description()));
        entity.setIdentityFieldKey(identityFieldKey);
        entity.setIdentityFieldLabel(identityFieldLabel);
        return toTypeDto(typeRepository.save(entity));
    }

    @Transactional
    public ResourceTypeDto updateType(String id, UpdateResourceTypeRequest request) {
        ResourceTypeEntity entity = findType(id);
        String name = normalizeRequired(request.name(), "resource type name required");
        String identityFieldKey = normalizeCode(request.identityFieldKey());
        String identityFieldLabel = normalizeRequired(request.identityFieldLabel(), "identity field label required");

        if (!entity.getName().equals(name) && typeRepository.existsByName(name)) {
            throw new BusinessException(40009, "resource type name already exists");
        }

        entity.setName(name);
        entity.setIcon(blankToNull(request.icon()));
        entity.setDescription(blankToNull(request.description()));
        entity.setIdentityFieldKey(identityFieldKey);
        entity.setIdentityFieldLabel(identityFieldLabel);
        return toTypeDto(typeRepository.save(entity));
    }

    @Transactional
    public void deleteType(String id) {
        ResourceTypeEntity entity = findType(id);
        if (workRepository.existsByTypeId(id) || itemRepository.existsByTypeId(id)) {
            throw new BusinessException(40009, "resource type is in use");
        }
        typeRepository.delete(entity);
    }

    @Transactional(readOnly = true)
    public PageResponse<ResourceWorkDto> listWorks(String typeId, int page, int pageSize) {
        Map<String, ResourceTypeEntity> types = loadTypeMap();
        int safePage = Math.max(0, page);
        int safePageSize = Math.max(1, Math.min(pageSize, 200));
        Pageable pageable = PageRequest.of(
            safePage,
            safePageSize,
            Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.desc("createdAt"))
        );
        Page<ResourceWorkEntity> entityPage = isBlank(typeId)
            ? workRepository.findAllByOrderByUpdatedAtDescCreatedAtDesc(pageable)
            : workRepository.findByTypeIdOrderByUpdatedAtDescCreatedAtDesc(typeId.trim(), pageable);
        List<ResourceWorkDto> items = entityPage.getContent().stream()
            .map(work -> toWorkDto(work, types.get(work.getTypeId())))
            .toList();
        return PageResponse.of(items, entityPage.getTotalElements(), safePage, safePageSize);
    }

    @Transactional
    public ResourceWorkDto createWork(CreateResourceWorkRequest request) {
        ResourceTypeEntity type = findType(request.typeId());
        ResourceWorkEntity entity = new ResourceWorkEntity();
        entity.setId("rw-" + compactUuid());
        entity.setTypeId(type.getId());
        entity.setTitle(normalizeRequired(request.title(), "resource work title required"));
        entity.setSubtitle(blankToNull(request.subtitle()));
        entity.setDescription(blankToNull(request.description()));
        entity.setClusterKey(blankToNull(request.clusterKey()));
        entity.setTitleSource(FieldSource.orAuto(request.titleSource()));
        return toWorkDto(workRepository.save(entity), type);
    }

    @Transactional
    public ResourceWorkDto updateWork(String id, UpdateResourceWorkRequest request) {
        ResourceWorkEntity entity = findWork(id);
        ResourceTypeEntity type = findType(request.typeId());
        if (!entity.getTypeId().equals(type.getId()) && itemRepository.existsByWorkId(id)) {
            throw new BusinessException(40009, "resource work type cannot change while items exist");
        }
        entity.setTypeId(type.getId());
        entity.setTitle(normalizeRequired(request.title(), "resource work title required"));
        entity.setSubtitle(blankToNull(request.subtitle()));
        entity.setDescription(blankToNull(request.description()));
        if (request.clusterKey() != null) {
            entity.setClusterKey(blankToNull(request.clusterKey()));
        }
        if (request.titleSource() != null) {
            entity.setTitleSource(FieldSource.orAuto(request.titleSource()));
        }
        return toWorkDto(workRepository.save(entity), type);
    }

    @Transactional
    public ResourceWorkDto mergeWorks(MergeResourceWorksRequest request) {
        if (request.sourceWorkId().equals(request.targetWorkId())) {
            throw new BusinessException(40000, "cannot merge a work into itself");
        }
        ResourceWorkEntity source = findWork(request.sourceWorkId());
        ResourceWorkEntity target = findWork(request.targetWorkId());
        if (!source.getTypeId().equals(target.getTypeId())) {
            throw new BusinessException(40000, "resource works must share the same type");
        }
        List<ResourceItemEntity> items = itemRepository.findByWorkIdOrderByUpdatedAtDescCreatedAtDesc(source.getId());
        for (ResourceItemEntity item : items) {
            item.setWorkId(target.getId());
            item.setWorkIdSource(FieldSource.MANUAL);
            itemRepository.save(item);
        }
        workRepository.delete(source);
        return toWorkDto(target, findType(target.getTypeId()));
    }

    @Transactional
    public ResourceItemDto splitItemToNewWork(String itemId) {
        ResourceItemEntity item = findItem(itemId);
        ResourceTypeEntity type = findType(item.getTypeId());
        ResourceWorkEntity work = new ResourceWorkEntity();
        work.setId("rw-" + compactUuid());
        work.setTypeId(type.getId());
        work.setTitle(item.getTitle());
        work.setTitleSource(FieldSource.MANUAL);
        work.setClusterKey("single|" + blankToNull(item.getIdentityValue()));
        work.setDescription("由资源实体拆分创建");
        work = workRepository.save(work);
        item.setWorkId(work.getId());
        item.setWorkIdSource(FieldSource.MANUAL);
        return toItemDto(itemRepository.save(item), type, work);
    }

    @Transactional
    public ResourceItemDto resetItemAutoFields(String itemId) {
        ResourceItemEntity item = findItem(itemId);
        ResourceTypeEntity type = findType(item.getTypeId());
        item.setTitleSource(FieldSource.AUTO);
        item.setWorkIdSource(FieldSource.AUTO);

        String identity = blankToNull(item.getIdentityValue());
        if (identity != null && WEB_LINK_TYPE_CODE.equals(type.getCode())) {
            String baseUrl = ExternalUrlNormalizer.toBasePageUrl(identity);
            if (baseUrl != null) {
                identity = baseUrl;
                item.setIdentityValue(baseUrl);
                item.setSourceUrl(baseUrl);
            }
            Optional<UrlClusterMatcherService.ClusterMatch> clusterMatch = clusterMatcherService.match(identity);
            ResourceWorkEntity work = resolveWorkForAutoReset(type, item, identity, clusterMatch);
            item.setWorkId(work.getId());
            if (clusterMatch.map(UrlClusterMatcherService.ClusterMatch::variantHint).orElse(null) != null) {
                item.setEdition(clusterMatch.get().variantHint());
                item.setVariantKind(VariantKind.OTHER.code());
            }
        }
        return toItemDto(itemRepository.save(item), type, findOptionalWork(item.getWorkId()));
    }

    @Transactional
    public void deleteWork(String id) {
        ResourceWorkEntity entity = findWork(id);
        if (itemRepository.existsByWorkId(id)) {
            throw new BusinessException(40009, "resource work is in use");
        }
        workRepository.delete(entity);
    }

    @Transactional(readOnly = true)
    public PageResponse<ResourceItemDto> listItems(String typeId, String workId, String identityValue, int page, int pageSize) {
        Map<String, ResourceTypeEntity> types = loadTypeMap();
        Map<String, ResourceWorkEntity> works = loadWorkMap();
        if (!isBlank(identityValue)) {
            if (isBlank(typeId)) {
                throw new BusinessException(40000, "typeId is required when identityValue is used");
            }
            List<ResourceItemDto> matches = itemRepository.findByTypeIdAndIdentityValue(typeId.trim(), identityValue.trim())
                .stream()
                .map(item -> toItemDto(item, types.get(item.getTypeId()), works.get(item.getWorkId())))
                .toList();
            return PageResponse.of(matches, matches.size(), 0, Math.max(1, matches.size()));
        }

        int safePage = Math.max(0, page);
        int safePageSize = Math.max(1, Math.min(pageSize, 200));
        Pageable pageable = PageRequest.of(
            safePage,
            safePageSize,
            Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.desc("createdAt"))
        );
        Page<ResourceItemEntity> entityPage;
        if (!isBlank(typeId) && !isBlank(workId)) {
            entityPage = itemRepository.findByTypeIdAndWorkIdOrderByUpdatedAtDescCreatedAtDesc(
                typeId.trim(),
                workId.trim(),
                pageable
            );
        } else if (!isBlank(typeId)) {
            entityPage = itemRepository.findByTypeIdOrderByUpdatedAtDescCreatedAtDesc(typeId.trim(), pageable);
        } else if (!isBlank(workId)) {
            entityPage = itemRepository.findByWorkIdOrderByUpdatedAtDescCreatedAtDesc(workId.trim(), pageable);
        } else {
            entityPage = itemRepository.findAllByOrderByUpdatedAtDescCreatedAtDesc(pageable);
        }
        List<ResourceItemDto> items = entityPage.getContent().stream()
            .map(item -> toItemDto(item, types.get(item.getTypeId()), works.get(item.getWorkId())))
            .toList();
        return PageResponse.of(items, entityPage.getTotalElements(), safePage, safePageSize);
    }

    @Transactional(readOnly = true)
    public ResourceItemDto getItem(String id) {
        ResourceItemEntity entity = findItem(id);
        return toItemDto(entity, findType(entity.getTypeId()), findOptionalWork(entity.getWorkId()));
    }

    @Transactional
    public ResourceItemDto createItem(CreateResourceItemRequest request) {
        ResourceTypeEntity type = findType(request.typeId());
        ResourceWorkEntity work = findOptionalWork(request.workId());
        if (work != null) {
            ensureWorkTypeMatches(type, work);
        }
        String identityValue = blankToNull(request.identityValue());
        if (identityValue != null && itemRepository.findByTypeIdAndIdentityValue(type.getId(), identityValue).isPresent()) {
            throw new BusinessException(40009, "resource item identity already exists");
        }

        ResourceItemEntity entity = new ResourceItemEntity();
        entity.setId("ri-" + compactUuid());
        fillItem(entity, type, work, request);
        return toItemDto(itemRepository.save(entity), type, work);
    }

    @Transactional
    public ResourceItemDto updateItem(String id, UpdateResourceItemRequest request) {
        ResourceItemEntity entity = findItem(id);
        ResourceTypeEntity type = findType(request.typeId());
        ResourceWorkEntity work = findOptionalWork(request.workId());
        if (work != null) {
            ensureWorkTypeMatches(type, work);
        }
        String identityValue = blankToNull(request.identityValue());
        if (identityValue != null) {
            itemRepository.findByTypeIdAndIdentityValue(type.getId(), identityValue)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BusinessException(40009, "resource item identity already exists");
                });
        }

        fillItem(entity, type, work, request);
        return toItemDto(itemRepository.save(entity), type, work);
    }

    @Transactional
    public void removeItem(String id) {
        excerptRepository.deleteByResourceItemId(id);
        contentTreeNodeService.deleteResourceScope(id);
        itemRelationRepository.deleteByFromItemIdOrToItemId(id, id);
        itemRepository.delete(findItem(id));
    }

    @Transactional(readOnly = true)
    public List<ResourceChapterDto> listChapters(String resourceItemId) {
        ResourceItemEntity item = findItem(resourceItemId);
        ensureBookItem(item);
        return contentTreeNodeService.listResourceItemNodes(item.getId())
            .stream()
            .map(chapter -> toChapterDto(chapter, item))
            .toList();
    }

    @Transactional(readOnly = true)
    public ResourceChapterDto getChapter(String id) {
        ContentTreeNodeEntity chapter = findChapter(id);
        ResourceItemEntity item = findItem(chapter.getScopeId());
        ensureBookItem(item);
        return toChapterDto(chapter, item);
    }

    @Transactional
    public ResourceChapterDto createChapter(String resourceItemId, CreateResourceChapterRequest request) {
        ResourceItemEntity item = findItem(resourceItemId);
        ensureBookItem(item);
        String parentId = blankToNull(request.parentId());
        validateChapterParent(item.getId(), parentId, null);

        ContentTreeNodeEntity entity = new ContentTreeNodeEntity();
        entity.setId("rc-" + compactUuid());
        entity.setScopeId(item.getId());
        fillChapter(entity, parentId, request.title(), request.locator(), request.note(), request.sortOrder(), item.getId());
        return toChapterDto(contentTreeNodeService.saveResourceNode(entity), item);
    }

    @Transactional
    public ResourceChapterDto updateChapter(String id, UpdateResourceChapterRequest request) {
        ContentTreeNodeEntity entity = findChapter(id);
        ResourceItemEntity item = findItem(entity.getScopeId());
        ensureBookItem(item);
        String parentId = blankToNull(request.parentId());
        validateChapterParent(item.getId(), parentId, entity.getId());
        fillChapter(entity, parentId, request.title(), request.locator(), request.note(), request.sortOrder(), item.getId());
        return toChapterDto(contentTreeNodeService.saveResourceNode(entity), item);
    }

    @Transactional
    public void deleteChapter(String id) {
        ContentTreeNodeEntity chapter = findChapter(id);
        ResourceItemEntity item = findItem(chapter.getScopeId());
        ensureBookItem(item);
        Set<String> chapterIds = collectChapterDescendantIds(item.getId(), chapter.getId());
        excerptRepository.findByResourceItemId(item.getId()).stream()
            .filter(excerpt -> excerpt.getChapterId() != null && chapterIds.contains(excerpt.getChapterId()))
            .forEach(excerpt -> {
                excerpt.setChapterId(null);
                excerptRepository.save(excerpt);
            });
        contentTreeNodeService.deleteResourceNodes(chapterIds);
    }

    @Transactional(readOnly = true)
    public List<ResourceItemRelationDto> listItemRelations(String itemId) {
        findItem(itemId);
        Map<String, ResourceItemEntity> items = itemRepository.findAll().stream()
            .collect(Collectors.toMap(ResourceItemEntity::getId, Function.identity()));
        return itemRelationRepository.findByFromItemIdOrToItemIdOrderByCreatedAtAsc(itemId, itemId)
            .stream()
            .map(relation -> toRelationDto(relation, items))
            .toList();
    }

    @Transactional
    public ResourceItemRelationDto createItemRelation(CreateResourceItemRelationRequest request) {
        if (request.fromItemId().equals(request.toItemId())) {
            throw new BusinessException(40000, "cannot relate an item to itself");
        }
        ResourceItemEntity from = findItem(request.fromItemId());
        ResourceItemEntity to = findItem(request.toItemId());
        if (!from.getTypeId().equals(to.getTypeId())) {
            throw new BusinessException(40000, "related items must share the same resource type");
        }
        String relationType = normalizeRelationType(request.relationType());

        ResourceItemRelationEntity entity = new ResourceItemRelationEntity();
        entity.setId("rir-" + compactUuid());
        entity.setFromItemId(from.getId());
        entity.setToItemId(to.getId());
        entity.setRelationType(relationType);
        entity.setNote(blankToNull(request.note()));
        entity = itemRelationRepository.save(entity);
        Map<String, ResourceItemEntity> items = Map.of(from.getId(), from, to.getId(), to);
        return toRelationDto(entity, items);
    }

    @Transactional
    public void deleteItemRelation(String relationId) {
        itemRelationRepository.delete(findItemRelation(relationId));
    }

    @Transactional(readOnly = true)
    public PageResponse<ResourceExcerptDto> listExcerpts(String resourceItemId, int page, int pageSize) {
        ResourceItemEntity item = findItem(resourceItemId);
        ensureExcerptSupportedItem(item);
        int safePage = Math.max(0, page);
        int safePageSize = Math.max(1, Math.min(pageSize, 200));
        Pageable pageable = PageRequest.of(
            safePage,
            safePageSize,
            Sort.by(Sort.Order.asc("sortOrder"), Sort.Order.asc("createdAt"))
        );
        Page<ResourceExcerptEntity> entityPage = excerptRepository
            .findByResourceItemIdOrderBySortOrderAscCreatedAtAsc(item.getId(), pageable);
        List<ResourceExcerptDto> items = entityPage.getContent().stream()
            .map(excerpt -> toExcerptDto(excerpt, item, loadChapterTitleMap(item.getId())))
            .toList();
        return PageResponse.of(items, entityPage.getTotalElements(), safePage, safePageSize);
    }

    @Transactional(readOnly = true)
    public ResourceExcerptDto getExcerpt(String id) {
        ResourceExcerptEntity excerpt = findExcerpt(id);
        ResourceItemEntity item = findItem(excerpt.getResourceItemId());
        ensureExcerptSupportedItem(item);
        return toExcerptDto(excerpt, item, loadChapterTitleMap(item.getId()));
    }

    @Transactional
    public ResourceExcerptDto createExcerpt(String resourceItemId, CreateResourceExcerptRequest request) {
        ResourceItemEntity item = findItem(resourceItemId);
        ensureExcerptSupportedItem(item);

        ResourceExcerptEntity entity = new ResourceExcerptEntity();
        entity.setId("re-" + compactUuid());
        entity.setResourceItemId(item.getId());
        fillExcerpt(
            entity,
            request.title(),
            request.chapterId(),
            request.locator(),
            request.excerptText(),
            request.note(),
            request.sortOrder(),
            item
        );
        return toExcerptDto(excerptRepository.save(entity), item, loadChapterTitleMap(item.getId()));
    }

    @Transactional
    public ResourceExcerptDto updateExcerpt(String id, UpdateResourceExcerptRequest request) {
        ResourceExcerptEntity entity = findExcerpt(id);
        ResourceItemEntity item = findItem(entity.getResourceItemId());
        ensureExcerptSupportedItem(item);
        fillExcerpt(
            entity,
            request.title(),
            request.chapterId(),
            request.locator(),
            request.excerptText(),
            request.note(),
            request.sortOrder(),
            item
        );
        return toExcerptDto(excerptRepository.save(entity), item, loadChapterTitleMap(item.getId()));
    }

    @Transactional
    public void deleteExcerpt(String id) {
        excerptRepository.delete(findExcerpt(id));
    }

    private void fillItem(ResourceItemEntity entity, ResourceTypeEntity type, ResourceWorkEntity work, CreateResourceItemRequest request) {
        entity.setTypeId(type.getId());
        entity.setWorkId(work == null ? null : work.getId());
        entity.setTitle(normalizeRequired(request.title(), "resource item title required"));
        String identityValue = blankToNull(request.identityValue());
        if (WEB_LINK_TYPE_CODE.equals(type.getCode()) && identityValue != null) {
            String baseUrl = ExternalUrlNormalizer.toBasePageUrl(identityValue);
            if (baseUrl != null) {
                identityValue = baseUrl;
            }
        }
        entity.setIdentityValue(identityValue);
        entity.setSourceUrl(blankToNull(request.sourceUrl()));
        entity.setEdition(blankToNull(request.edition()));
        entity.setNote(blankToNull(request.note()));
        entity.setTitleSource(FieldSource.orAuto(request.titleSource()));
        entity.setWorkIdSource(FieldSource.orAuto(request.workIdSource()));
        entity.setVariantKind(VariantKind.normalizeCode(request.variantKind()));
    }

    private void fillItem(ResourceItemEntity entity, ResourceTypeEntity type, ResourceWorkEntity work, UpdateResourceItemRequest request) {
        entity.setTypeId(type.getId());
        entity.setWorkId(work == null ? null : work.getId());
        entity.setTitle(normalizeRequired(request.title(), "resource item title required"));
        String identityValue = blankToNull(request.identityValue());
        if (WEB_LINK_TYPE_CODE.equals(type.getCode()) && identityValue != null) {
            String baseUrl = ExternalUrlNormalizer.toBasePageUrl(identityValue);
            if (baseUrl != null) {
                identityValue = baseUrl;
            }
        }
        entity.setIdentityValue(identityValue);
        entity.setSourceUrl(blankToNull(request.sourceUrl()));
        entity.setEdition(blankToNull(request.edition()));
        entity.setNote(blankToNull(request.note()));
        if (request.titleSource() != null) {
            entity.setTitleSource(FieldSource.orAuto(request.titleSource()));
        }
        if (request.workIdSource() != null) {
            entity.setWorkIdSource(FieldSource.orAuto(request.workIdSource()));
        }
        if (request.variantKind() != null) {
            entity.setVariantKind(VariantKind.normalizeCode(request.variantKind()));
        }
    }

    private ResourceWorkEntity resolveWorkForAutoReset(
        ResourceTypeEntity type,
        ResourceItemEntity item,
        String baseUrl,
        Optional<UrlClusterMatcherService.ClusterMatch> clusterMatch
    ) {
        if (clusterMatch.isPresent()) {
            String clusterKey = clusterMatch.get().clusterKey();
            return workRepository.findByTypeIdAndClusterKey(type.getId(), clusterKey)
                .orElseGet(() -> {
                    ResourceWorkEntity work = new ResourceWorkEntity();
                    work.setId("rw-" + compactUuid());
                    work.setTypeId(type.getId());
                    work.setTitle(item.getTitle());
                    work.setTitleSource(FieldSource.AUTO);
                    work.setClusterKey(clusterKey);
                    work.setDescription("URL 聚类：" + clusterKey);
                    return workRepository.save(work);
                });
        }
        return workRepository.findById(item.getWorkId())
            .orElseGet(() -> {
                ResourceWorkEntity work = new ResourceWorkEntity();
                work.setId("rw-" + compactUuid());
                work.setTypeId(type.getId());
                work.setTitle(item.getTitle());
                work.setTitleSource(FieldSource.AUTO);
                work.setClusterKey("single|" + baseUrl);
                work.setDescription("待归并：" + baseUrl);
                return workRepository.save(work);
            });
    }

    private ResourceItemRelationEntity findItemRelation(String id) {
        return itemRelationRepository.findById(id)
            .orElseThrow(() -> new BusinessException(40001, "resource item relation not found"));
    }

    private ResourceItemRelationDto toRelationDto(ResourceItemRelationEntity entity, Map<String, ResourceItemEntity> items) {
        ResourceItemEntity from = items.get(entity.getFromItemId());
        ResourceItemEntity to = items.get(entity.getToItemId());
        return new ResourceItemRelationDto(
            entity.getId(),
            entity.getFromItemId(),
            from == null ? "" : from.getTitle(),
            entity.getToItemId(),
            to == null ? "" : to.getTitle(),
            entity.getRelationType(),
            entity.getNote()
        );
    }

    private void ensureWorkTypeMatches(ResourceTypeEntity type, ResourceWorkEntity work) {
        if (!work.getTypeId().equals(type.getId())) {
            throw new BusinessException(40000, "resource work does not belong to resource type");
        }
    }

    private void ensureBookItem(ResourceItemEntity item) {
        ResourceTypeEntity type = findType(item.getTypeId());
        if (!BOOK_TYPE_CODE.equals(type.getCode())) {
            throw new BusinessException(40000, "resource chapters are only supported for book resources");
        }
    }

    private void ensureExcerptSupportedItem(ResourceItemEntity item) {
        ResourceTypeEntity type = findType(item.getTypeId());
        String code = type.getCode();
        if (!BOOK_TYPE_CODE.equals(code) && !WEB_LINK_TYPE_CODE.equals(code)) {
            throw new BusinessException(40000, "resource excerpts are only supported for book or web-link resources");
        }
    }

    private void fillExcerpt(
        ResourceExcerptEntity entity,
        String title,
        String chapterId,
        String locator,
        String excerptText,
        String note,
        Integer sortOrder,
        ResourceItemEntity item
    ) {
        entity.setTitle(normalizeRequired(title, "resource excerpt title required"));
        entity.setChapterId(resolveChapterForExcerpt(item, blankToNull(chapterId)));
        entity.setLocator(blankToNull(locator));
        entity.setExcerptText(blankToNull(excerptText));
        entity.setNote(blankToNull(note));
        entity.setSortOrder(sortOrder == null ? nextExcerptSortOrder(item.getId()) : Math.max(0, sortOrder));
    }

    private String resolveChapterForExcerpt(ResourceItemEntity item, String chapterId) {
        if (chapterId == null) {
            return null;
        }
        ContentTreeNodeEntity chapter = findChapter(chapterId);
        if (!item.getId().equals(chapter.getScopeId())) {
            throw new BusinessException(40000, "resource chapter does not belong to resource item");
        }
        ensureBookItem(item);
        return chapter.getId();
    }

    private void fillChapter(
        ContentTreeNodeEntity entity,
        String parentId,
        String title,
        String locator,
        String note,
        Integer sortOrder,
        String resourceItemId
    ) {
        entity.setScopeType(ScopeType.RESOURCE_ITEM);
        entity.setScopeId(resourceItemId);
        entity.setParentId(parentId);
        entity.setTitle(normalizeRequired(title, "resource chapter title required"));
        entity.setLocator(blankToNull(locator));
        entity.setNote(blankToNull(note));
        entity.setSortOrder(sortOrder == null ? nextChapterSortOrder(resourceItemId, parentId) : Math.max(0, sortOrder));
    }

    private int nextChapterSortOrder(String resourceItemId, String parentId) {
        return contentTreeNodeService.listResourceItemNodes(resourceItemId).stream()
            .filter(chapter -> {
                if (parentId == null) {
                    return chapter.getParentId() == null;
                }
                return parentId.equals(chapter.getParentId());
            })
            .map(ContentTreeNodeEntity::getSortOrder)
            .filter(order -> order != null)
            .max(Integer::compareTo)
            .map(order -> order + 1)
            .orElse(0);
    }

    private void validateChapterParent(String resourceItemId, String parentId, String selfId) {
        if (parentId == null) {
            return;
        }
        if (selfId != null && selfId.equals(parentId)) {
            throw new BusinessException(40000, "resource chapter cannot be its own parent");
        }
        ContentTreeNodeEntity parent = findChapter(parentId);
        if (!resourceItemId.equals(parent.getScopeId())) {
            throw new BusinessException(40000, "resource chapter parent must belong to the same resource item");
        }
        if (selfId != null) {
            Set<String> descendants = collectChapterDescendantIds(resourceItemId, selfId);
            if (descendants.contains(parentId)) {
                throw new BusinessException(40000, "resource chapter parent cannot be a descendant");
            }
        }
    }

    private Set<String> collectChapterDescendantIds(String resourceItemId, String rootId) {
        List<ContentTreeNodeEntity> chapters = contentTreeNodeService.listResourceItemNodes(resourceItemId);
        Map<String, List<ContentTreeNodeEntity>> byParent = chapters.stream()
            .collect(Collectors.groupingBy(chapter -> chapter.getParentId() == null ? "" : chapter.getParentId()));
        Set<String> result = new HashSet<>();
        collectChapterDescendantIdsRecursive(rootId, byParent, result);
        return result;
    }

    private void collectChapterDescendantIdsRecursive(
        String chapterId,
        Map<String, List<ContentTreeNodeEntity>> byParent,
        Set<String> result
    ) {
        result.add(chapterId);
        for (ContentTreeNodeEntity child : byParent.getOrDefault(chapterId, List.of())) {
            collectChapterDescendantIdsRecursive(child.getId(), byParent, result);
        }
    }

    private Map<String, String> loadChapterTitleMap(String resourceItemId) {
        return contentTreeNodeService.listResourceItemNodes(resourceItemId).stream()
            .collect(Collectors.toMap(ContentTreeNodeEntity::getId, ContentTreeNodeEntity::getTitle));
    }

    private int nextExcerptSortOrder(String resourceItemId) {
        return excerptRepository.findByResourceItemId(resourceItemId).stream()
            .map(ResourceExcerptEntity::getSortOrder)
            .filter(order -> order != null)
            .max(Integer::compareTo)
            .map(order -> order + 1)
            .orElse(0);
    }

    private ResourceTypeEntity findType(String id) {
        if (isBlank(id)) {
            throw new BusinessException(40000, "resource type id required");
        }
        return typeRepository.findById(id.trim())
            .orElseThrow(() -> new BusinessException(40001, "resource type not found"));
    }

    private ResourceWorkEntity findWork(String id) {
        if (isBlank(id)) {
            throw new BusinessException(40000, "resource work id required");
        }
        return workRepository.findById(id.trim())
            .orElseThrow(() -> new BusinessException(40001, "resource work not found"));
    }

    private ResourceWorkEntity findOptionalWork(String id) {
        return isBlank(id) ? null : findWork(id);
    }

    private ResourceItemEntity findItem(String id) {
        if (isBlank(id)) {
            throw new BusinessException(40000, "resource item id required");
        }
        return itemRepository.findById(id.trim())
            .orElseThrow(() -> new BusinessException(40001, "resource item not found"));
    }

    private ContentTreeNodeEntity findChapter(String id) {
        if (isBlank(id)) {
            throw new BusinessException(40000, "resource chapter id required");
        }
        return contentTreeNodeService.findResourceNode(id.trim());
    }

    private ResourceExcerptEntity findExcerpt(String id) {
        if (isBlank(id)) {
            throw new BusinessException(40000, "resource excerpt id required");
        }
        return excerptRepository.findById(id.trim())
            .orElseThrow(() -> new BusinessException(40001, "resource excerpt not found"));
    }

    private Map<String, ResourceTypeEntity> loadTypeMap() {
        return typeRepository.findAll().stream().collect(Collectors.toMap(ResourceTypeEntity::getId, Function.identity()));
    }

    private Map<String, ResourceWorkEntity> loadWorkMap() {
        return workRepository.findAll().stream().collect(Collectors.toMap(ResourceWorkEntity::getId, Function.identity()));
    }

    private ResourceTypeDto toTypeDto(ResourceTypeEntity entity) {
        return new ResourceTypeDto(
            entity.getId(),
            entity.getCode(),
            entity.getName(),
            entity.getIcon(),
            entity.getDescription(),
            entity.getIdentityFieldKey(),
            entity.getIdentityFieldLabel()
        );
    }

    private ResourceWorkDto toWorkDto(ResourceWorkEntity entity, ResourceTypeEntity type) {
        return new ResourceWorkDto(
            entity.getId(),
            entity.getTypeId(),
            type == null ? "" : type.getName(),
            entity.getTitle(),
            entity.getSubtitle(),
            entity.getDescription(),
            entity.getClusterKey(),
            entity.getTitleSource()
        );
    }

    private ResourceItemDto toItemDto(ResourceItemEntity entity, ResourceTypeEntity type, ResourceWorkEntity work) {
        return new ResourceItemDto(
            entity.getId(),
            entity.getTypeId(),
            type == null ? "" : type.getName(),
            type == null ? "" : type.getIdentityFieldKey(),
            type == null ? "" : type.getIdentityFieldLabel(),
            entity.getWorkId(),
            work == null ? "" : work.getTitle(),
            entity.getTitle(),
            entity.getIdentityValue(),
            entity.getSourceUrl(),
            entity.getEdition(),
            entity.getNote(),
            entity.getTitleSource(),
            entity.getWorkIdSource(),
            entity.getVariantKind()
        );
    }

    private ResourceChapterDto toChapterDto(ContentTreeNodeEntity entity, ResourceItemEntity item) {
        return new ResourceChapterDto(
            entity.getId(),
            entity.getScopeId(),
            item == null ? "" : item.getTitle(),
            entity.getParentId(),
            entity.getTitle(),
            entity.getLocator(),
            entity.getNote(),
            entity.getSortOrder()
        );
    }

    private ResourceExcerptDto toExcerptDto(
        ResourceExcerptEntity entity,
        ResourceItemEntity item,
        Map<String, String> chapterTitles
    ) {
        String chapterId = entity.getChapterId();
        return new ResourceExcerptDto(
            entity.getId(),
            entity.getResourceItemId(),
            item == null ? "" : item.getTitle(),
            chapterId,
            chapterId == null ? null : chapterTitles.get(chapterId),
            entity.getTitle(),
            entity.getLocator(),
            entity.getExcerptText(),
            entity.getNote(),
            entity.getSortOrder()
        );
    }

    private String normalizeCode(String value) {
        String normalized = normalizeRequired(value, "code required").toLowerCase();
        if (!normalized.matches("[a-z][a-z0-9_-]*")) {
            throw new BusinessException(40000, "code must start with a letter and contain only letters, numbers, underscore or hyphen");
        }
        return normalized;
    }

    private String normalizeRequired(String value, String message) {
        if (isBlank(value)) {
            throw new BusinessException(40000, message);
        }
        return value.trim();
    }

    private String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String normalizeRelationType(String value) {
        String normalized = normalizeRequired(value, "relation type required").toLowerCase();
        if (!normalized.matches("[a-z][a-z0-9_]*")) {
            throw new BusinessException(40000, "invalid relation type");
        }
        return normalized;
    }

    private String compactUuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
