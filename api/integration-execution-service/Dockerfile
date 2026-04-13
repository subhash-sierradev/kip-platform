# ================================================================
# Integration Execution Service
# Runtime-only image: copies the verified bootJar built by CI
# Build context: project root (.)
# ================================================================

FROM eclipse-temurin:25-jre AS production

WORKDIR /app

# Install curl for healthcheck (wget not available in temurin-jre)
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

# Create non-root user (Debian syntax)
RUN groupadd -r spring && useradd -r -g spring spring

# Copy the verified fat JAR built and tested in CI
COPY api/integration-execution-service/build/libs/integration-execution-service-*.jar app.jar

# Create log directory
RUN mkdir -p /app/logs && chown -R spring:spring /app

USER spring:spring

EXPOSE 8081

# JVM flags:
#   -XX:MaxRAMPercentage=75.0  — use up to 75% of container memory limit
#   -Djava.security.egd        — faster startup with non-blocking random
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
  CMD curl -sf http://localhost:8081/actuator/health || exit 1

# Use exec form with sh -c to allow $JAVA_OPTS expansion
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
