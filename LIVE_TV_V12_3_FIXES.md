# Live TV V12.3 — Startup Loading, Globe UI and Performance Fixes

## Fixed
- Added `/api/v1/live/readiness` so the React UI waits for the asynchronous IPTV-org startup import instead of caching an initial zero-channel response.
- Countries, categories and channels are now enabled only after the catalogue reports ready. The UI shows skeleton loading instead of misleading `0 channels`.
- Readiness polls once per second only while startup sync is incomplete; it stops immediately when the catalogue is ready.
- Kept existing server-side country/category intersection and search behaviour unchanged.
- Removed the instructional text overlay from the globe (`3D EARTH`, `Global`, drag instructions).
- Removed the continuous per-pixel animation loop from the globe. It now redraws only for resize, data/filter changes, hover, and user drag, greatly reducing CPU usage.
- Replaced expensive per-pixel Earth shading with a radial sphere gradient while retaining the country-vector projection and heatmap.
- Fixed malformed/partially clipped country paths that could produce the large purple rectangular artefacts.
- Added a green radar-style ring cursor when the pointer is over a clickable country; normal globe rotation keeps the grab cursor.
- Kept free pitch/yaw rotation and Shift/right-drag Z-roll.

## Verification
- Frontend `npx tsc --noEmit`: PASS.
- Backend changed classes compiled with Java 21 against the packaged runtime classpath: PASS.
- The packaged Live TV boot JAR was updated with the new controller/service classes.
- Existing backend APIs, IPTV sync, stream candidates, player, category drawer and country selector were not otherwise redesigned.

## Note
The separate `/api/v1/search/videos` HTTP 500 seen in browser console belongs to the platform Search Service, not the Live-TV API. This V12.3 change does not alter that unrelated service.
