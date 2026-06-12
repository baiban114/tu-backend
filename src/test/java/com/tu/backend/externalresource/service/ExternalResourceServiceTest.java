package com.tu.backend.externalresource.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tu.backend.common.BusinessException;
import com.tu.backend.contenttree.entity.ContentTreeNodeEntity;
import com.tu.backend.contenttree.entity.ScopeType;
import com.tu.backend.contenttree.service.ContentTreeNodeService;
import com.tu.backend.externalresource.dto.CreateResourceChapterRequest;
import com.tu.backend.externalresource.dto.CreateResourceItemRequest;
import com.tu.backend.externalresource.dto.CreateResourceExcerptRequest;
import com.tu.backend.externalresource.dto.UpdateResourceExcerptRequest;
import com.tu.backend.externalresource.entity.ResourceExcerptEntity;
import com.tu.backend.externalresource.entity.ResourceItemEntity;
import com.tu.backend.externalresource.entity.ResourceTypeEntity;
import com.tu.backend.externalresource.entity.ResourceWorkEntity;
import com.tu.backend.externalresource.repository.ResourceExcerptRepository;
import com.tu.backend.externalresource.repository.ResourceItemRelationRepository;
import com.tu.backend.externalresource.repository.ResourceItemRepository;
import com.tu.backend.externalresource.repository.ResourceTypeRepository;
import com.tu.backend.externalresource.repository.ResourceWorkRepository;
import com.tu.backend.externalresource.service.UrlClusterMatcherService;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ExternalResourceServiceTest {

    @Test
    void createsResourceItemWithoutWorkOrIdentityValue() {
        TestContext context = new TestContext();
        ResourceTypeEntity type = type("rt-book", "book", "图书");
        when(context.typeRepository.findById("rt-book")).thenReturn(Optional.of(type));
        when(context.itemRepository.save(any(ResourceItemEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var dto = context.service.createItem(new CreateResourceItemRequest(
            "rt-book",
            "",
            "无归类图书",
            "",
            null,
            null,
            null,
            null,
            null,
            null
        ));

        assertThat(dto.workId()).isNull();
        assertThat(dto.workTitle()).isEmpty();
        assertThat(dto.identityValue()).isNull();
        assertThat(dto.title()).isEqualTo("无归类图书");
        verify(context.workRepository, never()).findById(any());
    }

    @Test
    void createsBookExcerptWithNextSortOrder() {
        TestContext context = new TestContext();
        context.stubBookItem();
        ResourceExcerptEntity existing = excerpt("re-existing", "ri-book", "已有节选", 3);
        when(context.excerptRepository.findByResourceItemId("ri-book")).thenReturn(List.of(existing));
        when(context.excerptRepository.save(any(ResourceExcerptEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var dto = context.service.createExcerpt("ri-book", new CreateResourceExcerptRequest(
            "  新节选  ",
            null,
            " 第 2 章 ",
            "  节选正文  ",
            "  备注  ",
            null
        ));

        assertThat(dto.id()).startsWith("re-");
        assertThat(dto.resourceItemId()).isEqualTo("ri-book");
        assertThat(dto.resourceItemTitle()).isEqualTo("示例图书");
        assertThat(dto.title()).isEqualTo("新节选");
        assertThat(dto.locator()).isEqualTo("第 2 章");
        assertThat(dto.excerptText()).isEqualTo("节选正文");
        assertThat(dto.note()).isEqualTo("备注");
        assertThat(dto.sortOrder()).isEqualTo(4);
    }

    @Test
    void createsExcerptWithoutBodyText() {
        TestContext context = new TestContext();
        context.stubBookItem();
        when(context.excerptRepository.findByResourceItemId("ri-book")).thenReturn(List.of());
        when(context.excerptRepository.save(any(ResourceExcerptEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var dto = context.service.createExcerpt("ri-book", new CreateResourceExcerptRequest(
            "仅标题节选",
            null,
            "p. 1",
            "   ",
            null,
            0
        ));

        assertThat(dto.title()).isEqualTo("仅标题节选");
        assertThat(dto.excerptText()).isNull();
    }

    @Test
    void updatesBookExcerptAndClampsSortOrder() {
        TestContext context = new TestContext();
        context.stubBookItem();
        ResourceExcerptEntity entity = excerpt("re-1", "ri-book", "旧节选", 2);
        when(context.excerptRepository.findById("re-1")).thenReturn(Optional.of(entity));
        when(context.excerptRepository.save(any(ResourceExcerptEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var dto = context.service.updateExcerpt("re-1", new UpdateResourceExcerptRequest(
            "更新节选",
            null,
            "",
            "更新正文",
            "",
            -10
        ));

        assertThat(dto.title()).isEqualTo("更新节选");
        assertThat(dto.locator()).isNull();
        assertThat(dto.excerptText()).isEqualTo("更新正文");
        assertThat(dto.note()).isNull();
        assertThat(dto.sortOrder()).isEqualTo(0);
    }

    @Test
    void rejectsExcerptsForNonBookResources() {
        TestContext context = new TestContext();
        ResourceTypeEntity articleType = type("rt-article", "article", "文章");
        ResourceItemEntity articleItem = item("ri-article", "rt-article", "rw-article", "文章资源");
        when(context.typeRepository.findById("rt-article")).thenReturn(Optional.of(articleType));
        when(context.itemRepository.findById("ri-article")).thenReturn(Optional.of(articleItem));

        assertThatThrownBy(() -> context.service.createExcerpt("ri-article", new CreateResourceExcerptRequest(
            "不允许",
            null,
            null,
            "正文",
            null,
            0
        )))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("resource excerpts are only supported for book or web-link resources");
        verify(context.excerptRepository, never()).save(any());
    }

    @Test
    void createsExcerptsForWebLinkResources() {
        TestContext context = new TestContext();
        ResourceTypeEntity linkType = type("rt-link", "web-link", "网络链接");
        ResourceWorkEntity linkWork = work("rw-link", "rt-link", "示例站点");
        ResourceItemEntity linkItem = item("ri-link", "rt-link", "rw-link", "示例链接");
        when(context.typeRepository.findById("rt-link")).thenReturn(Optional.of(linkType));
        when(context.workRepository.findById("rw-link")).thenReturn(Optional.of(linkWork));
        when(context.itemRepository.findById("ri-link")).thenReturn(Optional.of(linkItem));
        when(context.excerptRepository.findByResourceItemId("ri-link")).thenReturn(List.of());
        when(context.excerptRepository.save(any(ResourceExcerptEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var dto = context.service.createExcerpt("ri-link", new CreateResourceExcerptRequest(
            "页面要点",
            null,
            "#intro",
            "这是从网页摘录的要点。",
            null,
            0
        ));

        assertThat(dto.title()).isEqualTo("页面要点");
        assertThat(dto.locator()).isEqualTo("#intro");
        verify(context.excerptRepository).save(any(ResourceExcerptEntity.class));
    }

    @Test
    void removingResourceItemDeletesItsExcerptsRegardlessOfReferences() {
        TestContext context = new TestContext();
        ResourceItemEntity item = item("ri-book", "rt-book", "rw-book", "示例图书");
        when(context.itemRepository.findById("ri-book")).thenReturn(Optional.of(item));

        context.service.removeItem("ri-book");

        verify(context.excerptRepository).deleteByResourceItemId("ri-book");
        verify(context.contentTreeNodeService).deleteResourceScope("ri-book");
        verify(context.itemRepository).delete(item);
    }

    @Test
    void createsNestedChaptersForBookItem() {
        TestContext context = new TestContext();
        context.stubBookItem();
        when(context.contentTreeNodeService.listResourceItemNodes("ri-book"))
            .thenReturn(List.of());
        when(context.contentTreeNodeService.saveResourceNode(any(ContentTreeNodeEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(context.contentTreeNodeService.findResourceNode("rc-parent"))
            .thenReturn(chapter("rc-parent", "ri-book", null, "第一卷", 0));

        var parent = context.service.createChapter("ri-book", new CreateResourceChapterRequest(
            null,
            "第一卷",
            "p.1–p.100",
            null,
            0
        ));
        var child = context.service.createChapter("ri-book", new CreateResourceChapterRequest(
            "rc-parent",
            "第一章",
            null,
            null,
            0
        ));

        assertThat(parent.id()).startsWith("rc-");
        assertThat(parent.parentId()).isNull();
        assertThat(child.parentId()).isEqualTo("rc-parent");
    }

    @Test
    void createsExcerptWithChapterId() {
        TestContext context = new TestContext();
        context.stubBookItem();
        ContentTreeNodeEntity chapter = chapter("rc-1", "ri-book", null, "第一章", 0);
        when(context.contentTreeNodeService.findResourceNode("rc-1")).thenReturn(chapter);
        when(context.excerptRepository.findByResourceItemId("ri-book")).thenReturn(List.of());
        when(context.excerptRepository.save(any(ResourceExcerptEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(context.contentTreeNodeService.listResourceItemNodes("ri-book"))
            .thenReturn(List.of(chapter));

        var dto = context.service.createExcerpt("ri-book", new CreateResourceExcerptRequest(
            "节选",
            "rc-1",
            "p.12",
            "正文",
            null,
            0
        ));

        assertThat(dto.chapterId()).isEqualTo("rc-1");
        assertThat(dto.chapterTitle()).isEqualTo("第一章");
    }

    @Test
    void rejectsExcerptChapterFromDifferentItem() {
        TestContext context = new TestContext();
        context.stubBookItem();
        ContentTreeNodeEntity otherChapter = chapter("rc-other", "ri-other", null, "其他章节", 0);
        when(context.contentTreeNodeService.findResourceNode("rc-other")).thenReturn(otherChapter);

        assertThatThrownBy(() -> context.service.createExcerpt("ri-book", new CreateResourceExcerptRequest(
            "节选",
            "rc-other",
            null,
            "正文",
            null,
            0
        )))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("resource chapter does not belong to resource item");
    }

    @Test
    void deletingChapterUnbindsExcerptsAndRemovesDescendants() {
        TestContext context = new TestContext();
        context.stubBookItem();
        ContentTreeNodeEntity parent = chapter("rc-parent", "ri-book", null, "第一卷", 0);
        ContentTreeNodeEntity child = chapter("rc-child", "ri-book", "rc-parent", "第一章", 1);
        ResourceExcerptEntity excerpt = excerpt("re-1", "ri-book", "节选", 0);
        excerpt.setChapterId("rc-child");
        when(context.contentTreeNodeService.findResourceNode("rc-parent")).thenReturn(parent);
        when(context.contentTreeNodeService.listResourceItemNodes("ri-book"))
            .thenReturn(List.of(parent, child));
        when(context.excerptRepository.findByResourceItemId("ri-book")).thenReturn(List.of(excerpt));
        when(context.excerptRepository.save(any(ResourceExcerptEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        context.service.deleteChapter("rc-parent");

        assertThat(excerpt.getChapterId()).isNull();
        verify(context.contentTreeNodeService).deleteResourceNodes(Set.of("rc-parent", "rc-child"));
    }

    private static ContentTreeNodeEntity chapter(
        String id,
        String itemId,
        String parentId,
        String title,
        int sortOrder
    ) {
        ContentTreeNodeEntity entity = new ContentTreeNodeEntity();
        entity.setId(id);
        entity.setScopeType(ScopeType.RESOURCE_ITEM);
        entity.setScopeId(itemId);
        entity.setParentId(parentId);
        entity.setTitle(title);
        entity.setSortOrder(sortOrder);
        return entity;
    }

    private static ResourceTypeEntity type(String id, String code, String name) {
        ResourceTypeEntity entity = new ResourceTypeEntity();
        entity.setId(id);
        entity.setCode(code);
        entity.setName(name);
        entity.setIdentityFieldKey("isbn");
        entity.setIdentityFieldLabel("ISBN");
        return entity;
    }

    private static ResourceWorkEntity work(String id, String typeId, String title) {
        ResourceWorkEntity entity = new ResourceWorkEntity();
        entity.setId(id);
        entity.setTypeId(typeId);
        entity.setTitle(title);
        return entity;
    }

    private static ResourceItemEntity item(String id, String typeId, String workId, String title) {
        ResourceItemEntity entity = new ResourceItemEntity();
        entity.setId(id);
        entity.setTypeId(typeId);
        entity.setWorkId(workId);
        entity.setTitle(title);
        entity.setIdentityValue("978-7-0000-0000-1");
        return entity;
    }

    private static ResourceExcerptEntity excerpt(String id, String itemId, String title, int sortOrder) {
        ResourceExcerptEntity entity = new ResourceExcerptEntity();
        entity.setId(id);
        entity.setResourceItemId(itemId);
        entity.setTitle(title);
        entity.setLocator("第 1 章");
        entity.setExcerptText("正文");
        entity.setSortOrder(sortOrder);
        return entity;
    }

    private static final class TestContext {
        final ResourceTypeRepository typeRepository = mock(ResourceTypeRepository.class);
        final ResourceWorkRepository workRepository = mock(ResourceWorkRepository.class);
        final ResourceItemRepository itemRepository = mock(ResourceItemRepository.class);
        final ResourceExcerptRepository excerptRepository = mock(ResourceExcerptRepository.class);
        final ContentTreeNodeService contentTreeNodeService = mock(ContentTreeNodeService.class);
        final ResourceItemRelationRepository itemRelationRepository = mock(ResourceItemRelationRepository.class);
        final UrlClusterMatcherService clusterMatcherService = mock(UrlClusterMatcherService.class);
        final ExternalResourceService service = new ExternalResourceService(
            typeRepository,
            workRepository,
            itemRepository,
            excerptRepository,
            contentTreeNodeService,
            itemRelationRepository,
            clusterMatcherService
        );

        void stubBookItem() {
            ResourceTypeEntity type = type("rt-book", "book", "图书");
            ResourceWorkEntity work = work("rw-book", "rt-book", "示例之书");
            ResourceItemEntity item = item("ri-book", "rt-book", "rw-book", "示例图书");
            when(typeRepository.findById("rt-book")).thenReturn(Optional.of(type));
            when(workRepository.findById("rw-book")).thenReturn(Optional.of(work));
            when(itemRepository.findById("ri-book")).thenReturn(Optional.of(item));
        }
    }
}
