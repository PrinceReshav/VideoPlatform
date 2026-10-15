# Live TV V10.1 Fix Log

## Critical fix
IPTV-org logo URLs are not bounded to 500 characters. The V10 database mapping previously used VARCHAR(500) for `logo` and `tvg_logo`, causing the entire startup catalogue synchronization to roll back when a single IPTV-org logo URL exceeded 500 characters.

V10.1 changes these external URL fields to `TEXT`:
- `live_channels.logo`
- `live_channels.tvg_logo`
- `live_channels.epg_url`

A PostgreSQL Flyway migration is included:
- `backend/live-tv-service/src/main/resources/db/migration/V4__expand_external_url_columns.sql`

Clean installations are also corrected in V1.

## Validation performed
- Live-TV Java sources compiled successfully against the packaged dependency set.
- Patched Live-TV executable JAR contains the corrected entity classes and V4 migration.
- A clean H2 startup smoke test with a synthetic 700+ character IPTV-org logo URL completed successfully.
- The smoke test imported the local IPTV stream and reported `saved=1` with no length exception.
- Frontend TypeScript compilation (`tsc --noEmit`) passed.
- Vite production build could not be executed in this Linux validation environment because the supplied Windows-oriented node_modules is missing Rollup's Linux optional native package. This is an environment/package-install limitation, not a TypeScript error.

## Runtime IPTV checkout
The project expects the user's complete IPTV-org Git checkout at:

`./iptv`

and streams under:

`./iptv/streams`

The build includes `scripts/verify-iptv.ps1` and `scripts/verify-iptv.cmd` to validate the checkout before startup. The user's local IPTV-org clone is not fabricated or substituted into this archive.
