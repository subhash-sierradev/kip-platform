# ================================================================
# Translation Service
# Runtime-only image: copies the verified bootJar built by CI/Gradle.
# Build context: api/ directory (the project root passed by docker-compose)
#
# Usage (from api/ directory):
#   docker build -f translation-service/Dockerfile -t translation-service .
# ================================================================

FROM eclipse-temurin:25-jre AS production

WORKDIR /app

# Create a non-root runtime user (Debian syntax)
RUN groupadd -r spring && useradd -r -g spring spring

# Copy the fat JAR produced by `./gradlew :translation-service:bootJar`
# Build context is the api/ directory (set in docker-compose.dev.yml), so the
# path below is relative to api/ — NOT to the repository root.
COPY translation-service/build/libs/translation-service-*.jar app.jar

# Create log directory and set ownership
RUN mkdir -p /app/logs && chown -R spring:spring /app

USER spring:spring

EXPOSE 8083

# JVM tuning flags:
#   -XX:MaxRAMPercentage=75.0   — use up to 75 % of the container memory limit
#   -XX:+ExitOnOutOfMemoryError — crash-fast on OOM so the orchestrator can restart
#   -XX:+UseZGC                 — low-latency GC (sub-ms pauses in Java 25)
#   -Xss512k                    — reduce per-thread stack (saves memory under load)
#   -XX:MetaspaceSize=128m      — hint to avoid early metaspace resizes
#   -XX:MaxMetaspaceSize=256m   — cap off-heap metaspace
#   -Dfile.encoding=UTF-8       — guarantee multi-byte translation output is UTF-8
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 \
              -XX:+ExitOnOutOfMemoryError \
              -XX:+UseZGC \
              -Xss512k \
              -XX:MetaspaceSize=128m \
              -XX:MaxMetaspaceSize=256m \
              -Dfile.encoding=UTF-8 \
              -Dstdout.encoding=UTF-8"

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8083/actuator/health || exit 1

# Use exec-form with sh -c to allow $JAVA_OPTS shell expansion
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]

