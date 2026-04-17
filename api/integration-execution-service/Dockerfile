# ================================================================
# Integration Execution Service
# Runtime-only image: copies the verified bootJar built by CI
# Build context: project root (.)
# ================================================================

FROM eclipse-temurin:25-jre AS production

WORKDIR /app

# Create non-root user (Debian syntax)
RUN groupadd -r spring && useradd -r -g spring spring

# Copy the verified fat JAR built and tested in CI
COPY api/integration-execution-service/build/libs/integration-execution-service-*.jar app.jar

# Create log directory
RUN mkdir -p /app/logs && chown -R spring:spring /app

USER spring:spring

EXPOSE 8080

# JVM flags:
#   -XX:MaxRAMPercentage=75.0    — use up to 75% of container memory limit
#   -XX:+ExitOnOutOfMemoryError  — crash fast on OOM so orchestrator can restart
#   -XX:+UseZGC                  — low-latency GC (sub-ms pauses, Generational ZGC default in Java 25)
#   -Xss512k                     — reduce thread stack size (saves memory under load)
#   -XX:MetaspaceSize=128m       — initial metaspace size (avoids early resize GC cycles)
#   -XX:MaxMetaspaceSize=256m    — cap off-heap metaspace (unbounded by default, can OOM kill pod)
#   -XX:+HeapDumpOnOutOfMemoryError — write heap dump on OOM for post-mortem analysis
#   -XX:HeapDumpPath             — dump to mounted log volume
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 \
              -XX:+ExitOnOutOfMemoryError \
              -XX:+UseZGC \
              -Xss512k \
              -XX:MetaspaceSize=128m \
              -XX:MaxMetaspaceSize=256m \
              -XX:+HeapDumpOnOutOfMemoryError \
              -XX:HeapDumpPath=/app/logs/heapdump.hprof"

HEALTHCHECK NONE
# Health probing is handled by Kubernetes liveness and readiness probes via
# Spring Boot Actuator: GET /actuator/health/liveness and /actuator/health/readiness

# Use exec form with sh -c to allow $JAVA_OPTS expansion
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
