package com.py2java.cloudide.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record TerminalRequest(
        @NotNull
        @Pattern(regexp = "python --version|java -version|javac -version|pwd|ls")
        String command) {
}
