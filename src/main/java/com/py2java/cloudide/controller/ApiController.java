package com.py2java.cloudide.controller;

import com.py2java.cloudide.dto.RunRequest;
import com.py2java.cloudide.dto.TerminalRequest;
import com.py2java.cloudide.dto.TranslateRequest;
import com.py2java.cloudide.model.ExecutionResult;
import com.py2java.cloudide.model.HealthStatus;
import com.py2java.cloudide.model.TerminalResult;
import com.py2java.cloudide.model.TranslationResult;
import com.py2java.cloudide.service.CodeExecutionService;
import com.py2java.cloudide.translator.CodeTranslationService;
import jakarta.validation.Valid;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ApiController {
    private final CodeExecutionService executionService;
    private final CodeTranslationService translationService;

    public ApiController(
            CodeExecutionService executionService,
            CodeTranslationService translationService) {
        this.executionService = executionService;
        this.translationService = translationService;
    }

    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    public Resource index() {
        return new ClassPathResource("static/index.html");
    }

    @GetMapping("/api/health")
    public HealthStatus health() {
        return new HealthStatus(true, executionService.dockerAvailable());
    }

    @PostMapping("/api/run")
    public ExecutionResult run(@Valid @RequestBody RunRequest request) {
        return executionService.run(request.language(), request.code());
    }

    @PostMapping("/api/terminal")
    public TerminalResult terminal(@Valid @RequestBody TerminalRequest request) {
        return executionService.terminal(request.command());
    }

    @PostMapping("/api/translate")
    public TranslationResult translate(@Valid @RequestBody TranslateRequest request) {
        var translated = translationService.translate(request.code());
        return new TranslationResult(
                translated.java(),
                translated.warnings(),
                translated.explanation());
    }
}
