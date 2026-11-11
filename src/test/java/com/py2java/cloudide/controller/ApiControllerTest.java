package com.py2java.cloudide.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.py2java.cloudide.exception.ApiExceptionHandler;
import com.py2java.cloudide.exception.DockerUnavailableException;
import com.py2java.cloudide.model.ExecutionResult;
import com.py2java.cloudide.model.TerminalResult;
import com.py2java.cloudide.model.TranslationOutput;
import com.py2java.cloudide.service.CodeExecutionService;
import com.py2java.cloudide.translator.CodeTranslationService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ApiController.class)
@Import(ApiExceptionHandler.class)
class ApiControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CodeExecutionService executionService;

    @MockBean
    private CodeTranslationService translationService;

    @Test
    void healthResponseUsesTheExistingSnakeCaseContract() throws Exception {
        when(executionService.dockerAvailable()).thenReturn(false);

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"ok\":true,\"docker_available\":false}"));
    }

    @Test
    void runResponseAndDockerUnavailableErrorKeepTheirContracts() throws Exception {
        when(executionService.run("python", "print(1)"))
                .thenReturn(new ExecutionResult("1\n", "", 0, false));
        mockMvc.perform(post("/api/run").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"language\":\"python\",\"code\":\"print(1)\"}"))
                .andExpect(status().isOk())
                .andExpect(content().json(
                        "{\"stdout\":\"1\\n\",\"stderr\":\"\",\"exit_code\":0,\"timed_out\":false}"));

        when(executionService.run("python", "print(2)"))
                .thenThrow(new DockerUnavailableException("Docker is not running."));
        mockMvc.perform(post("/api/run").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"language\":\"python\",\"code\":\"print(2)\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().json("{\"detail\":\"Docker is not running.\"}"));
    }

    @Test
    void translateAndTerminalResponsesKeepTheirExistingShapes() throws Exception {
        when(translationService.translate(anyString()))
                .thenReturn(new TranslationOutput("public class Main {}", List.of("warning"), "Review this output."));
        mockMvc.perform(post("/api/translate").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"pass\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.java").value("public class Main {}"))
                .andExpect(jsonPath("$.warnings[0]").value("warning"))
                .andExpect(jsonPath("$.explanation").exists());

        when(executionService.terminal("pwd"))
                .thenReturn(new TerminalResult(
                        "Terminal command is not enabled in this starter. Use Run for code execution.", "", 0));
        mockMvc.perform(post("/api/terminal").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"command\":\"pwd\"}"))
                .andExpect(status().isOk())
                .andExpect(content().json(
                        "{\"stdout\":\"Terminal command is not enabled in this starter. Use Run for code execution.\",\"stderr\":\"\",\"exit_code\":0}"));
    }

    @Test
    void validatesExistingRequestConstraintsAsUnprocessableEntity() throws Exception {
        mockMvc.perform(post("/api/run").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"language\":\"ruby\",\"code\":\"\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").isArray());

        mockMvc.perform(post("/api/translate").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + "x".repeat(20001) + "\"}"))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(post("/api/terminal").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"command\":\"whoami\"}"))
                .andExpect(status().isUnprocessableEntity());

        when(translationService.translate(anyString()))
                .thenReturn(new TranslationOutput("public class Main {}", List.of(), "Review this output."));
        mockMvc.perform(post("/api/translate").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + "😀".repeat(10001) + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void servesTheExistingIndexAtTheRoot() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("PY2JAVA Cloud IDE")));
    }
}
