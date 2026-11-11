package com.py2java.cloudide.executor;

import com.py2java.cloudide.config.DockerExecutionProperties;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DockerCli {
    private static final Logger LOGGER = LoggerFactory.getLogger(DockerCli.class);
    private final DockerExecutionProperties properties;

    public DockerCli(DockerExecutionProperties properties) {
        this.properties = properties;
    }

    public boolean isAvailable() {
        Process process;
        try {
            process = new ProcessBuilder("docker", "info")
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
        } catch (IOException exception) {
            LOGGER.warn("Docker CLI is unavailable: {}", exception.getMessage());
            return false;
        }

        try {
            if (!process.waitFor(properties.getInfoTimeoutSeconds(), TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return false;
            }
            return process.exitValue() == 0;
        } catch (InterruptedException exception) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            LOGGER.warn("Interrupted while checking Docker availability");
            return false;
        }
    }

    public ProcessResult run(Duration timeout, String... command) {
        Process process;
        try {
            process = new ProcessBuilder(command).start();
        } catch (IOException exception) {
            throw new ExecutionInfrastructureException("Could not start Docker command.", exception);
        }

        CompletableFuture<byte[]> stdout = readAsync(process.getInputStream());
        CompletableFuture<byte[]> stderr = readAsync(process.getErrorStream());
        boolean timedOut = false;
        try {
            if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                timedOut = true;
                process.destroyForcibly();
                process.waitFor();
            }
            return new ProcessResult(decode(stdout), decode(stderr),
                    timedOut ? 124 : process.exitValue(), timedOut);
        } catch (InterruptedException exception) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new ExecutionInfrastructureException("Interrupted while waiting for Docker.", exception);
        }
    }

    private CompletableFuture<byte[]> readAsync(InputStream stream) {
        return CompletableFuture.supplyAsync(() -> {
            try (stream; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                stream.transferTo(output);
                return output.toByteArray();
            } catch (IOException exception) {
                throw new ExecutionInfrastructureException("Could not read Docker output.", exception);
            }
        }, task -> Thread.startVirtualThread(task));
    }

    private String decode(CompletableFuture<byte[]> output) {
        try {
            return new String(output.get(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ExecutionInfrastructureException("Interrupted while reading Docker output.", exception);
        } catch (ExecutionException exception) {
            throw new ExecutionInfrastructureException("Could not read Docker output.", exception.getCause());
        }
    }

    public record ProcessResult(String stdout, String stderr, int exitCode, boolean timedOut) {
    }
}
