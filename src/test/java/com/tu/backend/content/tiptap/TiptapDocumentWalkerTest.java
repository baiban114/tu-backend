package com.tu.backend.content.tiptap;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TiptapDocumentWalkerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void extractPlainTextFromDocument() throws Exception {
        String json = """
            {
              "type": "doc",
              "content": [
                {
                  "type": "heading",
                  "attrs": { "level": 2, "blockId": "h1" },
                  "content": [{ "type": "text", "text": "Hello" }]
                },
                {
                  "type": "paragraph",
                  "content": [{ "type": "text", "text": "Body" }]
                }
              ]
            }
            """;
        var document = objectMapper.readTree(json);
        String text = TiptapDocumentWalker.extractPlainText(document);
        assertThat(text).contains("Hello");
        assertThat(text).contains("Body");
    }

    @Test
    void extractHeadingsFromDocument() throws Exception {
        String json = """
            {
              "type": "doc",
              "content": [
                {
                  "type": "heading",
                  "attrs": { "level": 1, "blockId": "h1" },
                  "content": [{ "type": "text", "text": "Top" }]
                }
              ]
            }
            """;
        var document = objectMapper.readTree(json);
        var headings = TiptapDocumentWalker.extractHeadings(document, "page-content");
        assertThat(headings).hasSize(1);
        assertThat(headings.get(0).text()).isEqualTo("Top");
        assertThat(headings.get(0).level()).isEqualTo(1);
        assertThat(headings.get(0).blockId()).isEqualTo("h1");
    }
}
