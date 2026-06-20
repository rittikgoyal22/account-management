# syntax=docker/dockerfile:1

# ---- Build stage (gradle-wrapper jar is not committed, so use the Gradle image) ----
FROM gradle:8.10.2-jdk21 AS build
WORKDIR /workspace
# Copy build config first to cache dependency resolution across source-only changes
COPY build.gradle settings.gradle ./
RUN gradle --no-daemon dependencies > /dev/null 2>&1 || true
COPY src ./src
RUN gradle --no-daemon clean bootJar

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd -r spring && useradd -r -g spring -u 1001 spring \
 && mkdir -p /app/uploads && chown -R spring:spring /app
COPY --from=build --chown=spring:spring /workspace/build/libs/*.jar app.jar
USER spring
EXPOSE 8081
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
