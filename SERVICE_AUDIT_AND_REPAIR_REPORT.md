# Service Audit and Repair Report

## Scope

Reviewed the supplied source archive with primary attention to Auth Service, Video Service, and `user-creator-service`. Live-TV Service and API Gateway were treated as working services and their source files were not changed.

## Source-level defects corrected

1. **Auth Service development seeder**
   - Before: seed logic checked only the expected UUID. An existing account with the same email or username but a different UUID caused a unique-key violation during startup.
   - After: seeding skips an account if its UUID, email, or username already exists. Existing account credentials, roles, and profile fields are not overwritten.

2. **`user-creator-service` cross-module dependency**
   - Before: `UserRepository` and `UserRelationshipService` referenced `com.platform.auth.entity.User`, which is owned by `auth-service` and is not provided by the `user-creator-service` module's declared dependencies. The repository also created an entity-scanning risk at runtime.
   - After: removed that repository and use `JdbcTemplate` for the one required user-existence check against `auth_schema.users`. This avoids importing an Auth Service JPA entity into the user service.

3. **Auth Service repeated index DDL warnings**
   - Removed duplicate JPA `@Index` declarations for the users and refresh-token tables. PostgreSQL index definitions remain in the existing Flyway migration. This avoids Hibernate attempting to recreate those named indexes during development schema updates on an already-initialized H2 database.

4. **Video Service configuration**
   - Merged duplicate top-level `management` mappings in `application.yml` so Actuator and metrics configuration are defined once.

5. **Executable JAR packaging**
   - The project uses a custom Maven parent rather than Spring Boot's parent, so merely declaring `spring-boot-maven-plugin` does not guarantee that `package` creates a runnable Boot archive.
   - Added explicit `repackage` executions for Auth, User Creator, and Video Service.
   - User Creator and Video Service are also consumed as libraries by other modules. Their normal thin JARs remain the main Maven artifacts; runnable archives are attached with the `exec` classifier. Their Dockerfiles now copy those exact executable archives, avoiding wildcard ambiguity and nested executable-JAR dependencies.
   - Auth Service is not consumed as a library in this project, so its standard artifact is repackaged as the executable JAR and its Dockerfile copies that exact artifact.

6. **Repeatable H2 development startup**
   - Added `scripts/start-h2-dev.ps1`. It starts the H2 TCP server on port 9123 using the H2 JAR already downloaded by Maven, detects an existing database before creating a new directory, and refuses to choose between multiple existing database locations. It does not delete or reset database files.

7. **Kubernetes manifest validation**
   - Removed the duplicate `JWT_SECRET` key from `infrastructure/kubernetes/base/configmap-secret.yaml` so the Secret manifest has unambiguous YAML data.

## Validation performed

- Inspected the supplied source, service POMs, profile configuration, SQL migrations, and archived application logs.
- Parsed 144 YAML/YML files (including multi-document Kubernetes manifests) with a duplicate-key-aware YAML check; no duplicate keys or parse errors remain.
- Parsed all 19 Maven POM files as XML.
- Verified the previously problematic Auth seeder condition and removed references to the deleted `UserRepository`.
- Checked executable-jar classifier and Docker copy paths for the three target services.
- Checked the H2 startup script for existing database safeguards and port handling.

## Archive hygiene

Generated Maven `target` directories, frontend `node_modules`/`dist`, and the root-level compiled smoke-test class were excluded from the delivered source archive. Source files, Maven POMs, npm package manifests/lockfiles, Dockerfiles, infrastructure files, and project documentation are retained. This avoids stale bytecode masking source changes; rebuild generated outputs on the development machine.

## Validation limitation

A fresh Maven build/test could not be completed in this environment because the Maven Wrapper distribution was not cached and the environment could not resolve `repo.maven.apache.org`. Therefore, this report does **not** claim that a fresh compile, integration test, or live startup test passed. After extracting this archive on the development machine, run a Maven build and start the services against the intended existing H2 database before relying on runtime status.

## Important database note

Do not delete the H2 database to address startup issues. The new script will stop and ask for a decision if it detects both known candidate locations containing `video-platform-dev` database files.
