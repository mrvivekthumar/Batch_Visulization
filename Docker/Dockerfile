# Production-Optimized Multi-stage Dockerfile
# Stage 1: Build stage with Maven cache optimization
FROM openjdk:24-jdk-slim as build-stage

# Install required build tools
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    maven \
    git && \
    rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /app

# Copy Maven configuration first for better caching
COPY pom.xml ./
COPY .mvn/ .mvn/
COPY mvnw ./

# Make mvnw executable
RUN chmod +x ./mvnw

# Download dependencies (cached layer)
RUN ./mvnw dependency:go-offline -B --no-transfer-progress

# Copy source code
COPY src ./src

# Build application with production profile
RUN ./mvnw clean package -Pprod -DskipTests -B --no-transfer-progress && \
    mkdir -p target/extracted && \
    java -Djarmode=layertools -jar target/*.jar extract --destination target/extracted

# Stage 2: Runtime stage with optimized JRE
FROM openjdk:24-jre-slim as runtime-stage

# Create application user and group
RUN groupadd -r appgroup && \
    useradd -r -g appgroup -d /app -s /bin/false appuser

# Install runtime dependencies and security updates
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    curl \
    netcat-openbsd \
    ca-certificates && \
    apt-get upgrade -y && \
    rm -rf /var/lib/apt/lists/* && \
    apt-get clean

# Set working directory
WORKDIR /app

# Copy application layers from build stage (optimized layer caching)
COPY --from=build-stage --chown=appuser:appgroup /app/target/extracted/dependencies/ ./
COPY --from=build-stage --chown=appuser:appgroup /app/target/extracted/spring-boot-loader/ ./
COPY --from=build-stage --chown=appuser:appgroup /app/target/extracted/snapshot-dependencies/ ./
COPY --from=build-stage --chown=appuser:appgroup /app/target/extracted/application/ ./

# Create necessary directories
RUN mkdir -p /app/logs /app/config /app/data && \
    chown -R appuser:appgroup /app

# Copy production configuration
COPY --chown=appuser:appgroup Docker/config/application-prod.yml /app/config/
COPY --chown=appuser:appgroup Docker/scripts/docker-entrypoint.sh /app/

# Make entrypoint script executable
RUN chmod +x /app/docker-entrypoint.sh

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8080

# Add health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Production JVM settings
ENV JAVA_OPTS="-server \
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseG1GC \
    -XX:+UseStringDeduplication \
    -XX:+OptimizeStringConcat \
    -XX:+UseCompressedOops \
    -XX:+UseCompressedClassPointers \
    -XX:+TieredCompilation \
    -XX:TieredStopAtLevel=4 \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=/app/logs/ \
    -XX:+ExitOnOutOfMemoryError \
    -Djava.security.egd=file:/dev/./urandom \
    -Dspring.profiles.active=prod,docker \
    -Dlogging.config=/app/config/logback-prod.xml"

# Set application properties
ENV SPRING_CONFIG_LOCATION="classpath:/application.yml,/app/config/application-prod.yml"
ENV SPRING_PROFILES_ACTIVE="prod,docker"
ENV SERVER_PORT=8080

# Application metadata
LABEL maintainer="vivek@company.com" \
    version="2.0.0" \
    description="Database Batch Performance Analyzer - Production" \
    org.opencontainers.image.source="https://github.com/vivek/batch-performance-analyzer" \
    org.opencontainers.image.documentation="https://github.com/vivek/batch-performance-analyzer/README.md"

# Use custom entrypoint for better initialization
ENTRYPOINT ["/app/docker-entrypoint.sh"]
CMD ["java", "org.springframework.boot.loader.JarLauncher"]