# Application of Object-Oriented Programming Principles in the PY2JAVA Java Backend

## Abstract

This report explains how the Java-based PY2JAVA backend applies the core principles of object-oriented programming (OOP) in a way that is both technically meaningful and consistent with the functional requirements of the project. The backend was migrated from a Python FastAPI implementation to Java 21 with Spring Boot while preserving the original API behavior, Docker execution model, translation workflow, and frontend compatibility. In this implementation, OOP is not treated as a cosmetic requirement but as the fundamental structure used to organize execution logic, validation, translation services, and controller interaction.

The project demonstrates the practical use of encapsulation, inheritance, polymorphism, and abstraction through concrete Java classes and interfaces. These concepts are applied to isolate problem-specific responsibilities, simplify extension, and maintain a clean architecture that mirrors the layered backend design required for this migration.

## 1. Introduction

The PY2JAVA project is a backend system that supports three major capabilities:

1. Executing Python and Java code inside isolated Docker containers.
2. Restricting terminal command usage to a safe allowlist.
3. Translating Python source code into Java source code through a rule-based conversion engine.

The migrated Java version keeps the frontend unchanged while preserving the REST contract expected by the browser client. To achieve this without redesigning the application, the backend uses a layered architecture and OOP design. Object-oriented principles are used to separate concerns between controllers, services, executors, translators, and domain models while maintaining the same end-user behavior.

This report analyzes how these principles are applied in the actual Java implementation.

## 2. Project Architecture and OOP Context

The Java backend is structured around a layered design including:

- Controller layer for HTTP endpoints
- Service layer for business logic
- Executor layer for code execution
- Translator layer for Python-to-Java conversion
- DTOs for request validation
- Models for return data
- Exception handling for centralized error responses
- Configuration for Docker runtime limits and application settings

This architectural arrangement makes the application easier to maintain and extend while preserving the original logic. In object-oriented terms, this structure demonstrates a strong separation of responsibilities, interface-based abstraction, and concrete implementations specialized for each use case.

## 3. Encapsulation

Encapsulation refers to restricting direct access to internal state and exposing only the necessary operations. In this project, encapsulation is implemented through private fields, constructor-based dependency injection, and validation-aware data objects.

### 3.1 Encapsulation in the Controller Layer

The controller class [ApiController.java](PY2JAVA-cloud-ide-starter/src/main/java/com/py2java/cloudide/controller/ApiController.java) uses private final fields for the execution service and translation service:

- `executionService`
- `translationService`

These dependencies are injected through the constructor, ensuring that the controller depends on abstractions rather than concrete implementations. This allows the application to change internals without affecting controller behavior.

The controller exposes HTTP endpoints such as:

- `GET /api/health`
- `POST /api/run`
- `POST /api/terminal`
- `POST /api/translate`

These methods coordinate with the services but do not embed execution logic directly, which preserves the integrity of the application layer and keeps internal behavior hidden.

### 3.2 Encapsulation in DTOs and Validation

The project uses request DTOs such as:

- [RunRequest.java](PY2JAVA-cloud-ide-starter/src/main/java/com/py2java/cloudide/dto/RunRequest.java)
- [TerminalRequest.java](PY2JAVA-cloud-ide-starter/src/main/java/com/py2java/cloudide/dto/TerminalRequest.java)
- [TranslateRequest.java](PY2JAVA-cloud-ide-starter/src/main/java/com/py2java/cloudide/dto/TranslateRequest.java)

These objects package request data and apply validation rules using Jakarta validation annotations. They protect the business layer from invalid or malformed input while ensuring that the request contract remains consistent with the original FastAPI backend.

### 3.3 Encapsulation in Execution Configuration

The class [DockerExecutionProperties.java](PY2JAVA-cloud-ide-starter/src/main/java/com/py2java/cloudide/config/DockerExecutionProperties.java) centralizes runtime constraints such as memory limit, CPU limit, timeout, and container user. These values are stored in configuration objects rather than scattered across classes. This encapsulation limits configuration changes to the configuration layer and prevents accidental modification from unrelated parts of the system.

