package com.tu.backend.externalresource.model;

import java.util.Locale;

public enum VariantKind {
    TRANSLATION,
    FORMAT,
    EDITION,
    MIRROR,
    OTHER;

    public String code() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static VariantKind fromCode(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (VariantKind kind : values()) {
            if (kind.code().equals(normalized)) {
                return kind;
            }
        }
        throw new IllegalArgumentException("unknown variant kind: " + value);
    }

    public static String normalizeCode(String value) {
        return value == null || value.isBlank() ? null : fromCode(value).code();
    }
}
