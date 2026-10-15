# Video Platform Backend - DEV / UAT

## Service ports

| Service | DEV/UAT host port |
|---|---:|
| API Gateway | 8080 |
| Auth | 8081 |
| User/Creator | 8082 |
| Video | 8083 |
| Upload | 8084 |
| Search | 8085 |
| Engagement | 8086 |
| Subscription | 8087 |
| Playlist | 8088 |
| Notification | 8089 |
| Analytics | 8090 |
| Moderation | 8091 |
| Transcoding | 8093 |

## DEV

All JPA services use the same file-based H2 database:

`../../data/video-platform-dev`

The DEV profile uses Hibernate `ddl-auto=update` and disables Flyway. H2 creates the application schemas on startup. Search remains Elasticsearch-backed and has no relational database.

Start infrastructure from the repository root:

```powershell
docker compose -f docker-compose.dev.yml up -d
```

Build all backend modules:

```powershell
cd backend
mvn clean install -DskipTests
```

Run all services:

```powershell
cd ..
.\scripts\run-dev.ps1
```

Or run a service directly:

```powershell
cd backend\api-gateway
$env:SPRING_PROFILES_ACTIVE='dev'
mvn spring-boot:run
```

## UAT

UAT uses PostgreSQL and Flyway. Hibernate is `validate`; it does not mutate the UAT schema.

Set:

```text
SPRING_PROFILES_ACTIVE=uat
SPRING_DATASOURCE_URL=jdbc:postgresql://.../video_platform_uat
SPRING_DATASOURCE_USERNAME=platform_uat
SPRING_DATASOURCE_PASSWORD=...
```

Run the UAT compose stack from the repository root:

```powershell
docker compose -f docker-compose.yml -f docker-compose.uat.yml up -d --build
```

## Important architecture rules

- `common` contains shared DTOs, events, security, storage and common JPA entities.
- `Video` stores `categoryId`, `genreId` and `languageId` rather than depending on user-service entity classes.
- Kafka event payloads include Jackson type metadata so the generic `EventEnvelope<T>` can be reconstructed by consumers.
- Transcoding consumes `video.uploaded.v1`, waits/retries for the video-service processing job, runs FFmpeg, and publishes processing-completed/failed events.
- Moderation actions are published through `moderation.action.applied.v1` and consumed by the relevant services.
