package com.py2java.cloudide.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import com.py2java.cloudide.config.DockerExecutionProperties;
import com.py2java.cloudide.exception.DockerUnavailableException;
import com.py2java.cloudide.executor.CodeExecutor;
import com.py2java.cloudide.executor.DockerCli;
import com.py2java.cloudide.model.ExecutionResult;
import java.util.List;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DockerCodeExecutionServiceTest {
    @Mock
    private DockerCli dockerCli;
    @Mock
    private CodeExecutor pythonExecutor;
    @Mock
    private CodeExecutor javaExecutor;

    @Test
    void selectsTheExecutorByRequestedLanguage() {
        when(pythonExecutor.language()).thenReturn("python");
        when(javaExecutor.language()).thenReturn("java");
        when(dockerCli.isAvailable()).thenReturn(true);
        ExecutionResult expected = new ExecutionResult("ok", "", 0, false);
        when(javaExecutor.execute("class Main {}")).thenReturn(expected);
        DockerCodeExecutionService service = new DockerCodeExecutionService(
                dockerCli, new DockerExecutionProperties(), List.of(pythonExecutor, javaExecutor));

        assertEquals(expected, service.run("java", "class Main {}"));
        verify(javaExecutor).execute("class Main {}");
    }

    @Test
    void doesNotStartAnExecutorWhenDockerIsUnavailable() {
        when(pythonExecutor.language()).thenReturn("python");
        when(dockerCli.isAvailable()).thenReturn(false);
        DockerCodeExecutionService service = new DockerCodeExecutionService(
                dockerCli, new DockerExecutionProperties(), List.of(pythonExecutor));

        DockerUnavailableException exception = assertThrows(
                DockerUnavailableException.class, () -> service.run("python", "print(1)"));

        assertEquals("Docker is not running. Start Docker Desktop/Engine and try again.",
                exception.getMessage());
    }

    @Test
    void terminalKeepsTheCommandAllowlistAndContainerArgumentsRestricted() {
        when(dockerCli.isAvailable()).thenReturn(true);
        AtomicReference<String[]> commandLine = new AtomicReference<>();
        when(dockerCli.run(eq(Duration.ofSeconds(8)), any(String[].class))).thenAnswer(invocation -> {
            commandLine.set(java.util.Arrays.stream(invocation.getArguments())
                    .skip(1)
                    .map(String.class::cast)
                    .toArray(String[]::new));
            return new DockerCli.ProcessResult("", "openjdk 21", 0, false);
        });
        DockerCodeExecutionService service = new DockerCodeExecutionService(
                dockerCli, new DockerExecutionProperties(), List.of());

        assertEquals(new com.py2java.cloudide.model.TerminalResult("", "openjdk 21", 0),
                service.terminal("java -version"));
        assertTrue(java.util.Arrays.asList(commandLine.get()).containsAll(List.of(
                "docker", "run", "--rm", "--network", "none",
                "eclipse-temurin:21-jdk", "java", "-version")));
        assertEquals(
                "Terminal command is not enabled in this starter. Use Run for code execution.",
                service.terminal("pwd").stdout());
        assertThrows(IllegalArgumentException.class, () -> service.terminal("whoami"));
    }
}
