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

    @Test
    void extractEmbedOutlinesFromPdfExcerptBlock() throws Exception {
        String json = """
            {
              "type": "doc",
              "content": [
                {
                  "type": "pdfExcerptBlock",
                  "attrs": {
                    "blockId": "pdf-1",
                    "fileId": "f1",
                    "fileName": "book.pdf",
                    "startPage": 3,
                    "endPage": 7
                  }
                }
              ]
            }
            """;
        var document = objectMapper.readTree(json);
        var embeds = TiptapDocumentWalker.extractEmbedOutlines(document, "page-content");
        assertThat(embeds).hasSize(1);
        assertThat(embeds.get(0).title()).isEqualTo("book.pdf · 第3–7页");
        assertThat(embeds.get(0).blockId()).isEqualTo("pdf-1");
        assertThat(TiptapDocumentWalker.extractPlainText(document)).contains("book.pdf");
    }

    @Test
    void pdfExcerptBlockLabelSupportsFullMode() throws Exception {
        var attrs = objectMapper.readTree("""
            {
              "fileName": "book.pdf",
              "viewMode": "full",
              "startPage": 1,
              "endPage": 120
            }
            """);
        assertThat(TiptapDocumentWalker.pdfExcerptBlockLabel(attrs)).isEqualTo("book.pdf · 全文");
    }
}
