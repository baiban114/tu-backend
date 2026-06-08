package com.tu.backend.search;

import com.tu.backend.rag.dto.RagIndexDocument;
import org.springframework.stereotype.Component;

@Component
public class SearchDocumentMapper {

    public SearchDocument from(RagIndexDocument rag, String kbName, String pageTitle) {
        String docId = rag.pageId() + ":" + rag.blockId();
        return new SearchDocument(
            docId,
            rag.kbId(),
            kbName,
            rag.pageId(),
            pageTitle,
            rag.blockId(),
            rag.blockType(),
            rag.title(),
            rag.content(),
            rag.updatedAt()
        );
    }

    public SearchDocument pageTitleOnly(String kbId, String kbName, String pageId, String pageTitle, String updatedAt) {
        return new SearchDocument(
            pageId + ":_page",
            kbId,
            kbName,
            pageId,
            pageTitle,
            "_page",
            "page",
            pageTitle,
            pageTitle,
            updatedAt
        );
    }
}
