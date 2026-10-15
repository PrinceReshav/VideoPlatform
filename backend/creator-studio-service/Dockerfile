FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

COPY backend/pom.xml /app/backend/pom.xml
COPY backend/common/pom.xml /app/backend/common/pom.xml
COPY backend/api-gateway/pom.xml /app/backend/api-gateway/pom.xml
COPY backend/auth-service/pom.xml /app/backend/auth-service/pom.xml
COPY backend/user-creator-service/pom.xml /app/backend/user-creator-service/pom.xml
COPY backend/video-service/pom.xml /app/backend/video-service/pom.xml
COPY backend/upload-service/pom.xml /app/backend/upload-service/pom.xml
COPY backend/transcoding-service/pom.xml /app/backend/transcoding-service/pom.xml
COPY backend/search-service/pom.xml /app/backend/search-service/pom.xml
COPY backend/engagement-service/pom.xml /app/backend/engagement-service/pom.xml
COPY backend/subscription-service/pom.xml /app/backend/subscription-service/pom.xml
COPY backend/playlist-service/pom.xml /app/backend/playlist-service/pom.xml
COPY backend/notification-service/pom.xml /app/backend/notification-service/pom.xml
COPY backend/analytics-service/pom.xml /app/backend/analytics-service/pom.xml
COPY backend/moderation-service/pom.xml /app/backend/moderation-service/pom.xml
COPY backend/live-tv-service/pom.xml /app/backend/live-tv-service/pom.xml
COPY backend/video-moderation-service/pom.xml /app/backend/video-moderation-service/pom.xml
COPY backend/monetization-service/pom.xml /app/backend/monetization-service/pom.xml
COPY backend/creator-studio-service/pom.xml /app/backend/creator-studio-service/pom.xml
COPY backend/common/src /app/backend/common/src
COPY backend/creator-studio-service/src /app/backend/creator-studio-service/src

RUN apk add --no-cache maven && \
    cd /app/backend && mvn clean package -DskipTests -pl creator-studio-service -am

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

RUN apk add --no-cache curl && \
    addgroup -g 1000 -S appgroup && \
    adduser -u 1000 -S appuser -G appgroup

COPY --from=builder /app/backend/creator-studio-service/target/creator-studio-service-*.jar app.jar

RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 8094

ENV JAVA_OPTS="-Xms512m -Xmx1g -XX:+UseG1GC -XX:+UseStringDeduplication"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]