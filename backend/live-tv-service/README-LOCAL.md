# Live TV Service — Local IntelliJ Run

## Why the previous startup failed

The normal PostgreSQL configuration expects `localhost:5432`. If PostgreSQL is not running, Flyway cannot obtain a connection and Spring Boot stops during startup.

The `dev` profile is therefore intentionally self-contained for local Live TV development.

## Local `dev` profile

It uses:
- H2 in-memory database
- Hibernate `create-drop`
- Caffeine cache
- IPTV synchronization disabled at startup
- Health checking disabled at startup
- Kafka listener auto-start disabled
- PostgreSQL is **not required**

The `docker`, UAT and production deployments continue to use PostgreSQL + Flyway.

## Start from IntelliJ

Run:

`com.platform.livetv.LiveTvServiceApplication`

The default profile is `dev`, so no additional VM argument is required.

Service:

`http://localhost:8092`

## Smoke tests

Health:

`GET http://localhost:8092/actuator/health`

Channels:

`GET http://localhost:8092/api/v1/live/channels`

Countries:

`GET http://localhost:8092/api/v1/live/countries`

Categories:

`GET http://localhost:8092/api/v1/live/categories`

Featured:

`GET http://localhost:8092/api/v1/live/featured`

## Full environment

For PostgreSQL/Kafka/Redis/MinIO integration, use the repository Docker Compose environment rather than the lightweight IntelliJ `dev` profile.
