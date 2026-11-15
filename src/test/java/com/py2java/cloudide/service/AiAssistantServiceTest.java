package com.py2java.cloudide.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.py2java.cloudide.ai.AiProviderClient;
import com.py2java.cloudide.ai.AiProviderException;
import com.py2java.cloudide.dto.AiAssistRequest;
import com.py2java.cloudide.exception.AiAssistantException;
import com.py2java.cloudide.exception.AiRequestException;
import com.py2java.cloudide.model.AiAssistResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiAssistantServiceTest {
    @Mock
    private AiProviderClient providerClient;

    @InjectMocks
    private AiAssistantService service;

    @Test
    void usesCloudProviderWhenSessionCredentialsArePresent() {
        AiAssistRequest request = request("explain", "api-key", "https://api.example.test/v1", "model");
        when(providerClient.completeOpenAiCompatible(
                        eq(request.baseUrl()), eq(request.model()), eq(request.apiKey()), anyString(), anyString()))
                .thenReturn("Explanation");

        AiAssistResponse response = service.assist(request);

        assertEquals("OpenAI-compatible", response.provider());
        assertEquals("Explanation", response.content());
        assertFalse(response.usedFallback());
        verify(providerClient, never()).completeOllama(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void ollamaOnlyModeSkipsCloudProviderEvenWhenCredentialsArePresent() {
        AiAssistRequest request = new AiAssistRequest(
                "explain",
                "python",
                "print(1)",
                "",
                "",
                "https://api.example.test/v1",
                "model",
                "api-key",
                "",
                "",
                "ollama-only");
        when(providerClient.completeOllama(anyString(), anyString(), anyString(), anyString()))
                .thenReturn("Local explanation");

        AiAssistResponse response = service.assist(request);

        assertEquals("Ollama", response.provider());
        assertEquals("Local explanation", response.content());
        assertFalse(response.usedFallback());
        assertTrue(response.warning().contains("cloud APIs are not used"));
        verify(providerClient, never())
                .completeOpenAiCompatible(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void fallsBackToOllamaAndRemovesJavaCodeFencesAfterCloudFailure() {
        AiAssistRequest request = request("translate", "api-key", "https://api.example.test/v1", "model");
        when(providerClient.completeOpenAiCompatible(
                        eq(request.baseUrl()), eq(request.model()), eq(request.apiKey()), anyString(), anyString()))
                .thenThrow(new AiProviderException("Cloud AI request failed (HTTP 401)."));
        when(providerClient.completeOllama(anyString(), anyString(), anyString(), anyString()))
                .thenReturn("```java\npublic class Main { public static void main(String[] args) {} }\n```");

        AiAssistResponse response = service.assist(request);

        assertEquals("Ollama", response.provider());
        assertEquals("public class Main { public static void main(String[] args) {} }", response.content());
        assertTrue(response.usedFallback());
        assertTrue(response.warning().contains("HTTP 401"));
    }

    @Test
    void translationReturnsOnlyTheExecutableJavaClass() {
        AiAssistRequest request = request("translate", "api-key", "https://api.example.test/v1", "model");
        when(providerClient.completeOpenAiCompatible(
                        eq(request.baseUrl()), eq(request.model()), eq(request.apiKey()), anyString(), anyString()))
                .thenReturn("Here is the translation:\n```java\n"
                        + "import java.util.List;\n\n"
                        + "public class Main {\n"
                        + "    public static void main(String[] args) {\n"
                        + "        String brace = \"}\";\n"
                        + "        // }\n"
                        + "        System.out.println(brace);\n"
                        + "    }\n"
                        + "}\n"
                        + "```\nThis program prints a brace.\nConclusion: it uses Java.");

        AiAssistResponse response = service.assist(request);

        assertEquals(
                "import java.util.List;\n\n"
                        + "public class Main {\n"
                        + "    public static void main(String[] args) {\n"
                        + "        String brace = \"}\";\n"
                        + "        // }\n"
                        + "        System.out.println(brace);\n"
                        + "    }\n"
                        + "}",
                response.content());
        assertFalse(response.content().contains("Conclusion"));
        assertFalse(response.content().contains("Here is the translation"));
    }

    @Test
    void asksForCredentialsWhenOllamaIsUnavailableAndNoCloudKeyWasProvided() {
        when(providerClient.completeOllama(anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new AiProviderException("Could not connect to Ollama."));

        AiAssistantException exception = assertThrows(
                AiAssistantException.class,
                () -> service.assist(request("explain", "", "", "")));

        assertTrue(exception.getMessage().contains("Add an API key"));
    }

    @Test
    void requiresAQuestionForFreeformRequests() {
        AiAssistRequest request = new AiAssistRequest(
                "question", "python", "print(1)", null, " ", "", "", "", "", "", "auto");

        assertThrows(AiRequestException.class, () -> service.assist(request));
        verify(providerClient, never()).completeOllama(anyString(), anyString(), anyString(), anyString());
    }

    private AiAssistRequest request(String task, String apiKey, String baseUrl, String model) {
        return new AiAssistRequest(
                task, "python", "print(1)", "", "", baseUrl, model, apiKey, "", "", "auto");
    }
}
