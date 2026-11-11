package com.py2java.cloudide.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record HealthStatus(
        boolean ok,
        @JsonProperty("docker_available") boolean dockerAvailable) {
}
