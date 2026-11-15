package com.py2java.cloudide.ai;

public interface AiProviderClient {
    String completeOpenAiCompatible(
            String baseUrl, String model, String apiKey, String systemPrompt, String userPrompt);

    String completeOllama(String baseUrl, String model, String systemPrompt, String userPrompt);
}