## 4. Inheritance

Inheritance is used to reduce duplication and reuse common behavior across similar execution types. The most important example is the abstract executor hierarchy built around [CodeExecutor.java](PY2JAVA-cloud-ide-starter/src/main/java/com/py2java/cloudide/executor/CodeExecutor.java).

### 4.1 Abstract Base Class

The base class `CodeExecutor` defines the common execution workflow for all supported languages:

- creation of temporary work directories
- writing user code to disk
- preparing Docker arguments
- enforcing execution timeout
- sanitizing output
- cleaning up temporary files

This class implements the reusable execution pipeline once, and all language-specific behavior inherits from it.

### 4.2 Specialized Executors

The project defines concrete subclasses:

- [PythonExecutor.java](PY2JAVA-cloud-ide-starter/src/main/java/com/py2java/cloudide/executor/PythonExecutor.java)
- [JavaExecutor.java](PY2JAVA-cloud-ide-starter/src/main/java/com/py2java/cloudide/executor/JavaExecutor.java)

These classes extend `CodeExecutor` and provide specific information such as:

- the language identifier
- the Docker image name
- the source file name
- the command used to run the code

This structure ensures that Python and Java execution follow the same secure and monitored workflow while still customizing the runtime behavior for each language.

### 4.3 Why This Is Meaningful

The inheritance model is not arbitrary; it captures a real common behavior among all execution types. The parent class controls the common Docker execution process, while the child classes provide the exact implementation details that differ between Python and Java. This reduces duplicated logic and ensures consistency in execution safety and timeout handling.

## 5. Polymorphism

Polymorphism enables different classes to implement the same method in different ways while preserving a shared interface or inherited contract. In this project, polymorphism is realized through method overriding in the executor hierarchy.

### 5.1 Shared Execution Contract

The abstract class `CodeExecutor` declares the method `execute(String code)`. Both `PythonExecutor` and `JavaExecutor` override this method to customize the actual execution for their language.

Although both concrete classes follow the same parent contract, they execute different source files and Docker commands:

- Python runs `main.py` using the Python image
- Java compiles and runs `Main.java` using the Java image

### 5.2 Dynamic Behavior Selection

The application can select the correct executor at runtime based on the requested language. This is an example of polymorphic behavior because the calling code depends on a common abstraction while the actual execution implementation is chosen dynamically.

This design ensures that the backend can support multiple runtimes without requiring repeated conditional logic throughout the system. The result is a cleaner and more extensible implementation.

## 6. Abstraction

Abstraction hides implementation details and exposes only the essential operations required by higher-level components. The project employs abstraction in two major forms: service interfaces and translation interfaces.

### 6.1 Execution Abstraction

The interface [CodeExecutionService.java](PY2JAVA-cloud-ide-starter/src/main/java/com/py2java/cloudide/service/CodeExecutionService.java) declares the core execution contract:

- `dockerAvailable()`
- `run(String language, String code)`
- `terminal(String command)`

This interface separates business logic from the concrete Docker orchestration implementation. Higher layers, including the controller, depend on the interface rather than the lower-level implementation.

### 6.2 Translation Abstraction

The interface [CodeTranslationService.java](PY2JAVA-cloud-ide-starter/src/main/java/com/py2java/cloudide/translator/CodeTranslationService.java) declares the translation contract:

- `translate(String code)`

The actual translation logic is implemented by [RuleBasedCodeTranslationService.java](PY2JAVA-cloud-ide-starter/src/main/java/com/py2java/cloudide/translator/RuleBasedCodeTranslationService.java). This division ensures that translation behavior can evolve without changing the controller or application flow.

### 6.3 Benefit of Abstraction

Because the controller interacts with interfaces rather than concrete classes, the system becomes easier to test and maintain. Mock implementations can be used in unit tests, and the same API contract can be preserved even if the underlying algorithm or infrastructure changes.

## 7. Real-World OOP Application in the Backend

