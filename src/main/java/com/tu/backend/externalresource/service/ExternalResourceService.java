package com.tu.backend.externalresource.service;

import com.tu.backend.common.BusinessException;
import com.tu.backend.externalresource.dto.CreateResourceItemRequest;
import com.tu.backend.externalresource.dto.CreateResourceExcerptRequest;
import com.tu.backend.externalresource.dto.CreateResourceTypeRequest;
import com.tu.backend.externalresource.dto.CreateResourceWorkRequest;
import com.tu.backend.externalresource.dto.ResourceExcerptDto;
import com.tu.backend.externalresource.dto.ResourceItemDto;
import com.tu.backend.externalresource.dto.ResourceTypeDto;
import com.tu.backend.externalresource.dto.ResourceWorkDto;
import com.tu.backend.externalresource.dto.UpdateResourceExcerptRequest;
import com.tu.backend.externalresource.dto.UpdateResourceItemRequest;
import com.tu.backend.externalresource.dto.UpdateResourceTypeRequest;
import com.tu.backend.externalresource.dto.UpdateResourceWorkRequest;
import com.tu.backend.externalresource.entity.ResourceExcerptEntity;
import com.tu.backend.externalresource.entity.ResourceItemEntity;
import com.tu.backend.externalresource.entity.ResourceTypeEntity;
import com.tu.backend.externalresource.entity.ResourceWorkEntity;
import com.tu.backend.externalresource.repository.ResourceExcerptRepository;
import com.tu.backend.externalresource.repository.ResourceItemRepository;
import com.tu.backend.externalresource.repository.ResourceTypeRepository;
import com.tu.backend.externalresource.repository.ResourceWorkRepository;
import com.tu.backend.reference.service.ReferenceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ExternalResourceService {

    private static final String BOOK_TYPE_CODE = "book";

    private final ResourceTypeRepository typeRepository;
    private final ResourceWorkRepository workRepository;
    private final ResourceItemRepository itemRepository;
    private final ResourceExcerptRepository excerptRepository;
    private final ReferenceService referenceService;

    public ExternalResourceService(
        ResourceTypeRepository typeRepository,
        ResourceWorkRepository workRepository,
        ResourceItemRepository itemRepository,
        ResourceExcerptRepository excerptRepository,
        ReferenceService referenceService
    ) {
        this.typeRepository = typeRepository;
        this.workRepository = workRepository;
        this.itemRepository = itemRepository;
        this.excerptRepository = excerptRepository;
        this.referenceService = referenceService;
    }

    @Transactional(readOnly = true)
    public List<ResourceTypeDto> listTypes() {
        return typeRepository.findAllByOrderByNameAscCreatedAtAsc()
            .stream()
            .map(this::toTypeDto)
            .toList();
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
    public List<ResourceWorkDto> listWorks(String typeId) {
        Map<String, ResourceTypeEntity> types = loadTypeMap();
        List<ResourceWorkEntity> works = isBlank(typeId)
            ? workRepository.findAllByOrderByUpdatedAtDescCreatedAtDesc()
            : workRepository.findByTypeIdOrderByUpdatedAtDescCreatedAtDesc(typeId.trim());
        return works.stream().map(work -> toWorkDto(work, types.get(work.getTypeId()))).toList();
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
        return toWorkDto(workRepository.save(entity), type);
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
    public List<ResourceItemDto> listItems(String typeId, String workId, String identityValue) {
        Map<String, ResourceTypeEntity> types = loadTypeMap();
        Map<String, ResourceWorkEntity> works = loadWorkMap();
        if (!isBlank(identityValue)) {
            if (isBlank(typeId)) {
                throw new BusinessException(40000, "typeId is required when identityValue is used");
            }
            return itemRepository.findByTypeIdAndIdentityValue(typeId.trim(), identityValue.trim())
                .stream()
                .map(item -> toItemDto(item, types.get(item.getTypeId()), works.get(item.getWorkId())))
                .toList();
        }

        List<ResourceItemEntity> items;
        if (!isBlank(typeId) && !isBlank(workId)) {
            items = itemRepository.findByTypeIdAndWorkIdOrderByUpdatedAtDescCreatedAtDesc(typeId.trim(), workId.trim());
        } else if (!isBlank(typeId)) {
            items = itemRepository.findByTypeIdOrderByUpdatedAtDescCreatedAtDesc(typeId.trim());
        } else if (!isBlank(workId)) {
            items = itemRepository.findByWorkIdOrderByUpdatedAtDescCreatedAtDesc(workId.trim());
        } else {
            items = itemRepository.findAllByOrderByUpdatedAtDescCreatedAtDesc();
        }
        return items.stream().map(item -> toItemDto(item, types.get(item.getTypeId()), works.get(item.getWorkId()))).toList();
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
        fillItem(entity, type, work, request.title(), identityValue, request.sourceUrl(), request.edition(), request.note());
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

        fillItem(entity, type, work, request.title(), identityValue, request.sourceUrl(), request.edition(), request.note());
        return toItemDto(itemRepository.save(entity), type, work);
    }

    @Transactional
    public void deleteItem(String id) {
        if (referenceService.hasResourceItemReferences(id)) {
            throw new BusinessException(40009, "resource item is in use");
        }
        excerptRepository.deleteByResourceItemId(id);
        itemRepository.delete(findItem(id));
    }

    @Transactional(readOnly = true)
    public List<ResourceExcerptDto> listExcerpts(String resourceItemId) {
        ResourceItemEntity item = findItem(resourceItemId);
        ensureBookItem(item);
        return excerptRepository.findByResourceItemIdOrderBySortOrderAscCreatedAtAsc(item.getId())
            .stream()
            .map(excerpt -> toExcerptDto(excerpt, item))
            .toList();
    }

    @Transactional(readOnly = true)
    public ResourceExcerptDto getExcerpt(String id) {
        ResourceExcerptEntity excerpt = findExcerpt(id);
        ResourceItemEntity item = findItem(excerpt.getResourceItemId());
        ensureBookItem(item);
        return toExcerptDto(excerpt, item);
    }

    @Transactional
    public ResourceExcerptDto createExcerpt(String resourceItemId, CreateResourceExcerptRequest request) {
        ResourceItemEntity item = findItem(resourceItemId);
        ensureBookItem(item);

        ResourceExcerptEntity entity = new ResourceExcerptEntity();
        entity.setId("re-" + compactUuid());
        entity.setResourceItemId(item.getId());
        fillExcerpt(entity, request.title(), request.locator(), request.excerptText(), request.note(), request.sortOrder(), item.getId());
        return toExcerptDto(excerptRepository.save(entity), item);
    }

    @Transactional
    public ResourceExcerptDto updateExcerpt(String id, UpdateResourceExcerptRequest request) {
        ResourceExcerptEntity entity = findExcerpt(id);
        ResourceItemEntity item = findItem(entity.getResourceItemId());
        ensureBookItem(item);
        fillExcerpt(entity, request.title(), request.locator(), request.excerptText(), request.note(), request.sortOrder(), item.getId());
        return toExcerptDto(excerptRepository.save(entity), item);
    }

    @Transactional
    public void deleteExcerpt(String id) {
        excerptRepository.delete(findExcerpt(id));
    }

    private void fillItem(
        ResourceItemEntity entity,
        ResourceTypeEntity type,
        ResourceWorkEntity work,
        String title,
        String identityValue,
        String sourceUrl,
        String edition,
        String note
    ) {
        entity.setTypeId(type.getId());
        entity.setWorkId(work == null ? null : work.getId());
        entity.setTitle(normalizeRequired(title, "resource item title required"));
        entity.setIdentityValue(identityValue);
        entity.setSourceUrl(blankToNull(sourceUrl));
        entity.setEdition(blankToNull(edition));
        entity.setNote(blankToNull(note));
    }

    private void ensureWorkTypeMatches(ResourceTypeEntity type, ResourceWorkEntity work) {
        if (!work.getTypeId().equals(type.getId())) {
            throw new BusinessException(40000, "resource work does not belong to resource type");
        }
    }

    private void ensureBookItem(ResourceItemEntity item) {
        ResourceTypeEntity type = findType(item.getTypeId());
        if (!BOOK_TYPE_CODE.equals(type.getCode())) {
            throw new BusinessException(40000, "resource excerpts are only supported for book resources");
        }
    }

    private void fillExcerpt(
        ResourceExcerptEntity entity,
        String title,
        String locator,
        String excerptText,
        String note,
        Integer sortOrder,
        String resourceItemId
    ) {
        entity.setTitle(normalizeRequired(title, "resource excerpt title required"));
        entity.setLocator(blankToNull(locator));
        entity.setExcerptText(normalizeRequired(excerptText, "resource excerpt text required"));
        entity.setNote(blankToNull(note));
        entity.setSortOrder(sortOrder == null ? nextExcerptSortOrder(resourceItemId) : Math.max(0, sortOrder));
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
            entity.getDescription()
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
            entity.getNote()
        );
    }

    private ResourceExcerptDto toExcerptDto(ResourceExcerptEntity entity, ResourceItemEntity item) {
        return new ResourceExcerptDto(
            entity.getId(),
            entity.getResourceItemId(),
            item == null ? "" : item.getTitle(),
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

    private String compactUuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
