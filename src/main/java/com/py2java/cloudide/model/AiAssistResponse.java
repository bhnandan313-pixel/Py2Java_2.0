package com.py2java.cloudide.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiAssistResponse(
        String task,
        String provider,
        String content,
        @JsonProperty("used_fallback") boolean usedFallback,
        String warning) {
}
