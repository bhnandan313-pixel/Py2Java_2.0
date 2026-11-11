package com.py2java.cloudide;

import com.py2java.cloudide.config.DockerExecutionProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(DockerExecutionProperties.class)
public class Py2JavaApplication {
    public static void main(String[] args) {
        SpringApplication.run(Py2JavaApplication.class, args);
    }
}
