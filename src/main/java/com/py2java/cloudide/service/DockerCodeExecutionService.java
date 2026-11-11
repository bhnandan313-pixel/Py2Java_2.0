package com.py2java.cloudide.service;

import com.py2java.cloudide.config.DockerExecutionProperties;
import com.py2java.cloudide.exception.DockerUnavailableException;
import com.py2java.cloudide.executor.CodeExecutor;
import com.py2java.cloudide.executor.DockerCli;
import com.py2java.cloudide.model.ExecutionResult;
import com.py2java.cloudide.model.TerminalResult;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class DockerCodeExecutionService implements CodeExecutionService {
    private final DockerCli dockerCli;
    private final DockerExecutionProperties properties;
    private final Map<String, CodeExecutor> executors;

    public DockerCodeExecutionService(
            DockerCli dockerCli,
            DockerExecutionProperties properties,
            List<CodeExecutor> executors) {
        this.dockerCli = dockerCli;
        this.properties = properties;
        this.executors = executors.stream()
                .collect(Collectors.toUnmodifiableMap(CodeExecutor::language, Function.identity()));
    }

    @Override
    public boolean dockerAvailable() {
        return dockerCli.isAvailable();
    }

    @Override
    public ExecutionResult run(String language, String code) {
        CodeExecutor executor = executors.get(language);
        if (executor == null) {
            throw new IllegalArgumentException("Unsupported language.");
        }
        if (!dockerAvailable()) {
            throw new DockerUnavailableException(
                    "Docker is not running. Start Docker Desktop/Engine and try again.");
        }
        return executor.execute(code);
    }

    @Override
    public TerminalResult terminal(String command) {
        List<String> dockerCommand = switch (command) {
            case "python --version" -> List.of("docker", "run", "--rm", "--network", "none",
                    properties.getPythonImage(), "python", "--version");
            case "java -version" -> List.of("docker", "run", "--rm", "--network", "none",
                    properties.getJavaImage(), "java", "-version");
            case "javac -version" -> List.of("docker", "run", "--rm", "--network", "none",
                    properties.getJavaImage(), "javac", "-version");
            case "pwd", "ls" -> null;
            default -> throw new IllegalArgumentException("Terminal command is not enabled in this starter.");
        };
        if (dockerCommand == null) {
            return new TerminalResult(
                    "Terminal command is not enabled in this starter. Use Run for code execution.", "", 0);
        }
        if (!dockerAvailable()) {
            throw new DockerUnavailableException("Docker is not running.");
        }
        DockerCli.ProcessResult result = dockerCli.run(
                Duration.ofSeconds(8), dockerCommand.toArray(String[]::new));
        if (result.timedOut()) {
            return new TerminalResult("", "Command timed out.", 124);
        }
        return new TerminalResult(result.stdout(), result.stderr(), result.exitCode());
    }
}
