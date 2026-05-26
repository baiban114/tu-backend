package com.tu.backend.taskintegration;

import com.tu.backend.common.BusinessException;
import com.tu.backend.externalresource.entity.ResourceItemEntity;
import com.tu.backend.externalresource.entity.ResourceTypeEntity;
import com.tu.backend.externalresource.entity.ResourceWorkEntity;
import com.tu.backend.externalresource.repository.ResourceItemRepository;
import com.tu.backend.externalresource.repository.ResourceTypeRepository;
import com.tu.backend.externalresource.repository.ResourceWorkRepository;
import com.tu.backend.taskintegration.dto.CreateExternalTaskRequest;
import com.tu.backend.taskintegration.dto.CreateTaskRelationRequest;
import com.tu.backend.taskintegration.dto.ExternalProjectDto;
import com.tu.backend.taskintegration.dto.ExternalProviderDto;
import com.tu.backend.taskintegration.dto.ExternalTaskDto;
import com.tu.backend.taskintegration.dto.IntegrationConnectionDto;
import com.tu.backend.taskintegration.dto.IntegrationConnectionTestResultDto;
import com.tu.backend.taskintegration.dto.MoveExternalTaskRequest;
import com.tu.backend.taskintegration.dto.TaskRelationDto;
import com.tu.backend.taskintegration.dto.UpdateIntegrationConnectionRequest;
import com.tu.backend.taskintegration.dto.UpdateExternalTaskRequest;
import com.tu.backend.taskintegration.entity.ExternalResourceRelationEntity;
import com.tu.backend.taskintegration.entity.IntegrationConnectionEntity;
import com.tu.backend.taskintegration.repository.ExternalResourceRelationRepository;
import com.tu.backend.taskintegration.repository.IntegrationConnectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class TaskIntegrationService {

    private static final String PROVIDER = "kaneo";
    private static final String TASK_TYPE_CODE = "kaneo-task";

    private final IntegrationClient integrationClient;
    private final ResourceTypeRepository typeRepository;
    private final ResourceWorkRepository workRepository;
    private final ResourceItemRepository itemRepository;
    private final ExternalResourceRelationRepository relationRepository;
    private final IntegrationConnectionRepository connectionRepository;

    public TaskIntegrationService(
        IntegrationClient integrationClient,
        ResourceTypeRepository typeRepository,
        ResourceWorkRepository workRepository,
        ResourceItemRepository itemRepository,
        ExternalResourceRelationRepository relationRepository,
        IntegrationConnectionRepository connectionRepository
    ) {
        this.integrationClient = integrationClient;
        this.typeRepository = typeRepository;
        this.workRepository = workRepository;
        this.itemRepository = itemRepository;
        this.relationRepository = relationRepository;
        this.connectionRepository = connectionRepository;
    }

    @Transactional(readOnly = true)
    public List<ExternalProviderDto> providers() {
        KaneoConnectionContext connection = requireConnection(false);
        return List.of(new ExternalProviderDto("kaneo", "Kaneo", "MIT", connection.configured()));
    }

    @Transactional(readOnly = true)
    public IntegrationConnectionDto getConnection() {
        return defaultConnectionEntity()
            .map(this::toConnectionDto)
            .orElseGet(this::emptyConnectionDto);
    }

    @Transactional(readOnly = true)
    public List<IntegrationConnectionDto> listConnections() {
        return connectionRepository.findByProviderOrProviderIsNullOrderByUpdatedAtDescCreatedAtDesc(PROVIDER)
            .stream()
            .map(this::toConnectionDto)
            .toList();
    }

    @Transactional
    public IntegrationConnectionDto createConnection(UpdateIntegrationConnectionRequest request) {
        IntegrationConnectionEntity entity = new IntegrationConnectionEntity();
        entity.setId("tic-" + compactUuid());
        entity.setProvider(PROVIDER);
        applyConnectionRequest(entity, request);
        return toConnectionDto(connectionRepository.save(entity));
    }

    @Transactional
    public IntegrationConnectionDto updateConnection(UpdateIntegrationConnectionRequest request) {
        IntegrationConnectionEntity entity = defaultConnectionEntity().orElseGet(() -> {
            IntegrationConnectionEntity created = new IntegrationConnectionEntity();
            created.setId("tic-" + compactUuid());
            created.setProvider(PROVIDER);
            return created;
        });
        applyConnectionRequest(entity, request);
        return toConnectionDto(connectionRepository.save(entity));
    }

    @Transactional
    public IntegrationConnectionDto updateConnection(String id, UpdateIntegrationConnectionRequest request) {
        IntegrationConnectionEntity entity = connectionRepository.findById(normalizeRequired(id, "connection id required"))
            .orElseThrow(() -> new BusinessException(40001, "Kaneo connection not found"));
        applyConnectionRequest(entity, request);
        return toConnectionDto(connectionRepository.save(entity));
    }

    @Transactional
    public void deleteConnection(String id) {
        connectionRepository.deleteById(normalizeRequired(id, "connection id required"));
    }

    @Transactional(readOnly = true)
    public IntegrationConnectionTestResultDto testConnection() {
        try {
            List<ExternalProjectDto> projects = integrationClient.projects(requireConnection(true));
            return new IntegrationConnectionTestResultDto(true, "连接成功，读取到 " + projects.size() + " 个项目");
        } catch (RuntimeException ex) {
            return new IntegrationConnectionTestResultDto(false, ex.getMessage());
        }
    }

    @Transactional
    public List<ExternalProjectDto> projects() {
        return integrationClient.projects(requireConnection(true));
    }

    @Transactional
    public List<ExternalTaskDto> tasks(String projectId, String status, String priority, String assigneeId, String page, String limit, String sortBy, String sortOrder) {
        List<ExternalTaskDto> tasks = integrationClient.tasks(requireConnection(true), projectId, status, priority, assigneeId, page, limit, sortBy, sortOrder);
        ResourceTypeEntity type = ensureTaskType();
        ResourceWorkEntity work = ensureProjectWork(type, projectId, projectId, null);
        tasks.forEach(task -> upsertTaskItem(type, work, task));
        return tasks;
    }

    @Transactional
    public ExternalTaskDto createTask(String projectId, CreateExternalTaskRequest request) {
        ExternalTaskDto task = integrationClient.createTask(requireConnection(true), projectId, request);
        persistTask(task);
        return task;
    }

    @Transactional
    public ExternalTaskDto updateTask(String taskId, UpdateExternalTaskRequest request) {
        ExternalTaskDto task = integrationClient.updateTask(requireConnection(true), taskId, request);
        persistTask(task);
        return task;
    }

    @Transactional
    public ExternalTaskDto moveTask(String taskId, MoveExternalTaskRequest request) {
        ExternalTaskDto task = integrationClient.moveTask(requireConnection(true), taskId, request);
        persistTask(task);
        return task;
    }

    @Transactional(readOnly = true)
    public List<TaskRelationDto> listRelations(String taskId) {
        return relationRepository.findByProviderAndExternalIdOrderByUpdatedAtDescCreatedAtDesc(PROVIDER, normalizeRequired(taskId, "task id required"))
            .stream()
            .map(this::toRelationDto)
            .toList();
    }

    @Transactional
    public TaskRelationDto createRelation(String taskId, CreateTaskRelationRequest request) {
        String externalId = normalizeRequired(taskId, "task id required");
        String pageId = normalizeRequired(request.pageId(), "pageId required");
        String blockId = blankToDefault(request.blockId(), "page");
        String relationType = blankToDefault(request.relationType(), "related");
        ResourceItemEntity item = ensureTaskItemByExternalId(externalId);

        ExternalResourceRelationEntity entity = relationRepository
            .findByProviderAndExternalIdAndPageIdAndBlockIdAndRelationType(PROVIDER, externalId, pageId, blockId, relationType)
            .orElseGet(ExternalResourceRelationEntity::new);

        if (entity.getId() == null) entity.setId("err-" + compactUuid());
        entity.setProvider(PROVIDER);
        entity.setExternalId(externalId);
        entity.setResourceItemId(item.getId());
        entity.setPageId(pageId);
        entity.setBlockId(blockId);
        entity.setRelationType(relationType);
        return toRelationDto(relationRepository.save(entity));
    }

    @Transactional
    public void deleteRelation(String relationId) {
        relationRepository.deleteById(normalizeRequired(relationId, "relation id required"));
    }

    private void persistTask(ExternalTaskDto task) {
        ResourceTypeEntity type = ensureTaskType();
        ResourceWorkEntity work = ensureProjectWork(type, task.projectId(), task.projectId(), null);
        upsertTaskItem(type, work, task);
    }

    private ResourceTypeEntity ensureTaskType() {
        return typeRepository.findByCode(TASK_TYPE_CODE).orElseGet(() -> {
            ResourceTypeEntity type = new ResourceTypeEntity();
            type.setId("rt-" + compactUuid());
            type.setCode(TASK_TYPE_CODE);
            type.setName("Kaneo Task");
            type.setIcon("kanban");
            type.setDescription("Tasks mirrored from Kaneo project management");
            type.setIdentityFieldKey("externalTaskId");
            type.setIdentityFieldLabel("Kaneo Task ID");
            return typeRepository.save(type);
        });
    }

    private ResourceWorkEntity ensureProjectWork(ResourceTypeEntity type, String projectId, String title, String description) {
        String workTitle = "Kaneo Project " + normalizeRequired(title, "project id required");
        return workRepository.findByTypeIdAndTitle(type.getId(), workTitle).orElseGet(() -> {
            ResourceWorkEntity work = new ResourceWorkEntity();
            work.setId("rw-" + compactUuid());
            work.setTypeId(type.getId());
            work.setTitle(workTitle);
            work.setSubtitle(projectId);
            work.setDescription(description);
            return workRepository.save(work);
        });
    }

    private ResourceItemEntity upsertTaskItem(ResourceTypeEntity type, ResourceWorkEntity work, ExternalTaskDto task) {
        String externalId = normalizeRequired(task.externalId(), "task external id required");
        ResourceItemEntity item = itemRepository.findByTypeIdAndIdentityValue(type.getId(), externalId).orElseGet(() -> {
            ResourceItemEntity created = new ResourceItemEntity();
            created.setId("ri-" + compactUuid());
            return created;
        });
        item.setTypeId(type.getId());
        item.setWorkId(work.getId());
        item.setTitle(trimToMax(blankToDefault(task.title(), externalId), 255));
        item.setIdentityValue(externalId);
        item.setSourceUrl(trimToMax(task.sourceUrl(), 1024));
        item.setEdition(trimToMax(task.status(), 128));
        item.setNote(trimToMax(task.description(), 1024));
        return itemRepository.save(item);
    }

    private ResourceItemEntity ensureTaskItemByExternalId(String externalId) {
        ResourceTypeEntity type = ensureTaskType();
        return itemRepository.findByTypeIdAndIdentityValue(type.getId(), externalId)
            .orElseThrow(() -> new BusinessException(40001, "task resource item not found; fetch task list first"));
    }

    private TaskRelationDto toRelationDto(ExternalResourceRelationEntity entity) {
        return new TaskRelationDto(
            entity.getId(),
            entity.getProvider(),
            entity.getExternalId(),
            entity.getResourceItemId(),
            entity.getPageId(),
            entity.getBlockId(),
            entity.getRelationType()
        );
    }

    private KaneoConnectionContext requireConnection(boolean requireEnabled) {
        IntegrationConnectionEntity entity = defaultConnectionEntity().orElse(null);
        if (entity == null || entity.getApiKey() == null || entity.getApiKey().isBlank()) {
            if (requireEnabled) throw new BusinessException(40000, "Kaneo connection is not configured");
            return new KaneoConnectionContext("", "", "", "");
        }
        if (requireEnabled && !entity.isEnabled()) {
            throw new BusinessException(40000, "Kaneo connection is disabled");
        }
        if (requireEnabled && (entity.getWorkspaceId() == null || entity.getWorkspaceId().isBlank())) {
            throw new BusinessException(40000, "Kaneo workspaceId is required");
        }
        return new KaneoConnectionContext(
            entity.getBaseUrl(),
            entity.getApiKey(),
            entity.getWorkspaceId(),
            defaultIfBlank(entity.getAdapterProfileJson(), DefaultAdapterProfiles.kaneo())
        );
    }

    private IntegrationConnectionDto toConnectionDto(IntegrationConnectionEntity entity) {
        return new IntegrationConnectionDto(
            entity.getId(),
            entity.getProvider(),
            entity.getBaseUrl(),
            entity.getWorkspaceId(),
            defaultIfBlank(entity.getAdapterProfileJson(), DefaultAdapterProfiles.kaneo()),
            entity.getApiKey() != null && !entity.getApiKey().isBlank(),
            entity.isEnabled()
        );
    }

    private IntegrationConnectionDto emptyConnectionDto() {
        return new IntegrationConnectionDto(null, PROVIDER, "", "", DefaultAdapterProfiles.kaneo(), false, false);
    }

    private java.util.Optional<IntegrationConnectionEntity> defaultConnectionEntity() {
        return connectionRepository.findFirstByProviderAndEnabledTrueOrderByUpdatedAtDescCreatedAtDesc(PROVIDER)
            .or(() -> connectionRepository.findFirstByProviderOrderByUpdatedAtDescCreatedAtDesc(PROVIDER))
            .or(() -> connectionRepository.findById(PROVIDER));
    }

    private void applyConnectionRequest(IntegrationConnectionEntity entity, UpdateIntegrationConnectionRequest request) {
        entity.setProvider(PROVIDER);
        entity.setBaseUrl(normalizeRequired(request.baseUrl(), "baseUrl required"));
        if (request.apiKey() != null && !request.apiKey().isBlank()) {
            entity.setApiKey(request.apiKey().trim());
        }
        entity.setWorkspaceId(blankToNull(request.workspaceId()));
        entity.setAdapterProfileJson(defaultIfBlank(request.adapterProfileJson(), DefaultAdapterProfiles.kaneo()));
        entity.setEnabled(request.enabled() == null || request.enabled());
    }

    private String normalizeRequired(String value, String message) {
        if (value == null || value.isBlank()) throw new BusinessException(40000, message);
        return value.trim();
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String trimToMax(String value, int max) {
        if (value == null || value.isBlank()) return null;
        String trimmed = value.trim();
        return trimmed.length() > max ? trimmed.substring(0, max) : trimmed;
    }

    private String compactUuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
