package com.py2java.cloudide.dto;

import com.py2java.cloudide.dto.validation.CodePointSize;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AiAssistRequest(
        @NotNull
        @Pattern(regexp = "explain|compare|translate|question")
        String task,
        @NotNull
        @Pattern(regexp = "python|java")
        String language,
        @NotNull
        @CodePointSize(min = 1, max = 20000)
        String code,
        @Size(max = 20000)
        String javaCode,
        @Size(max = 4000)
        String question,
        @Size(max = 2048)
        String baseUrl,
        @Size(max = 256)
        String model,
        @Size(max = 4096)
        String apiKey,
        @Size(max = 2048)
        String ollamaUrl,
        @Size(max = 256)
        String ollamaModel,
        @Pattern(regexp = "auto|ollama-only")
        String mode) {
}
