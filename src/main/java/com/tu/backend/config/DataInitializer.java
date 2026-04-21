package com.tu.backend.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tu.backend.content.entity.PageContentEntity;
import com.tu.backend.content.repository.PageContentRepository;
import com.tu.backend.knowledge.entity.KnowledgeBaseEntity;
import com.tu.backend.knowledge.repository.KnowledgeBaseRepository;
import com.tu.backend.page.entity.PageEntity;
import com.tu.backend.page.repository.PageRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seedDemoData(
        KnowledgeBaseRepository knowledgeBaseRepository,
        PageRepository pageRepository,
        PageContentRepository pageContentRepository,
        ObjectMapper objectMapper
    ) {
        return args -> {
            if (knowledgeBaseRepository.count() > 0) {
                return;
            }

            KnowledgeBaseEntity kb1 = createKb("kb-demo-1", "个人笔记", "📘", "日常学习与思考记录");
            KnowledgeBaseEntity kb2 = createKb("kb-demo-2", "技术文档", "📗", "开发规范与技术方案");
            knowledgeBaseRepository.save(kb1);
            knowledgeBaseRepository.save(kb2);

            PageEntity page1 = createPage("p-demo-1", kb1.getId(), null, "快速入门", 0);
            PageEntity page2 = createPage("p-demo-2", kb1.getId(), null, "基础概念", 1);
            PageEntity page3 = createPage("p-demo-3", kb1.getId(), page2.getId(), "数据结构", 0);
            PageEntity page4 = createPage("p-demo-4", kb2.getId(), null, "API 文档", 0);
            pageRepository.save(page1);
            pageRepository.save(page2);
            pageRepository.save(page3);
            pageRepository.save(page4);

            pageContentRepository.save(createContent(page1.getId(), buildWelcomeBlocks(objectMapper)));
            pageContentRepository.save(createContent(page2.getId(), buildConceptBlocks(objectMapper)));
            pageContentRepository.save(createContent(page4.getId(), buildApiBlocks(objectMapper)));
        };
    }

    private KnowledgeBaseEntity createKb(String id, String name, String icon, String description) {
        KnowledgeBaseEntity entity = new KnowledgeBaseEntity();
        entity.setId(id);
        entity.setName(name);
        entity.setIcon(icon);
        entity.setDescription(description);
        return entity;
    }

    private PageEntity createPage(String id, String kbId, String parentId, String title, int sortOrder) {
        PageEntity entity = new PageEntity();
        entity.setId(id);
        entity.setKbId(kbId);
        entity.setParentId(parentId);
        entity.setTitle(title);
        entity.setSortOrder(sortOrder);
        return entity;
    }

    private PageContentEntity createContent(String pageId, String blocksJson) {
        PageContentEntity entity = new PageContentEntity();
        entity.setPageId(pageId);
        entity.setBlocksJson(blocksJson);
        return entity;
    }

    private String buildWelcomeBlocks(ObjectMapper objectMapper) throws JsonProcessingException {
        ArrayNode blocks = objectMapper.createArrayNode();

        ObjectNode richText = objectMapper.createObjectNode();
        richText.put("id", "b-demo-1");
        richText.put("type", "richtext");
        richText.put("content", "# 快速入门\n\n欢迎使用 tu，当前前后端已经连通。");
        blocks.add(richText);

        ObjectNode x6 = objectMapper.createObjectNode();
        x6.put("id", "b-demo-x6-1");
        x6.put("type", "x6");
        x6.put("title", "示例画板");
        ObjectNode graphData = objectMapper.createObjectNode();
        ArrayNode nodes = objectMapper.createArrayNode();
        ArrayNode edges = objectMapper.createArrayNode();
        ObjectNode n1 = objectMapper.createObjectNode();
        n1.put("id", "demo-node-1");
        n1.put("x", 120);
        n1.put("y", 100);
        n1.put("width", 120);
        n1.put("height", 56);
        n1.put("label", "开始");
        ObjectNode n2 = objectMapper.createObjectNode();
        n2.put("id", "demo-node-2");
        n2.put("x", 340);
        n2.put("y", 100);
        n2.put("width", 140);
        n2.put("height", 56);
        n2.put("label", "已连接后端");
        ObjectNode e1 = objectMapper.createObjectNode();
        e1.put("id", "demo-edge-1");
        e1.put("source", "demo-node-1");
        e1.put("target", "demo-node-2");
        nodes.add(n1);
        nodes.add(n2);
        edges.add(e1);
        graphData.set("nodes", nodes);
        graphData.set("edges", edges);
        x6.set("graphData", graphData);
        blocks.add(x6);

        return objectMapper.writeValueAsString(blocks);
    }

    private String buildConceptBlocks(ObjectMapper objectMapper) throws JsonProcessingException {
        ArrayNode blocks = objectMapper.createArrayNode();
        ObjectNode richText = objectMapper.createObjectNode();
        richText.put("id", "b-demo-2");
        richText.put("type", "richtext");
        richText.put("content", "# 基础概念\n\n这里可以继续添加富文本、时间轴、容器和引用块。");
        blocks.add(richText);
        return objectMapper.writeValueAsString(blocks);
    }

    private String buildApiBlocks(ObjectMapper objectMapper) throws JsonProcessingException {
        ArrayNode blocks = objectMapper.createArrayNode();
        ObjectNode richText = objectMapper.createObjectNode();
        richText.put("id", "b-demo-3");
        richText.put("type", "richtext");
        richText.put("content", "# API 文档\n\n后端接口已经具备知识库、页面、内容、块引用和块同步能力。");
        blocks.add(richText);
        return objectMapper.writeValueAsString(blocks);
    }
}

