package com.py2java.cloudide.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HttpAiProviderClientTest {
    private final AtomicReference<String> requestPath = new AtomicReference<>();
    private final AtomicReference<String> authorization = new AtomicReference<>();
    private final AtomicReference<String> requestBody = new AtomicReference<>();
    private HttpServer server;
    private HttpAiProviderClient client;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", exchange -> {
            requestPath.set(exchange.getRequestURI().getPath());
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            String content = "/api/chat".equals(requestPath.get())
                    ? "{\"message\":{\"content\":\"local answer\"}}"
                    : "{\"choices\":[{\"message\":{\"content\":\"cloud answer\"}}]}";
            byte[] response = content.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (var output = exchange.getResponseBody()) {
                output.write(response);
            }
        });
        server.start();
        client = new HttpAiProviderClient(new ObjectMapper());
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void sendsOpenAiCompatibleChatCompletionWithBearerKey() throws Exception {
        String baseUrl = baseUrl() + "/v1";

        String content = client.completeOpenAiCompatible(
                baseUrl, "test-model", "test-api-key", "system prompt", "user prompt");

        JsonNode body = new ObjectMapper().readTree(requestBody.get());
        assertEquals("cloud answer", content);
        assertEquals("/v1/chat/completions", requestPath.get());
        assertEquals("Bearer test-api-key", authorization.get());
        assertEquals("test-model", body.path("model").asText());
        assertEquals("system prompt", body.at("/messages/0/content").asText());
        assertEquals("user prompt", body.at("/messages/1/content").asText());
    }

    @Test
    void sendsOllamaChatRequestWithoutAnApiKey() {
        String content = client.completeOllama(baseUrl(), "local-model", "system prompt", "user prompt");

        assertEquals("local answer", content);
        assertEquals("/api/chat", requestPath.get());
        assertNull(authorization.get());
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }
}
