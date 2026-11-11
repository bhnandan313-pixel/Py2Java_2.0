package com.py2java.cloudide.executor;

import com.py2java.cloudide.config.DockerExecutionProperties;
import com.py2java.cloudide.model.ExecutionResult;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class JavaExecutor extends CodeExecutor {
    private final DockerExecutionProperties properties;

    public JavaExecutor(DockerCli dockerCli, DockerExecutionProperties properties) {
        super(dockerCli, properties);
        this.properties = properties;
    }

    @Override
    public String language() {
        return "java";
    }

    @Override
    public ExecutionResult execute(String code) {
        return executeInDocker(code);
    }

    @Override
    protected String image() {
        return properties.getJavaImage();
    }

    @Override
    protected String sourceFileName() {
        return "Main.java";
    }

    @Override
    protected List<String> executionCommand() {
        return List.of("sh", "-lc",
                "mkdir -p /tmp/classes && javac -d /tmp/classes /workspace/Main.java && java -cp /tmp/classes Main");
    }
}
