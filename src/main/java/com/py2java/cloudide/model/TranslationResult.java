package com.py2java.cloudide.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record TranslationResult(
        @JsonProperty("java") String javaSource,
        List<String> warnings,
        String explanation) {
}
