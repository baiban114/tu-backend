package com.tu.backend.search;

import static org.assertj.core.api.Assertions.assertThat;

import com.tu.backend.search.dto.SearchResponseDto;
import org.junit.jupiter.api.Test;

class SearchServiceTest {

    @Test
    void returnsEmptyWhenSearchDisabled() {
        SearchProperties properties = new SearchProperties();
        properties.setEnabled(false);

        SearchService service = new SearchService(null, properties);
        SearchResponseDto response = service.search("hello", 20);

        assertThat(response.enabled()).isFalse();
        assertThat(response.hits()).isEmpty();
        assertThat(response.message()).isEqualTo("search disabled");
    }

    @Test
    void returnsUnavailableWhenClientMissing() {
        SearchProperties properties = new SearchProperties();
        properties.setEnabled(true);

        SearchService service = new SearchService(null, properties);
        SearchResponseDto response = service.search("hello", 20);

        assertThat(response.enabled()).isTrue();
        assertThat(response.hits()).isEmpty();
        assertThat(response.message()).isEqualTo("search unavailable");
    }

    @Test
    void returnsEmptyForShortQuery() {
        SearchProperties properties = new SearchProperties();
        properties.setEnabled(true);

        SearchService service = new SearchService(null, properties);
        SearchResponseDto response = service.search("a", 20);

        assertThat(response.enabled()).isTrue();
        assertThat(response.hits()).isEmpty();
        assertThat(response.message()).isNull();
    }

    @Test
    void trimsQueryBeforeLengthCheck() {
        SearchProperties properties = new SearchProperties();
        properties.setEnabled(true);

        SearchService service = new SearchService(null, properties);
        SearchResponseDto response = service.search("  a  ", 20);

        assertThat(response.hits()).isEmpty();
        assertThat(response.message()).isNull();
    }
}
