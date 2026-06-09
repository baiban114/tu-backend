package com.tu.backend.page.service;

import com.tu.backend.annotation.service.OrphanedAnnotationService;
import com.tu.backend.common.BusinessException;
import com.tu.backend.content.service.PageContentService;
import com.tu.backend.knowledge.repository.KnowledgeBaseRepository;
import com.tu.backend.page.dto.CreatePageRequest;
import com.tu.backend.page.dto.PageItemDto;
import com.tu.backend.page.dto.UpdatePageRequest;
import com.tu.backend.page.entity.PageEntity;
import com.tu.backend.page.repository.PageRepository;
import com.tu.backend.index.PageIndexCoordinator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class PageService {

    private static final Logger log = LoggerFactory.getLogger(PageService.class);

    private final PageRepository pageRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final PageContentService pageContentService;
    private final PageIndexCoordinator pageIndexCoordinator;
    private final OrphanedAnnotationService orphanedAnnotationService;

    public PageService(
        PageRepository pageRepository,
        KnowledgeBaseRepository knowledgeBaseRepository,
        PageContentService pageContentService,
        PageIndexCoordinator pageIndexCoordinator,
        OrphanedAnnotationService orphanedAnnotationService
    ) {
        this.pageRepository = pageRepository;
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.pageContentService = pageContentService;
        this.pageIndexCoordinator = pageIndexCoordinator;
        this.orphanedAnnotationService = orphanedAnnotationService;
    }

    @Transactional(readOnly = true)
    public List<PageItemDto> getTree(String kbId) {
        ensureKnowledgeBaseExists(kbId);
        List<PageEntity> pages = pageRepository.findByKbIdOrderBySortOrderAscCreatedAtAsc(kbId);
        return buildTree(pages);
    }

    @Transactional
    public PageItemDto create(CreatePageRequest request) {
        ensureKnowledgeBaseExists(request.kbId());
        String parentId = normalizeParentId(request.parentId());
        validateParent(request.kbId(), parentId);

        PageEntity entity = new PageEntity();
        entity.setId("p-" + UUID.randomUUID().toString().replace("-", ""));
        entity.setKbId(request.kbId());
        entity.setParentId(parentId);
        entity.setTitle(normalizeTitle(request.title()));
        entity.setPageType(normalizePageType(request.pageType()));
        entity.setSortOrder(nextOrder(request.kbId(), parentId));

        return toDto(pageRepository.save(entity));
    }

    @Transactional
    public PageItemDto update(String id, UpdatePageRequest request) {
        PageEntity entity = pageRepository.findById(id)
            .orElseThrow(() -> new BusinessException(40001, "page not found"));

        boolean changed = false;

        boolean titleChanged = false;

        if (request.isTitlePresent()) {
            entity.setTitle(normalizeTitle(request.getTitle()));
            changed = true;
            titleChanged = true;
        }

        if (request.isParentIdPresent() || request.isOrderPresent()) {
            String sourceParentId = entity.getParentId();
            String targetParentId = request.isParentIdPresent() ? normalizeParentId(request.getParentId()) : entity.getParentId();
            validateParent(entity.getKbId(), targetParentId);
            validateNotMoveToSelfOrDescendant(entity, targetParentId);
            entity.setParentId(targetParentId);
            entity.setSortOrder(reorderAndResolveSortOrder(entity, sourceParentId, targetParentId, request.getOrder()));
            changed = true;
        }

        if (!changed) {
            throw new BusinessException(40000, "no fields to update");
        }

        PageItemDto saved = toDto(pageRepository.save(entity));
        if (titleChanged) {
            pageIndexCoordinator.onPageContentChanged(id);
        }
        return saved;
    }

    @Transactional
    public void delete(String id) {
        PageEntity root = pageRepository.findById(id)
            .orElseThrow(() -> new BusinessException(40001, "page not found"));

        List<PageEntity> allPages = pageRepository.findByKbIdOrderBySortOrderAscCreatedAtAsc(root.getKbId());
        Map<String, List<PageEntity>> childrenMap = new HashMap<>();
        for (PageEntity page : allPages) {
            if (page.getParentId() == null) {
                continue;
            }
            childrenMap.computeIfAbsent(page.getParentId(), key -> new ArrayList<>()).add(page);
        }

        ArrayDeque<String> stack = new ArrayDeque<>();
        List<String> idsToDelete = new ArrayList<>();
        stack.push(root.getId());
        while (!stack.isEmpty()) {
            String currentId = stack.pop();
            idsToDelete.add(currentId);
            for (PageEntity child : childrenMap.getOrDefault(currentId, List.of())) {
                stack.push(child.getId());
            }
        }

        int orphaned = orphanedAnnotationService.orphanAnnotationsForPages(idsToDelete);
        if (orphaned > 0) {
            log.info("Orphaned {} annotations before deleting {} pages", orphaned, idsToDelete.size());
        }
        pageContentService.deleteByPageIds(idsToDelete);
        pageRepository.deleteAllById(idsToDelete);
        pageIndexCoordinator.deletePages(root.getKbId(), idsToDelete);
    }

    public void deleteByKnowledgeBaseId(String kbId) {
        List<String> pageIds = pageRepository.findByKbIdOrderBySortOrderAscCreatedAtAsc(kbId)
            .stream()
            .map(PageEntity::getId)
            .toList();
        int orphaned = orphanedAnnotationService.orphanAnnotationsForPages(pageIds);
        if (orphaned > 0) {
            log.info("Orphaned {} annotations before deleting KB {} ({} pages)", orphaned, kbId, pageIds.size());
        }
        pageContentService.deleteByPageIds(pageIds);
        pageRepository.deleteByKbId(kbId);
        pageIndexCoordinator.deleteKnowledgeBase(kbId);
    }

    private void ensureKnowledgeBaseExists(String kbId) {
        if (!knowledgeBaseRepository.existsById(kbId)) {
            throw new BusinessException(40001, "knowledge base not found");
        }
    }

    private void validateParent(String kbId, String parentId) {
        if (parentId == null) {
            return;
        }
        PageEntity parent = pageRepository.findById(parentId)
            .orElseThrow(() -> new BusinessException(40001, "parent page not found"));
        if (!Objects.equals(parent.getKbId(), kbId)) {
            throw new BusinessException(40009, "parent page does not belong to current knowledge base");
        }
    }

    private void validateNotMoveToSelfOrDescendant(PageEntity entity, String targetParentId) {
        if (targetParentId == null) {
            return;
        }
        if (entity.getId().equals(targetParentId)) {
            throw new BusinessException(40009, "page cannot be moved under itself");
        }

        List<PageEntity> allPages = pageRepository.findByKbIdOrderBySortOrderAscCreatedAtAsc(entity.getKbId());
        Map<String, List<PageEntity>> childrenMap = new HashMap<>();
        for (PageEntity page : allPages) {
            if (page.getParentId() != null) {
                childrenMap.computeIfAbsent(page.getParentId(), key -> new ArrayList<>()).add(page);
            }
        }

        ArrayDeque<String> stack = new ArrayDeque<>();
        stack.push(entity.getId());
        while (!stack.isEmpty()) {
            String current = stack.pop();
            for (PageEntity child : childrenMap.getOrDefault(current, List.of())) {
                if (child.getId().equals(targetParentId)) {
                    throw new BusinessException(40009, "page cannot be moved under its descendant");
                }
                stack.push(child.getId());
            }
        }
    }

    private int nextOrder(String kbId, String parentId) {
        if (parentId == null) {
            return Math.toIntExact(pageRepository.countByKbIdAndParentIdIsNull(kbId));
        }
        return Math.toIntExact(pageRepository.countByKbIdAndParentId(kbId, parentId));
    }

    private int normalizeOrder(Integer order) {
        if (order == null || order < 0) {
            return 0;
        }
        return order;
    }

    private int reorderAndResolveSortOrder(PageEntity entity, String sourceParentId, String targetParentId, Integer requestedOrder) {
        List<PageEntity> siblings = pageRepository.findByKbIdOrderBySortOrderAscCreatedAtAsc(entity.getKbId())
            .stream()
            .filter(page -> !page.getId().equals(entity.getId()))
            .filter(page -> Objects.equals(page.getParentId(), targetParentId))
            .sorted(Comparator.comparing(PageEntity::getSortOrder).thenComparing(PageEntity::getCreatedAt))
            .toList();

        List<PageEntity> reordered = new ArrayList<>(siblings);
        int targetIndex = Math.min(normalizeOrder(requestedOrder), reordered.size());
        reordered.add(targetIndex, entity);

        for (int i = 0; i < reordered.size(); i++) {
            PageEntity sibling = reordered.get(i);
            sibling.setSortOrder(i);
            if (!sibling.getId().equals(entity.getId())) {
                pageRepository.save(sibling);
            }
        }

        if (!Objects.equals(sourceParentId, targetParentId)) {
            normalizeSiblingOrders(entity.getKbId(), sourceParentId);
        }

        return targetIndex;
    }

    private void normalizeSiblingOrders(String kbId, String parentId) {
        List<PageEntity> siblings = pageRepository.findByKbIdOrderBySortOrderAscCreatedAtAsc(kbId)
            .stream()
            .filter(page -> Objects.equals(page.getParentId(), parentId))
            .sorted(Comparator.comparing(PageEntity::getSortOrder).thenComparing(PageEntity::getCreatedAt))
            .toList();

        for (int i = 0; i < siblings.size(); i++) {
            PageEntity sibling = siblings.get(i);
            if (!Objects.equals(sibling.getSortOrder(), i)) {
                sibling.setSortOrder(i);
                pageRepository.save(sibling);
            }
        }
    }

    private String normalizeTitle(String title) {
        if (title == null || title.isBlank()) {
            return "新页面";
        }
        return title.trim();
    }

    private String normalizePageType(String pageType) {
        if (pageType == null || pageType.isBlank()) {
            return "document";
        }
        String normalized = pageType.trim().toLowerCase();
        if ("mindmap".equals(normalized)) {
            return "mindmap";
        }
        if ("x6board".equals(normalized)) {
            return "x6board";
        }
        return "document";
    }

    private String normalizeParentId(String parentId) {
        if (parentId == null || parentId.isBlank()) {
            return null;
        }
        return parentId.trim();
    }

    private List<PageItemDto> buildTree(List<PageEntity> pages) {
        Map<String, PageItemDto> dtoMap = new HashMap<>();
        List<PageItemDto> roots = new ArrayList<>();

        for (PageEntity page : pages) {
            dtoMap.put(page.getId(), toDto(page));
        }

        for (PageEntity page : pages) {
            PageItemDto current = dtoMap.get(page.getId());
            if (page.getParentId() == null) {
                roots.add(current);
                continue;
            }

            PageItemDto parent = dtoMap.get(page.getParentId());
            if (parent != null) {
                parent.getChildren().add(current);
            } else {
                roots.add(current);
            }
        }

        sortTree(roots);
        return roots;
    }

    private void sortTree(List<PageItemDto> nodes) {
        nodes.sort(Comparator.comparing(PageItemDto::getOrder).thenComparing(PageItemDto::getId));
        for (PageItemDto node : nodes) {
            sortTree(node.getChildren());
        }
    }

    private PageItemDto toDto(PageEntity entity) {
        PageItemDto dto = new PageItemDto();
        dto.setId(entity.getId());
        dto.setKbId(entity.getKbId());
        dto.setParentId(entity.getParentId());
        dto.setTitle(entity.getTitle());
        dto.setOrder(entity.getSortOrder());
        dto.setPageType(entity.getPageType() != null ? entity.getPageType() : "document");
        return dto;
    }
}
