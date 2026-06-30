package com.tu.backend.contenttree;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OutlineExtractorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void extractPageOutlineWithPdfExcerptOnlyNeverUsesNullTitle() throws Exception {
        String blocksJson = """
            [
              {
                "id": "page-content",
                "type": "richtext",
                "document": {
                  "type": "doc",
                  "content": [
                    {
                      "type": "pdfExcerptBlock",
                      "attrs": {
                        "blockId": "pdf-1",
                        "fileId": "f1",
                        "fileName": "notes.pdf",
                        "startPage": 2,
                        "endPage": 2
                      }
                    }
                  ]
                },
                "content": "",
                "metadata": { "schemaVersion": 2 }
              }
            ]
            """;
        ArrayNode blocks = (ArrayNode) objectMapper.readTree(blocksJson);
        var nodes = OutlineExtractor.extractPageOutline("page-1", blocks);
        assertThat(nodes).isNotEmpty();
        assertThat(nodes.get(0).title()).isEqualTo("notes.pdf · 第2页");
        assertThat(nodes.get(0).title()).isNotBlank();
    }
}
