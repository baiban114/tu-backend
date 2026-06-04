package com.tu.backend.externalresource.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ExternalUrlNormalizerTest {

    @Test
    void stripsHashForBaseUrl() {
        ExternalUrlNormalizer.ParsedExternalUrl parsed = ExternalUrlNormalizer.parse(
            "https://example.com/docs/page#section-1"
        );
        assertThat(parsed).isNotNull();
        assertThat(parsed.baseUrl()).isEqualTo("https://example.com/docs/page");
        assertThat(parsed.anchor()).isEqualTo("section-1");
        assertThat(parsed.excerptMode()).isTrue();
    }

    @Test
    void normalizesTrailingSlashOnPath() {
        ExternalUrlNormalizer.ParsedExternalUrl parsed = ExternalUrlNormalizer.parse(
            "https://example.com/books/123/"
        );
        assertThat(parsed).isNotNull();
        assertThat(parsed.baseUrl()).isEqualTo("https://example.com/books/123");
        assertThat(parsed.excerptMode()).isFalse();
    }
}
