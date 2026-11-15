package com.py2java.cloudide.exception;

import com.py2java.cloudide.executor.ExecutionInfrastructureException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(AiAssistantException.class)
    public ResponseEntity<ApiError> aiUnavailable(AiAssistantException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ApiError(exception.getMessage()));
    }

    @ExceptionHandler(AiRequestException.class)
    public ResponseEntity<ApiError> invalidAiRequest(AiRequestException exception) {
        return ResponseEntity.unprocessableEntity().body(new ApiError(exception.getMessage()));
    }

    @ExceptionHandler(DockerUnavailableException.class)
    public ResponseEntity<ApiError> dockerUnavailable(DockerUnavailableException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ApiError(exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrors> validation(MethodArgumentNotValidException exception) {
        List<ValidationError> details = exception.getBindingResult().getFieldErrors().stream()
                .map(this::toValidationError)
                .toList();
        return ResponseEntity.unprocessableEntity().body(new ValidationErrors(details));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ValidationErrors> invalidJson(HttpMessageNotReadableException exception) {
        return ResponseEntity.unprocessableEntity().body(new ValidationErrors(List.of(
                new ValidationError(List.of("body"), "Field required", "missing"))));
    }

    @ExceptionHandler(ExecutionInfrastructureException.class)
    public ResponseEntity<ApiError> infrastructure(ExecutionInfrastructureException exception) {
        return ResponseEntity.internalServerError().body(new ApiError(exception.getMessage()));
    }

    private ValidationError toValidationError(FieldError error) {
        String field = error.getField();
        String annotation = error.getCode();
        if ("NotNull".equals(annotation)) {
            return new ValidationError(List.of("body", field), "Field required", "missing");
        }
        if ("Pattern".equals(annotation)) {
            String allowed = field.equals("language")
                    ? "Input should be 'python' or 'java'"
                    : "Input should be 'python --version', 'java -version', 'javac -version', 'pwd' or 'ls'";
            return new ValidationError(List.of("body", field), allowed, "literal_error");
        }
        if ("CodePointSize".equals(annotation)) {
            String value = error.getRejectedValue() instanceof String text ? text : "";
            int length = value.codePointCount(0, value.length());
            if (length == 0 && error.getObjectName().toLowerCase().contains("run")) {
                return new ValidationError(
                        List.of("body", field), "String should have at least 1 character", "string_too_short");
            }
            return new ValidationError(
                    List.of("body", field), "String should have at most 20000 characters", "string_too_long");
        }
        return new ValidationError(List.of("body", field), error.getDefaultMessage(), "value_error");
    }

    public record ApiError(String detail) {
    }

    public record ValidationErrors(List<ValidationError> detail) {
    }

    public record ValidationError(List<String> loc, String msg, String type) {
    }
}
