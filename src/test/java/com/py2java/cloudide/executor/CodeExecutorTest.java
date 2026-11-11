package com.py2java.cloudide.executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.py2java.cloudide.config.DockerExecutionProperties;
import com.py2java.cloudide.model.ExecutionResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CodeExecutorTest {
    @Mock
    private DockerCli dockerCli;

    @Test
    void pythonExecutionUsesTheRequiredContainerRestrictions() throws Exception {
        AtomicReference<Path> sourceFile = new AtomicReference<>();
        AtomicReference<String[]> commandLine = new AtomicReference<>();
        when(dockerCli.run(eq(Duration.ofSeconds(8)), any(String[].class))).thenAnswer(invocation -> {
            String[] command = java.util.Arrays.stream(invocation.getArguments())
                    .skip(1)
                    .map(String.class::cast)
                    .toArray(String[]::new);
            commandLine.set(command);
            int volumeIndex = Arrays.asList(command).indexOf("-v");
            Path directory = Path.of(command[volumeIndex + 1].split(":/workspace:ro")[0]);
            sourceFile.set(directory.resolve("main.py"));
            assertTrue(Files.exists(sourceFile.get()));
            if (Files.getFileStore(directory).supportsFileAttributeView("posix")) {
                assertTrue(Files.getPosixFilePermissions(directory)
                        .contains(java.nio.file.attribute.PosixFilePermission.OTHERS_EXECUTE));
                assertTrue(Files.getPosixFilePermissions(sourceFile.get())
                        .contains(java.nio.file.attribute.PosixFilePermission.OTHERS_READ));
            }
            return new DockerCli.ProcessResult("done", "", 0, false);
        });
        PythonExecutor executor = new PythonExecutor(dockerCli, new DockerExecutionProperties());

        assertEquals(new ExecutionResult("done", "", 0, false), executor.execute("print('ok')"));

        verify(dockerCli).run(eq(Duration.ofSeconds(8)), any(String[].class));
        var args = Arrays.asList(commandLine.get());
        assertTrue(args.containsAll(Arrays.asList(
                "--network", "none", "--memory", "128m", "--cpus", "0.5",
                "--pids-limit", "64", "--read-only", "--tmpfs",
                "/tmp:rw,noexec,nosuid,size=64m", "--cap-drop", "ALL",
                "--security-opt", "no-new-privileges", "--user", "10001:10001",
                "python:3.12-alpine", "python", "-I", "/workspace/main.py")));
        assertFalse(Files.exists(sourceFile.get()));
    }

    @Test
    void javaExecutionUsesTheExistingCompileAndRunCommand() {
        AtomicReference<String[]> commandLine = new AtomicReference<>();
        when(dockerCli.run(eq(Duration.ofSeconds(8)), any(String[].class)))
                .thenAnswer(invocation -> {
                    commandLine.set(java.util.Arrays.stream(invocation.getArguments())
                            .skip(1)
                            .map(String.class::cast)
                            .toArray(String[]::new));
                    return new DockerCli.ProcessResult("", "", 0, false);
                });
        JavaExecutor executor = new JavaExecutor(dockerCli, new DockerExecutionProperties());

        executor.execute("public class Main {}");

        verify(dockerCli).run(eq(Duration.ofSeconds(8)), any(String[].class));
        var args = Arrays.asList(commandLine.get());
        assertTrue(args.contains("eclipse-temurin:21-jdk"));
        assertTrue(args.containsAll(Arrays.asList(
                "sh", "-lc",
                "mkdir -p /tmp/classes && javac -d /tmp/classes /workspace/Main.java && java -cp /tmp/classes Main")));
    }
}
