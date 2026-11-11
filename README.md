# PY2JAVA Cloud IDE

A local browser IDE with Python and Java execution in isolated Docker containers and a basic Python-to-Java converter. The backend is Java 21 with Spring Boot; the existing frontend is served unchanged.

## Requirements
- Java 21
- Maven 3.9+
- Docker Engine/Desktop (running)

## Run locally

1. Start Docker Desktop/Engine.
2. From this project directory, optionally pull the execution images:

   ```powershell
   docker pull python:3.12-alpine
   docker pull eclipse-temurin:21-jdk
   ```

3. Start the backend:

   ```powershell
   mvn spring-boot:run
   ```

4. Open http://127.0.0.1:8000. The backend health endpoint is http://127.0.0.1:8000/api/health.

Run the test suite with `mvn test` and create an executable JAR with `mvn package`.

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
- The AI assistant is a placeholder panel.
- Project persistence, accounts, cloud sync, GitHub integration, and billing are not implemented.
