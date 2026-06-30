package com.tu.backend.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tu.backend.common.BusinessException;
import org.junit.jupiter.api.Test;

class HttpByteRangeTest {

    @Test
    void parsesInclusiveByteRange() {
        HttpByteRange range = HttpByteRange.parse("bytes=0-1023", 5000);
        assertThat(range.start()).isEqualTo(0);
        assertThat(range.end()).isEqualTo(1023);
        assertThat(range.length()).isEqualTo(1024);
        assertThat(range.toContentRangeValue(5000)).isEqualTo("bytes 0-1023/5000");
        assertThat(range.toS3RangeValue()).isEqualTo("bytes=0-1023");
    }

    @Test
    void parsesOpenEndedRange() {
        HttpByteRange range = HttpByteRange.parse("bytes=1024-", 5000);
        assertThat(range.start()).isEqualTo(1024);
        assertThat(range.end()).isEqualTo(4999);
    }

    @Test
    void parsesSuffixRange() {
        HttpByteRange range = HttpByteRange.parse("bytes=-512", 5000);
        assertThat(range.start()).isEqualTo(4488);
        assertThat(range.end()).isEqualTo(4999);
    }

    @Test
    void rejectsInvalidRange() {
        assertThatThrownBy(() -> HttpByteRange.parse("bytes=6000-7000", 5000))
            .isInstanceOf(BusinessException.class);
    }
}
