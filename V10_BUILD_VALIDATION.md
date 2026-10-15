# V10 Build Validation

## Source baseline
- Built from `video-platform-live-tv-famelack-v8-2-playback-resilient.zip`.
- Existing files were preserved; V10 changes are additive/replacement changes in the Live TV catalogue, playback and fallback layers.

## Passed
- Java Live-TV sources compile with the available Spring/Jackson dependency set.
- Spring Boot Live-TV service starts successfully in the dev profile with sync/health disabled for isolated startup validation.
- Hibernate/JPA entity model initializes successfully.
- Spring Data reports both Live TV repositories.
- Frontend TypeScript compilation passes with `npx tsc --noEmit`.
- Patched executable Live-TV JAR starts successfully after removing stale root application classes from the old JAR packaging.
- Fixture sync test passed with 2 metadata channels and 2 stream candidates for one channel; a metadata-only channel was retained (`saved=2`, `skippedNoStream=0`) and the initial UUID/version optimistic-lock issue was fixed with `Persistable.isNew()`.
- Patched JAR ZIP integrity passes.

## Environment limitation
- Full Vite production bundle could not be generated in this Linux validation container because the supplied `node_modules` contains Windows Rollup optional binaries while the container is Linux. TypeScript itself passes.
- Full Maven online build is not possible in this container because external Maven DNS/network access is unavailable.
- The user's Windows IPTV-org git clone is not accessible to the build container. V10 includes an `iptv/README.md` placeholder and expects the user's complete clone to replace that directory. The application recursively reads all M3Us and local IPTV data once the clone is present.

## Required runtime
Place the user's complete IPTV-org checkout at:
`<project-root>/iptv`

Run `scripts/verify-iptv.cmd` before starting Live TV.
