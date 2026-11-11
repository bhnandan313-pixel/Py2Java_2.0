package com.py2java.cloudide.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import com.py2java.cloudide.dto.validation.CodePointSize;

public record RunRequest(
        @NotNull
        @Pattern(regexp = "python|java")
        String language,
        @NotNull
        @CodePointSize(min = 1, max = 20000)
        String code) {
}
