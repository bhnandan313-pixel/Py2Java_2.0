# PY2JAVA Cloud IDE

A local browser IDE with Python and Java execution in isolated Docker containers and a basic Python-to-Java converter. The backend is Java 21 with Spring Boot; the existing frontend is served unchanged.

## Requirements

- Java 21 JDK
- Maven 3.9+
- Docker Engine/Desktop (running)

## First-time setup on Windows

Install the JDK and Docker Desktop from PowerShell:

```powershell
winget install --id EclipseAdoptium.Temurin.21.JDK -e
winget install --id Docker.DockerDesktop -e
```

Install Maven separately: download the **Binary zip archive** from the [Apache Maven download page](https://maven.apache.org/download.cgi), extract it to a permanent folder (for example, `%USERPROFILE%\tools\apache-maven-3.x.x`), and add that folder's `bin` subfolder to your user `Path` in Windows Environment Variables. Maven is not available in the default WinGet catalog as an Apache Maven package.

If Maven reports that `JAVA_HOME` is missing, set the user `JAVA_HOME` environment variable to the JDK installation folder (for example, `C:\Program Files\Eclipse Adoptium\jdk-21...`, not its `bin` folder), and add `%JAVA_HOME%\bin` to `Path`.

After installing or changing environment variables, close and reopen VS Code. Verify the tools in a new terminal:

```powershell
java -version
javac -version
mvn -v
docker --version
```

## Run locally

1. Start Docker Desktop/Engine.
2. In VS Code, open this project and open a terminal in the project root (the folder containing `pom.xml`).
3. On the first run, optionally download the execution images:

   ```powershell
   docker pull python:3.12-alpine
   docker pull eclipse-temurin:21-jdk
   ```

4. Start the backend:

   ```powershell
   mvn spring-boot:run
   ```

5. Open http://127.0.0.1:8000. The backend health endpoint is http://127.0.0.1:8000/api/health.

## Run it next time

$env:JAVA_HOME = [Environment]::GetEnvironmentVariable('JAVA_HOME', 'User')
$env:MAVEN_HOME = [Environment]::GetEnvironmentVariable('MAVEN_HOME', 'User')
$env:Path = "$env:MAVEN_HOME\bin;$env:JAVA_HOME\bin;$env:Path"
mvn -v


docker stop py2java-cloud-ide-workspace
mvn spring-boot:run


You do not need to reinstall Java, Maven, or Docker. Start Docker Desktop, open a terminal in the project root, and run:

```powershell
mvn spring-boot:run
```

Stop the backend with `Ctrl+C`. Run the test suite with `mvn test`, or create an executable JAR with `mvn package`.

## AI assistant

The AI assistant can explain code, compare Python and Java syntax, answer code questions, and translate complex Python code into a Java 21 starting point. AI translation returns Java source code only; it does not include an explanation or conclusion in the generated code. Use **AI settings** in the UI to choose automatic cloud-with-Ollama-fallback mode or **Ollama only**. Ollama-only mode never calls a cloud provider or sends a cloud API key. In automatic mode, enter an OpenAI-compatible API base URL, model name, and API key. Example base URLs:

- OpenAI: `https://api.openai.com/v1`
- Gemini OpenAI-compatible endpoint: `https://generativelanguage.googleapis.com/v1beta/openai`
- Groq: `https://api.groq.com/openai/v1`

The API key is held in browser memory for the current page session only. It is sent to the configured provider with the request and is not persisted by the app. Your source code and question are also sent to that provider. Use only a provider you trust.

If the cloud provider is not configured or fails, the app tries Ollama at `http://localhost:11434` using `qwen2.5-coder:3b` by default. Install Ollama and pull the model with:

```powershell
ollama pull qwen2.5-coder:3b
```
ollama run qwen2.5-coder:3b

AI-generated Java can require changes, especially when Python libraries have no direct Java equivalent. Review the output and run it to verify behavior.

## Docker image

Build the backend image with:

```powershell
docker build -t py2java-cloud-ide .
```

The backend invokes the Docker CLI to create execution containers. To run the backend image with access to a Docker Engine socket, mount that socket into the container. For example, on a Linux Docker host:

```sh
docker run --rm -p 8000:8000 -v /var/run/docker.sock:/var/run/docker.sock py2java-cloud-ide
```

For a Linux Docker host, the backend container also needs the same host `/tmp` path for execution workspaces:

```sh
docker run --rm -p 8000:8000 \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v /tmp:/tmp \
  py2java-cloud-ide
```

On Docker Desktop for Windows, running Maven/Java directly on the host is the recommended local workflow so Docker receives host-visible temporary source paths. Do not expose the Docker socket or this API to untrusted users.

## Execution and security boundary

This is a local development MVP, not a production multi-user cloud IDE. Each submitted Python or Java program runs in a Docker container with networking disabled, 128 MB memory, 0.5 CPU, a 64-process limit, a read-only root filesystem, a restricted `/tmp`, dropped Linux capabilities, `no-new-privileges`, a non-root container user, and an eight-second timeout. The backend never executes submitted code on the host. Docker must be available to the backend process.

The terminal remains restricted to `python --version`, `java -version`, `javac -version`, `pwd`, and `ls`; it is not a general-purpose shell. Package installation and arbitrary internet access are not enabled.

## Current limitations

- Translation supports only a small subset of Python syntax and is not a complete compiler.
- AI output depends on the selected model and may need review, especially for unsupported Python libraries.
- Project persistence, accounts, cloud sync, GitHub integration, and billing are not implemented.
