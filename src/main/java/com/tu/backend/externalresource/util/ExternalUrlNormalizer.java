package com.tu.backend.externalresource.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

/**
 * Canonical URL normalization aligned with tu-web-ts externalUrlResource.ts.
 */
public final class ExternalUrlNormalizer {

    private ExternalUrlNormalizer() {
    }

    public record ParsedExternalUrl(
        String href,
        String baseUrl,
        String anchor,
        boolean excerptMode
    ) {
    }

    public static ParsedExternalUrl parse(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        if (value.isBlank() || value.contains(" ") || value.contains("\n") || value.contains("\r")) {
            return null;
        }
        URI uri;
        try {
            uri = new URI(value);
        } catch (URISyntaxException ex) {
            return null;
        }
        String scheme = uri.getScheme();
        if (scheme == null) {
            return null;
        }
        String lowerScheme = scheme.toLowerCase(Locale.ROOT);
        if (!"http".equals(lowerScheme) && !"https".equals(lowerScheme)) {
            return null;
        }

        String fragment = uri.getFragment();
        String anchor = fragment == null || fragment.isBlank() ? null : decodeURIComponent(fragment);
        String baseUrl = toBasePageUrl(uri);
        return new ParsedExternalUrl(value, baseUrl, anchor, anchor != null);
    }

    public static String toBasePageUrl(String raw) {
        ParsedExternalUrl parsed = parse(raw);
        return parsed == null ? null : parsed.baseUrl();
    }

    public static String toBasePageUrl(URI uri) {
        try {
            URI normalized = new URI(
                uri.getScheme(),
                uri.getUserInfo(),
                uri.getHost(),
                uri.getPort(),
                normalizePathname(uri.getPath()),
                uri.getQuery(),
                null
            );
            return normalized.toString();
        } catch (URISyntaxException ex) {
            return uri.toString();
        }
    }

    public static String normalizePathname(String pathname) {
        if (pathname == null || pathname.isBlank() || "/".equals(pathname)) {
            return "/";
        }
        String trimmed = pathname.replaceAll("/+$", "");
        return trimmed.isEmpty() ? "/" : trimmed;
    }

    private static String decodeURIComponent(String value) {
        try {
            return java.net.URLDecoder.decode(value, java.nio.charset.StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            return value;
        }
    }
}
