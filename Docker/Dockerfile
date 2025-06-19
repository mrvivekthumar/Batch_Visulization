# Multi-Environment Optimized Dockerfile
# Uses your existing comprehensive scripts

# ==============================================
# Stage 1: Build stage with Maven
# ==============================================
FROM eclipse-temurin:21-jdk-alpine as build-stage

# Set build arguments for flexibility
ARG BUILD_PROFILE=docker
ARG SKIP_TESTS=true

# Install required build tools and security updates
RUN apk add --no-cache maven curl git && \
    apk upgrade --no-cache

# Set working directory
WORKDIR /app

# Copy Maven wrapper and configuration first (for better caching)
COPY pom.xml ./
COPY .mvn/ .mvn/
COPY mvnw ./

# Make mvnw executable
RUN chmod +x ./mvnw

# Download dependencies (cached layer if pom.xml doesn't change)
RUN ./mvnw dependency:go-offline -B --no-transfer-progress

# Copy source code
COPY src ./src

# Build application with environment-specific profile
RUN if [ "$SKIP_TESTS" = "true" ]; then \
    ./mvnw clean package -DskipTests -B --no-transfer-progress; \
    else \
    ./mvnw clean package -B --no-transfer-progress; \
    fi && \
    # Verify JAR was created
    ls -la target/*.jar

# ==============================================
# Stage 2: Runtime stage with optimized JRE
# ==============================================
FROM eclipse-temurin:21-jre-alpine as runtime-stage

# Create application user and group for security
RUN addgroup -S appgroup && \
    adduser -S -G appgroup -h /app -s /bin/false appuser

# Install runtime dependencies and security updates
RUN apk add --no-cache \
    curl \
    netcat-openbsd \
    ca-certificates \
    tzdata \
    bash && \
    apk upgrade --no-cache

# Set timezone (can be overridden by environment variable)
ENV TZ=UTC
RUN cp /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Set working directory
WORKDIR /app

# Copy the built JAR file from build stage
COPY --from=build-stage --chown=appuser:appgroup /app/target/*.jar app.jar

# Copy your existing comprehensive scripts
COPY --chown=appuser:appgroup Docker/scripts/docker-entrypoint.sh /app/docker-entrypoint.sh
COPY --chown=appuser:appgroup Docker/scripts/deploy-prod.sh /app/deploy-prod.sh

# Make scripts executable
RUN chmod +x /app/docker-entrypoint.sh /app/deploy-prod.sh

# Create necessary directories with proper permissions
RUN mkdir -p /app/logs /app/config /app/data /app/tmp && \
    chown -R appuser:appgroup /app && \
    chmod 755 /app/logs /app/config /app/data /app/tmp

# Switch to non-root user for security
USER appuser

# Expose port
EXPOSE 8080

# Use simple health check that works with your entrypoint script
HEALTHCHECK --interval=30s --timeout=15s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Environment-aware JVM settings
ENV JAVA_OPTS_BASE="-server \
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseG1GC \
    -XX:+UseStringDeduplication \
    -XX:+OptimizeStringConcat \
    -XX:+UseCompressedOops \
    -XX:+UseCompressedClassPointers \
    -XX:+TieredCompilation \
    -XX:TieredStopAtLevel=4 \
    -Djava.security.egd=file:/dev/./urandom \
    -Dfile.encoding=UTF-8 \
    -Duser.timezone=UTC"

# Additional JVM settings for different environments
ENV JAVA_OPTS_DEV="-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/app/logs/ -XX:+PrintGCDetails"
ENV JAVA_OPTS_PROD="-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/app/logs/ -XX:+ExitOnOutOfMemoryError"

# Default application properties (can be overridden)
ENV SPRING_PROFILES_ACTIVE="docker"
ENV SERVER_PORT=8080

# Application metadata
LABEL maintainer="vivek@company.com" \
    version="2.0.0" \
    description="Database Batch Performance Analyzer - Multi-Environment" \
    org.opencontainers.image.source="https://github.com/vivek/batch-performance-analyzer" \
    org.opencontainers.image.version="2.0.0" \
    org.opencontainers.image.title="Batch Performance Analyzer"

# Use your comprehensive entrypoint script
ENTRYPOINT ["/app/docker-entrypoint.sh"]
CMD ["java", "-jar", "app.jar"]