# Development vs UAT

## Development

Development uses a shared **file-based H2 database**. PostgreSQL is not used by the Spring Boot services in the `dev` profile.

- Spring profile: `dev`
- Database: H2 file `../../data/video-platform-dev` (override with `VIDEO_PLATFORM_DEV_DB`)
- Hibernate: `ddl-auto=update`
- Flyway: disabled
- Kafka: localhost:9092
- Redis: localhost:6379
- Elasticsearch: http://localhost:9200
- MinIO: http://localhost:9000
- Gateway: http://localhost:8080

Start the H2 TCP server first (this does not reset or delete the database):

```powershell
.\scripts\start-h2-dev.ps1
```

Then start the supporting infrastructure:

```powershell
docker compose -f docker-compose.dev.yml up -d
```

Run the backend modules locally with `SPRING_PROFILES_ACTIVE=dev`. Start Auth Service before using user-relationship endpoints in `user-creator-service`; all services share the same development H2 database. The gateway is `backend/api-gateway`.

### Development credentials

The dev profile seeds these accounts on first start:

| Account | Username | Password | Role |
|---|---|---|---|
| Admin | `admin` | `Admin@123` | ADMIN |
| Creator | `creator` | `Creator@123` | CREATOR |
| Viewer | `viewer` | `Viewer@123` | USER |

Dev email verification is disabled so local registration/login is immediate.

## UAT

UAT uses a **dedicated PostgreSQL database** and Flyway migrations.

- Spring profile: `uat`
- Database: `video_platform_uat`
- Hibernate: `validate`
- Flyway: enabled
- PostgreSQL credentials are configured by `UAT_POSTGRES_*`

Run the UAT stack with:

```powershell
docker compose -f docker-compose.yml -f docker-compose.uat.yml up -d --build
```

UAT does not use the dev H2 seed data and email verification remains enabled.
