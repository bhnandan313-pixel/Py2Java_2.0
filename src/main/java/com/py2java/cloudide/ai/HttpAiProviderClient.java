package com.py2java.cloudide.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class HttpAiProviderClient implements AiProviderClient {
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(8);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(90);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public HttpAiProviderClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
    }

    @Override
    public String completeOpenAiCompatible(
            String baseUrl, String model, String apiKey, String systemPrompt, String userPrompt) {
        URI endpoint = endpoint(baseUrl, "/chat/completions", "Cloud AI");
        Map<String, Object> body = Map.of(
                "model", model,
                "messages", messages(systemPrompt, userPrompt));
        JsonNode response = send(endpoint, body, "Cloud AI", apiKey);
        return messageContent(response.at("/choices/0/message/content"), "Cloud AI");
    }

    @Override
    public String completeOllama(String baseUrl, String model, String systemPrompt, String userPrompt) {
        URI endpoint = endpoint(baseUrl, "/api/chat", "Ollama");
        Map<String, Object> body = Map.of(
                "model", model,
                "stream", false,
                "messages", messages(systemPrompt, userPrompt));
        JsonNode response = send(endpoint, body, "Ollama", null);
        return messageContent(response.at("/message/content"), "Ollama");
    }

    private List<Map<String, String>> messages(String systemPrompt, String userPrompt) {
        return List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt));
    }

    private JsonNode send(URI endpoint, Map<String, Object> body, String provider, String apiKey) {
        String json;
        try {
            json = objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException exception) {
            throw new AiProviderException("Could not encode the " + provider + " request.", exception);
        }

        HttpRequest.Builder request = HttpRequest.newBuilder(endpoint)
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json");
        if (apiKey != null && !apiKey.isBlank()) {
            String trimmedApiKey = apiKey.trim();
            if (trimmedApiKey.chars().anyMatch(Character::isISOControl)) {
                throw new AiProviderException("Cloud API key contains unsupported control characters.");
            }
            request.header("Authorization", "Bearer " + trimmedApiKey);
        }
        HttpResponse<String> response;
        try {
            response = httpClient.send(
                    request.POST(HttpRequest.BodyPublishers.ofString(json)).build(),
                    HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AiProviderException("The " + provider + " request was interrupted.", exception);
        } catch (IOException exception) {
            throw new AiProviderException("Could not connect to " + provider + ".", exception);
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            if ("Ollama".equals(provider) && response.statusCode() == 404) {
                throw new AiProviderException(
                        "Ollama returned HTTP 404. Verify its API is available and the selected model is installed.");
            }
            throw new AiProviderException(
                    provider + " request failed (HTTP " + response.statusCode() + ").");
        }
        try {
            return objectMapper.readTree(response.body());
        } catch (JsonProcessingException exception) {
            throw new AiProviderException(provider + " returned an invalid response.", exception);
        }
    }

    private String messageContent(JsonNode content, String provider) {
        if (!content.isTextual() || content.asText().isBlank()) {
            throw new AiProviderException(provider + " returned an empty response.");
        }
        return content.asText();
    }

    private URI endpoint(String baseUrl, String path, String provider) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new AiProviderException(provider + " endpoint URL is missing.");
        }
        String normalizedBase = baseUrl.trim().replaceAll("/+$", "");
        String endpoint = normalizedBase.endsWith(path)
                ? normalizedBase
                : normalizedBase + path;
        URI uri;
        try {
            uri = URI.create(endpoint);
        } catch (IllegalArgumentException exception) {
            throw new AiProviderException(provider + " endpoint URL is invalid.", exception);
        }
        String scheme = uri.getScheme();
        if (uri.getHost() == null
                || uri.getUserInfo() != null
                || uri.getQuery() != null
                || uri.getFragment() != null
                || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
            throw new AiProviderException(
                    provider + " endpoint must be an HTTP(S) URL without embedded credentials or query parameters.");
        }
        return uri;
    }
}
