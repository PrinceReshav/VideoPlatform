# Playlist Service Dockerfile
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# Copy Maven project files
COPY backend/pom.xml .
COPY backend/common/pom.xml ./common/
COPY backend/analytics-service/pom.xml ./analytics-service/
COPY backend/auth-service/pom.xml ./auth-service/
COPY backend/engagement-service/pom.xml ./engagement-service/
COPY backend/moderation-service/pom.xml ./moderation-service/
COPY backend/notification-service/pom.xml ./notification-service/
COPY backend/playlist-service/pom.xml ./playlist-service/
COPY backend/search-service/pom.xml ./search-service/
COPY backend/subscription-service/pom.xml ./subscription-service/
COPY backend/transcoding-service/pom.xml ./transcoding-service/
COPY backend/upload-service/pom.xml ./upload-service/
COPY backend/user-creator-service/pom.xml ./user-creator-service/
COPY backend/video-service/pom.xml ./video-service/

# Download dependencies
RUN mvn -B dependency:go-offline -pl common,playlist-service -am

# Copy source code
COPY backend/common/src ./common/src
COPY backend/playlist-service/src ./playlist-service/src

# Build
RUN mvn clean package -DskipTests -pl playlist-service -am

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Install curl for health checks
RUN apk add --no-cache curl

# Create non-root user
RUN addgroup -g 1000 -S appgroup && \
    adduser -u 1000 -S appuser -G appgroup

# Copy built jar
COPY --from=builder /app/playlist-service/target/*.jar app.jar

# Change ownership
RUN chown -R appuser:appgroup /app

USER appuser

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8088/actuator/health || exit 1

EXPOSE 8088

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]