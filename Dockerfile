FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
COPY static ./static
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-jammy
RUN apt-get update \
    && apt-get install -y --no-install-recommends docker.io \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /workspace/target/cloud-ide-starter-1.0.0.jar app.jar
EXPOSE 8000
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