### 7.1 Controller Separates HTTP from Logic

The `ApiController` acts as the HTTP facade. It accepts requests, validates them, and delegates to services. It does not contain Docker management, code conversion logic, or output formatting rules. This is a strong object-oriented design choice because it keeps the controller focused and reduces complexity.

### 7.2 Service Layer Contains Business Logic

The concrete execution service, [DockerCodeExecutionService.java](PY2JAVA-cloud-ide-starter/src/main/java/com/py2java/cloudide/service/DockerCodeExecutionService.java), orchestrates runtime execution and enforce safe command restrictions. This layer handles the application workflow without exposing Docker internals to the controller.

### 7.3 Executor Hierarchy Encodes Behavior by Language

The executor classes show the clearest OOP implementation in the system. Shared execution logic is centralized in `CodeExecutor`, while language-specific rules are delegated to `PythonExecutor` and `JavaExecutor`.

### 7.4 Translator Engine Isolated from HTTP Layer

The translation logic is implemented in [RuleBasedCodeTranslationService.java](PY2JAVA-cloud-ide-starter/src/main/java/com/py2java/cloudide/translator/RuleBasedCodeTranslationService.java). This service converts Python syntax into Java structures and returns warnings for unsupported or manual-review cases. This design preserves the original Python translation rules while preventing the controller from becoming overloaded with conversion logic.

## 8. Centralized Error Handling and Domain Models

The backend also uses object-oriented design in its error and response model layers.

- [ApiExceptionHandler.java](PY2JAVA-cloud-ide-starter/src/main/java/com/py2java/cloudide/exception/ApiExceptionHandler.java) centralizes exception handling.
- Domain model classes such as [ExecutionResult.java](PY2JAVA-cloud-ide-starter/src/main/java/com/py2java/cloudide/model/ExecutionResult.java), [HealthStatus.java](PY2JAVA-cloud-ide-starter/src/main/java/com/py2java/cloudide/model/HealthStatus.java), and [TranslationResult.java](PY2JAVA-cloud-ide-starter/src/main/java/com/py2java/cloudide/model/TranslationResult.java) represent return values clearly.

These classes enforce a consistent response structure and keep API behavior predictable. The use of strong domain objects with defined fields is another example of encapsulation and abstraction in practice.

## 9. Summary of OOP Principles in This Project

| OOP Principle | Implementation in Project | Evidence |
|---|---|---|
| Encapsulation | Private fields, validation-based DTOs, constructor injection, protected execution internals | ApiController, DTOs, DockerExecutionProperties |
| Inheritance | Shared execution workflow in abstract base class reused by language-specific subclasses | CodeExecutor, PythonExecutor, JavaExecutor |
| Polymorphism | Overriding `execute()` in subclasses with different runtime executions | PythonExecutor, JavaExecutor |
| Abstraction | Interfaces that hide implementation details from controllers and callers | CodeExecutionService, CodeTranslationService |

## 10. Conclusion

The PY2JAVA Java backend demonstrates a strong and meaningful application of object-oriented programming principles. The project does not simply mention OOP in theory; it uses real object-oriented design to organize the system around reusable behavior, protected internal state, and clear responsibilities.

The most important examples are:

- `CodeExecutor` as the shared abstraction for language execution
- `PythonExecutor` and `JavaExecutor` as specialized implementations
- `CodeExecutionService` and `CodeTranslationService` as interfaces that separate business logic from implementation
- `ApiController` as the HTTP layer that delegates to service abstractions
- DTOs and domain models that encapsulate data and validation

These design choices preserve the original application behavior while creating a maintainable Java Spring Boot backend that remains faithful to the original project requirements. In this sense, the migration is not only a language conversion but also a structured OOP-based refactoring of the backend architecture.

## 11. Final Remarks

The project successfully balances software engineering quality with strict functional preservation. The Java migration keeps the frontend untouched, maintains the existing REST contracts, and applies OOP principles in a practical and professional manner. As a result, the backend is both functionally compatible and structurally well-designed.
