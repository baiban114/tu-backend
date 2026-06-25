package com.tu.backend.knowledge.service;

import com.tu.backend.common.BusinessException;
import com.tu.backend.content.entity.PageContentEntity;
import com.tu.backend.content.repository.PageContentRepository;
import com.tu.backend.knowledge.dto.CreateKnowledgeBaseRequest;
import com.tu.backend.knowledge.dto.ImportRoadmapRequest;
import com.tu.backend.knowledge.dto.ImportRoadmapResponse;
import com.tu.backend.knowledge.dto.KnowledgeBaseDto;
import com.tu.backend.knowledge.dto.RoadmapNodeRequest;
import com.tu.backend.knowledge.dto.UpdateKnowledgeBaseRequest;
import com.tu.backend.knowledge.entity.KnowledgeBaseEntity;
import com.tu.backend.knowledge.repository.KnowledgeBaseRepository;
import com.tu.backend.page.dto.PageItemDto;
import com.tu.backend.page.entity.PageEntity;
import com.tu.backend.page.repository.PageRepository;
import com.tu.backend.knowledgerelation.service.KnowledgePointService;
import com.tu.backend.knowledgerelation.service.KnowledgeRelationService;
import com.tu.backend.page.service.PageService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class KnowledgeBaseService {

    private static final int MAX_ROADMAP_PAGES = 500;

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final PageService pageService;
    private final PageRepository pageRepository;
    private final PageContentRepository pageContentRepository;
    private final ObjectMapper objectMapper;
    private final KnowledgeRelationService knowledgeRelationService;
    private final KnowledgePointService knowledgePointService;

    public KnowledgeBaseService(
        KnowledgeBaseRepository knowledgeBaseRepository,
        PageService pageService,
        PageRepository pageRepository,
        PageContentRepository pageContentRepository,
        ObjectMapper objectMapper,
        KnowledgeRelationService knowledgeRelationService,
        KnowledgePointService knowledgePointService
    ) {
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.pageService = pageService;
        this.pageRepository = pageRepository;
        this.pageContentRepository = pageContentRepository;
        this.objectMapper = objectMapper;
        this.knowledgeRelationService = knowledgeRelationService;
        this.knowledgePointService = knowledgePointService;
    }

    @Transactional(readOnly = true)
    public List<KnowledgeBaseDto> list() {
        return knowledgeBaseRepository.findAll()
            .stream()
            .map(this::toDto)
            .toList();
    }

    @Transactional
    public KnowledgeBaseDto create(CreateKnowledgeBaseRequest request) {
        String normalizedName = request.name().trim();
        if (knowledgeBaseRepository.existsByName(normalizedName)) {
            throw new BusinessException(40009, "knowledge base name already exists");
        }

        KnowledgeBaseEntity entity = new KnowledgeBaseEntity();
        entity.setId("kb-" + UUID.randomUUID().toString().replace("-", ""));
        entity.setName(normalizedName);
        entity.setIcon(request.icon() == null || request.icon().isBlank() ? "📘" : request.icon().trim());
        entity.setDescription(request.description() == null || request.description().isBlank() ? null : request.description().trim());
        return toDto(knowledgeBaseRepository.save(entity));
    }

    @Transactional
    public ImportRoadmapResponse importRoadmap(ImportRoadmapRequest request) {
        List<RoadmapNodeRequest> roots = resolveRoadmapRoots(request);
        String kbName = normalizeKnowledgeBaseName(request.name(), roots);
        if (knowledgeBaseRepository.existsByName(kbName)) {
            throw new BusinessException(40009, "knowledge base name already exists");
        }

        int pageCount = countNodes(roots);
        if (pageCount == 0) {
            throw new BusinessException(40000, "roadmap contains no pages");
        }
        if (pageCount > MAX_ROADMAP_PAGES) {
            throw new BusinessException(40000, "roadmap page count exceeds " + MAX_ROADMAP_PAGES);
        }

        KnowledgeBaseEntity kb = new KnowledgeBaseEntity();
        kb.setId("kb-" + UUID.randomUUID().toString().replace("-", ""));
        kb.setName(kbName);
        kb.setIcon(request.icon() == null || request.icon().isBlank() ? "📚" : request.icon().trim());
        kb.setDescription(request.description() == null || request.description().isBlank() ? null : request.description().trim());
        KnowledgeBaseEntity savedKb = knowledgeBaseRepository.save(kb);

        for (int i = 0; i < roots.size(); i++) {
            createRoadmapPageTree(savedKb.getId(), null, roots.get(i), i);
        }

        List<PageItemDto> pages = pageService.getTree(savedKb.getId());
        return new ImportRoadmapResponse(toDto(savedKb), pages, pageCount);
    }

    @Transactional
    public KnowledgeBaseDto update(String id, UpdateKnowledgeBaseRequest request) {
        KnowledgeBaseEntity entity = knowledgeBaseRepository.findById(id)
            .orElseThrow(() -> new BusinessException(40001, "knowledge base not found"));

        String normalizedName = request.name().trim();
        if (!entity.getName().equals(normalizedName) && knowledgeBaseRepository.existsByName(normalizedName)) {
            throw new BusinessException(40009, "knowledge base name already exists");
        }

        entity.setName(normalizedName);
        return toDto(knowledgeBaseRepository.save(entity));
    }

    @Transactional
    public void delete(String id) {
        KnowledgeBaseEntity entity = knowledgeBaseRepository.findById(id)
            .orElseThrow(() -> new BusinessException(40001, "knowledge base not found"));
        pageService.deleteByKnowledgeBaseId(id);
        knowledgePointService.deleteByKbId(id);
        knowledgeRelationService.deleteByKbId(id);
        knowledgeBaseRepository.delete(entity);
    }

    private KnowledgeBaseDto toDto(KnowledgeBaseEntity entity) {
        return new KnowledgeBaseDto(
            entity.getId(),
            entity.getName(),
            entity.getIcon(),
            entity.getDescription()
        );
    }

    private List<RoadmapNodeRequest> resolveRoadmapRoots(ImportRoadmapRequest request) {
        if (request.root() != null) {
            return List.of(request.root());
        }
        if (request.pages() != null && !request.pages().isEmpty()) {
            return request.pages();
        }
        if (request.roadmap() == null) {
            throw new BusinessException(40000, "roadmap is required");
        }

        Object roadmap = request.roadmap();
        if (roadmap instanceof List<?> items) {
            return items.stream().map(this::convertRoadmapNode).toList();
        }
        return List.of(convertRoadmapNode(roadmap));
    }

    private RoadmapNodeRequest convertRoadmapNode(Object value) {
        return objectMapper.convertValue(value, RoadmapNodeRequest.class);
    }

    private String normalizeKnowledgeBaseName(String requestedName, List<RoadmapNodeRequest> roots) {
        if (requestedName != null && !requestedName.isBlank()) {
            return requestedName.trim();
        }
        if (roots.size() == 1) {
            return normalizeRoadmapTitle(roots.get(0));
        }
        return "Roadmap " + UUID.randomUUID().toString().substring(0, 8);
    }

    private int countNodes(List<RoadmapNodeRequest> nodes) {
        int total = 0;
        for (RoadmapNodeRequest node : nodes) {
            total++;
            if (node.children() != null) {
                total += countNodes(node.children());
            }
        }
        return total;
    }

    private void createRoadmapPageTree(String kbId, String parentId, RoadmapNodeRequest node, int sortOrder) {
        PageEntity page = new PageEntity();
        page.setId("p-" + UUID.randomUUID().toString().replace("-", ""));
        page.setKbId(kbId);
        page.setParentId(parentId);
        page.setTitle(normalizeRoadmapTitle(node));
        page.setSortOrder(sortOrder);
        PageEntity saved = pageRepository.save(page);

        saveInitialContent(saved, node);

        List<RoadmapNodeRequest> children = node.children() == null ? List.of() : node.children();
        for (int i = 0; i < children.size(); i++) {
            createRoadmapPageTree(kbId, saved.getId(), children.get(i), i);
        }
    }

    private void saveInitialContent(PageEntity page, RoadmapNodeRequest node) {
        String body = firstNonBlank(node.content(), node.description());
        if (body == null) {
            body = "";
        }

        Map<String, Object> block = new LinkedHashMap<>();
        block.put("id", "b-" + UUID.randomUUID().toString().replace("-", ""));
        block.put("type", "richtext");
        block.put("content", "# " + page.getTitle() + (body.isBlank() ? "" : "\n\n" + body));

        PageContentEntity content = new PageContentEntity();
        content.setPageId(page.getId());
        content.setBlocksJson(serializeBlocks(List.<Object>of(block)));
        pageContentRepository.save(content);
    }

    private String normalizeRoadmapTitle(RoadmapNodeRequest node) {
        String title = firstNonBlank(node.title(), node.name());
        if (title == null) {
            throw new BusinessException(40000, "roadmap node title is required");
        }
        return title.length() > 128 ? title.substring(0, 128) : title;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private String serializeBlocks(List<Object> blocks) {
        try {
            return objectMapper.writeValueAsString(blocks);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(40000, "failed to build page content");
        }
    }
}
