package com.py2java.cloudide.executor;

import com.py2java.cloudide.config.DockerExecutionProperties;
import com.py2java.cloudide.model.ExecutionResult;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PythonExecutor extends CodeExecutor {
    private final DockerExecutionProperties properties;

    public PythonExecutor(DockerCli dockerCli, DockerExecutionProperties properties) {
        super(dockerCli, properties);
        this.properties = properties;
    }

    @Override
    public String language() {
        return "python";
    }

    @Override
    public ExecutionResult execute(String code) {
        return executeInDocker(code);
    }

    @Override
    protected String image() {
        return properties.getPythonImage();
    }

    @Override
    protected String sourceFileName() {
        return "main.py";
    }

    @Override
    protected List<String> executionCommand() {
        return List.of("python", "-I", "/workspace/main.py");
    }
}
