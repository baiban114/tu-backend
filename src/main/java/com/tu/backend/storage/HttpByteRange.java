package com.tu.backend.storage;

import com.tu.backend.common.BusinessException;

public record HttpByteRange(long start, long end) {

    public long length() {
        return end - start + 1;
    }

    public String toContentRangeValue(long totalSize) {
        return "bytes " + start + "-" + end + "/" + totalSize;
    }

    public String toS3RangeValue() {
        return "bytes=" + start + "-" + end;
    }

    public static HttpByteRange parse(String rangeHeader, long totalSize) {
        if (!org.springframework.util.StringUtils.hasText(rangeHeader)) {
            throw new BusinessException(416, "range required");
        }
        String trimmed = rangeHeader.trim();
        if (!trimmed.regionMatches(true, 0, "bytes=", 0, 6)) {
            throw new BusinessException(416, "unsupported range unit");
        }
        String spec = trimmed.substring(6).split(",", 2)[0].trim();
        int dash = spec.indexOf('-');
        if (dash < 0) {
            throw new BusinessException(416, "invalid range");
        }
        String startText = spec.substring(0, dash).trim();
        String endText = spec.substring(dash + 1).trim();
        if (totalSize <= 0) {
            throw new BusinessException(416, "invalid range");
        }

        long lastIndex = totalSize - 1;
        long start;
        long end;
        if (startText.isEmpty()) {
            long suffixLength = Long.parseLong(endText);
            if (suffixLength <= 0) {
                throw new BusinessException(416, "invalid range");
            }
            start = Math.max(0, totalSize - suffixLength);
            end = lastIndex;
        } else {
            start = Long.parseLong(startText);
            end = endText.isEmpty() ? lastIndex : Long.parseLong(endText);
        }

        if (start < 0 || end < start || start > lastIndex) {
            throw new BusinessException(416, "invalid range");
        }
        end = Math.min(end, lastIndex);
        return new HttpByteRange(start, end);
    }
}
