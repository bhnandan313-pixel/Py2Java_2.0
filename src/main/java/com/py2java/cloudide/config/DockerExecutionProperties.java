package com.py2java.cloudide.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "py2java.docker")
public class DockerExecutionProperties {
    private String pythonImage = "python:3.12-alpine";
    private String javaImage = "eclipse-temurin:21-jdk";
    private long executionTimeoutSeconds = 8;
    private long infoTimeoutSeconds = 5;
    private String memoryLimit = "128m";
    private String cpuLimit = "0.5";
    private int pidsLimit = 64;
    private String tmpfsOptions = "/tmp:rw,noexec,nosuid,size=64m";
    private String containerUser = "10001:10001";

    public String getPythonImage() {
        return pythonImage;
    }

    public void setPythonImage(String pythonImage) {
        this.pythonImage = pythonImage;
    }

    public String getJavaImage() {
        return javaImage;
    }

    public void setJavaImage(String javaImage) {
        this.javaImage = javaImage;
    }

    public long getExecutionTimeoutSeconds() {
        return executionTimeoutSeconds;
    }

    public void setExecutionTimeoutSeconds(long executionTimeoutSeconds) {
        this.executionTimeoutSeconds = executionTimeoutSeconds;
    }

    public long getInfoTimeoutSeconds() {
        return infoTimeoutSeconds;
    }

    public void setInfoTimeoutSeconds(long infoTimeoutSeconds) {
        this.infoTimeoutSeconds = infoTimeoutSeconds;
    }

    public String getMemoryLimit() {
        return memoryLimit;
    }

    public void setMemoryLimit(String memoryLimit) {
        this.memoryLimit = memoryLimit;
    }

    public String getCpuLimit() {
        return cpuLimit;
    }

    public void setCpuLimit(String cpuLimit) {
        this.cpuLimit = cpuLimit;
    }

    public int getPidsLimit() {
        return pidsLimit;
    }

    public void setPidsLimit(int pidsLimit) {
        this.pidsLimit = pidsLimit;
    }

    public String getTmpfsOptions() {
        return tmpfsOptions;
    }

    public void setTmpfsOptions(String tmpfsOptions) {
        this.tmpfsOptions = tmpfsOptions;
    }

    public String getContainerUser() {
        return containerUser;
    }

    public void setContainerUser(String containerUser) {
        this.containerUser = containerUser;
    }
}
