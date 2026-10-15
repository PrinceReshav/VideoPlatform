# Live TV V11 Build

## Sync and catalogue reliability
- Replaced the V10.3 two-phase detached `saveAll()` flow with transaction-scoped, 100-channel batch upserts.
- Existing channels are loaded by stable IPTV-org `tvgId` and updated as managed JPA entities; new channels are persisted before candidate rows are attached.
- Candidate replacement flushes deletes before inserts, preventing `(channel_id,url)` unique-key collisions on repeat syncs.
- Optimistic-lock conflicts retry at batch level; unexpected batch failures fall back to per-channel persistence so one malformed channel cannot abort the catalogue.
- A transiently empty source set does not erase the last known candidate set.
- Search index and caches rebuild after persistence batches.
- Health checks remain blocked while sync is active.

## Playback resilience
- Keeps every discovered IPTV candidate instead of reducing a channel to one URL.
- Direct-browser HLS is attempted only when the provider explicitly permits CORS; compatibility proxy remains available for every IPTV candidate.
- Proxy keeps candidate-specific Referer/User-Agent and rewrites HLS master, variant, segment, key and map URLs through the same compatibility endpoint.
- Multiple browser/client request profiles remain available for upstream compatibility.
- Candidate failover remains active for initial manifests; child HLS requests stay pinned to the selected feed.
- 404/410-only exhaustion is treated as genuine free-stream unavailability; temporary 403/5xx/timeouts remain an error/compatibility condition rather than being falsely labeled unavailable.
- YouTube Live fallback remains available after IPTV exhaustion.

## V11 globe / UI
- Replaced the previous 2D globe artwork with a canvas-rendered 3D spherical Earth using an equirectangular country-boundary texture.
- Globe continuously spins, can be dragged, focuses on the selected country, and displays a selected-country beacon.
- Country selection continues to drive the exact backend country filter, so the left-side channel guide immediately shows channels for the selected country.
- Desktop layout is now: channel guide (left) → 3D Earth (center) → category browser (right).
- Existing player modes, Live-only controls, logos, search, intersection filtering, YouTube fallback, and VOD player are preserved.

## Validation
- Java Live TV sources compiled successfully against the packaged Spring Boot dependency set.
- Frontend TypeScript (`tsc --noEmit`) passed.
- V11 Live TV service startup passed.
- Local two-channel IPTV fixture sync passed.
- Repeated sync smoke test passed three consecutive syncs with `failedBatches=0` and no optimistic-lock or candidate unique-key failures.
- The container's Linux Rollup optional native package is unavailable, so the final Vite bundle step could not run in this Linux build environment; this is the same optional-dependency issue seen with the prior archive and is not a TypeScript failure.
