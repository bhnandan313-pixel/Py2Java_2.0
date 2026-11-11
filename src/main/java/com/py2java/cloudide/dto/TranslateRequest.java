package com.py2java.cloudide.dto;

import jakarta.validation.constraints.NotNull;
import com.py2java.cloudide.dto.validation.CodePointSize;

public record TranslateRequest(
        @NotNull
        @CodePointSize(max = 20000)
        String code) {
}
