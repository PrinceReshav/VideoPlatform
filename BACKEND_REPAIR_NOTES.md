# Backend Repair Notes

This file records the source/configuration changes applied to the supplied project archive.

## Source fixes

- Added missing playlist collaborator imports to `playlist-service` (`PlaylistService` and `PlaylistController`).
- Added missing `Channel`, `CreatorProfile`, and repository imports to `upload-service`'s `UploadSessionService`.
- Fixed a missing closing class brace in `playlist-service`'s `PlaylistResponse.java`.
- Changed failed-login transaction handling in `auth-service` so `BadCredentialsException` does not roll back the persisted failed-login/lockout counter update.

## Configuration fixes

- Corrected `video-moderation-service` Kafka settings: the `spring.kafka` configuration was incorrectly nested under `server` and is now under `spring`.
- Added explicit `application-dev.yml`, `application-uat.yml`, and `application-prod.yml` files for each runnable backend service module (17 service modules; `common` is a shared library and does not need application profiles).
- Added production profile configurations with `ddl-auto: validate`, required environment-supplied database/Kafka/Redis settings where those integrations are configured, required `JWT_SECRET` in services using shared JWT security, and restricted Actuator health details.
- Removed fallback UAT database credentials so missing UAT credentials fail at startup rather than using a default password.
- Kept Flyway disabled for placeholder service modules that have no migration scripts yet.
- Corrected production service ports to match each service's base configuration.
- Corrected the production transcoding-service datasource environment in `docker-compose.prod.yml` and disabled Flyway for the transcoding worker, which has no migration scripts of its own.
- Added API Gateway routes for saved videos and recommendations, and included achievements, user relationships, and verification in the user/creator route for both the default and production Gateway configurations.
- Added production Gateway configuration using explicit environment-provided service URLs and allowed origins rather than localhost fallbacks.
- Updated Search Service production Elasticsearch configuration to require `ELASTICSEARCH_HOST`.

## Validation performed

- Parsed all 19 backend Maven POM files as XML.
- Parsed 112 backend YAML files and checked for duplicate mapping keys; no YAML syntax or duplicate-key errors were detected.
- Verified the 17 runnable service modules each contain dev, UAT, and prod profile files.
- Ran a source-level Java compiler parse pass across backend Java sources; no Java syntax diagnostics remained after fixing the missing brace.
- Performed a static scan for unresolved project-source imports; the previously identified missing imports were corrected.

## Validation limitation

A complete Maven compile/test run could not be performed in this environment: Maven is not installed, the Maven wrapper needs to download its distribution, and network DNS access to Maven Central is unavailable. The source tree therefore has **not** been proven to compile, pass tests, or start successfully against real PostgreSQL/Kafka/Redis/MinIO/Elasticsearch services. Run `./mvnw -f backend/pom.xml clean verify` in an environment with Maven Central access before treating this archive as production-ready.

## Follow-up dev database fix

- Replaced all `jdbc:h2:tcp://localhost:9123/video-platform-dev` URLs in development profiles with per-service in-memory H2 databases. The TCP URL required a separately running H2 server; the application log showed connection refusal on port 9123.
- Each affected service now has a service-specific database name and keeps its own configured default schema (for example, `moderation_schema` for `moderation-service`).
- Updated legacy `spring.redis` keys to the Spring Boot 3.x `spring.data.redis` prefix in backend YAML configuration.
- Removed embedded UTF-8 BOM characters found in YAML files, including the transcoding service development profile.
- Updated validation after this follow-up: YAML syntax parsing completed with no errors, and no `localhost:9123` H2 TCP URLs remain in backend YAML files.

The development H2 databases are intentionally isolated per service. If you instead require multiple services to share one local database, start and configure an H2 TCP server explicitly or use a local PostgreSQL instance with separate schemas.
