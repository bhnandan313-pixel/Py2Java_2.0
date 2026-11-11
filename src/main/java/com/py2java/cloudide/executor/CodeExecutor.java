package com.py2java.cloudide.executor;

import com.py2java.cloudide.config.DockerExecutionProperties;
import com.py2java.cloudide.model.ExecutionResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;

public abstract class CodeExecutor {
    private final DockerCli dockerCli;
    private final DockerExecutionProperties properties;

    protected CodeExecutor(DockerCli dockerCli, DockerExecutionProperties properties) {
        this.dockerCli = dockerCli;
        this.properties = properties;
    }

    public abstract String language();

    public abstract ExecutionResult execute(String code);

    protected abstract String image();

    protected abstract String sourceFileName();

    protected abstract List<String> executionCommand();

    protected final ExecutionResult executeInDocker(String code) {
        Path createdDirectory = null;
        try {
            createdDirectory = Files.createTempDirectory("py2java-");
            Path sourceFile = createdDirectory.resolve(sourceFileName());
            Files.writeString(sourceFile, code);
            setContainerReadablePermissions(createdDirectory, sourceFile);
        } catch (IOException | UnsupportedOperationException exception) {
            if (createdDirectory != null) {
                try {
                    deleteTemporaryDirectory(createdDirectory);
                } catch (ExecutionInfrastructureException cleanupException) {
                    exception.addSuppressed(cleanupException);
                }
            }
            throw new ExecutionInfrastructureException("Could not prepare code for execution.", exception);
        }

        Path workDirectory = createdDirectory;
        try {
            List<String> command = new ArrayList<>(List.of(
                    "docker", "run", "--rm", "--network", "none",
                    "--memory", properties.getMemoryLimit(),
                    "--cpus", properties.getCpuLimit(),
                    "--pids-limit", Integer.toString(properties.getPidsLimit()),
                    "--read-only",
                    "--tmpfs", properties.getTmpfsOptions(),
                    "--cap-drop", "ALL",
                    "--security-opt", "no-new-privileges",
                    "--user", properties.getContainerUser(),
                    "-v", workDirectory + ":/workspace:ro",
                    "-w", "/tmp",
                    image()));
            command.addAll(executionCommand());
            DockerCli.ProcessResult result = dockerCli.run(
                    Duration.ofSeconds(properties.getExecutionTimeoutSeconds()),
                    command.toArray(String[]::new));
            if (result.timedOut()) {
                return new ExecutionResult(tail(result.stdout(), 12000),
                        "Execution timed out (8 second limit).", 124, true);
            }
            return new ExecutionResult(tail(result.stdout(), 12000),
                    tail(result.stderr(), 6000), result.exitCode(), false);
        } finally {
            deleteTemporaryDirectory(workDirectory);
        }
    }

    private void setContainerReadablePermissions(Path directory, Path sourceFile) throws IOException {
        if (Files.getFileStore(directory).supportsFileAttributeView("posix")) {
            Files.setPosixFilePermissions(directory, PosixFilePermissions.fromString("rwx--x--x"));
            Files.setPosixFilePermissions(sourceFile, PosixFilePermissions.fromString("rw-r--r--"));
        }
    }

    private String tail(String value, int maxLength) {
        int codePointLength = value.codePointCount(0, value.length());
        int start = codePointLength > maxLength
                ? value.offsetByCodePoints(0, codePointLength - maxLength)
                : 0;
        return value.substring(start);
    }

    private void deleteTemporaryDirectory(Path directory) {
        try (var paths = Files.walk(directory)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        } catch (IOException exception) {
            throw new ExecutionInfrastructureException("Could not clean up temporary code files.", exception);
        }
    }
}
