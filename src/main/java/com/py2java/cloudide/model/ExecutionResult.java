package com.py2java.cloudide.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ExecutionResult(
        String stdout,
        String stderr,
        @JsonProperty("exit_code") int exitCode,
        @JsonProperty("timed_out") boolean timedOut) {
}
