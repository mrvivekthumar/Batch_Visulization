# Production-Optimized Multi-stage Dockerfile
# Stage 1: Build stage with Maven
FROM eclipse-temurin:21-jdk-alpine as build-stage

# Install required build tools
RUN apk add --no-cache \
    maven \
    curl

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

# Build application with proper Spring Boot packaging
RUN ./mvnw clean package -DskipTests -B --no-transfer-progress

# Stage 2: Runtime stage with optimized JRE
FROM eclipse-temurin:21-jre-alpine as runtime-stage

# Create application user and group (Alpine Linux syntax)
RUN addgroup -S appgroup && \
    adduser -S -G appgroup -h /app -s /bin/false appuser

# Install runtime dependencies and security updates
RUN apk add --no-cache \
    curl \
    netcat-openbsd \
    ca-certificates

# Set working directory
WORKDIR /app

# Copy the built JAR file from build stage
COPY --from=build-stage --chown=appuser:appgroup /app/target/*.jar app.jar

# Create necessary directories
RUN mkdir -p /app/logs /app/config /app/data && \
    chown -R appuser:appgroup /app

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
    -Djava.security.egd=file:/dev/./urandom"

# Set application properties
ENV SPRING_PROFILES_ACTIVE="docker"
ENV SERVER_PORT=8080

# Application metadata
LABEL maintainer="vivek@company.com" \
    version="2.0.0" \
    description="Database Batch Performance Analyzer - Production" \
    org.opencontainers.image.source="https://github.com/vivek/batch-performance-analyzer"

# Simple entrypoint that directly runs the JAR
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]