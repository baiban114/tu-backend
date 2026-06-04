package com.tu.backend.externalresource.service;

import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PageTitleFetcherService {

    private static final Pattern TITLE_PATTERN = Pattern.compile(
        "<title[^>]*>([^<]+)</title>",
        Pattern.CASE_INSENSITIVE
    );

    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(3))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();

    public Optional<String> fetchTitle(String url) {
        if (url == null || url.isBlank()) {
            return Optional.empty();
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url.trim()))
                .timeout(Duration.ofSeconds(5))
                .header("User-Agent", "tu-resource-bot/1.0")
                .GET()
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 400) {
                return Optional.empty();
            }
            String body = response.body();
            if (body == null || body.isBlank()) {
                return Optional.empty();
            }
            Matcher matcher = TITLE_PATTERN.matcher(body);
            if (!matcher.find()) {
                return Optional.empty();
            }
            String title = matcher.group(1).replaceAll("\\s+", " ").trim();
            if (title.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(title.length() > 255 ? title.substring(0, 255) : title);
        } catch (Exception ex) {
            return Optional.empty();
        }
    }
}
