# Live TV V7 — Stability & Player Architecture

This version keeps the existing Live TV catalogue, IPTV-org local checkout integration, server-side pagination/filtering, favorites, search, health checks, gateway route, and HLS player. V7 focuses on the failures observed during real browser playback.

## V7 changes

### 1. Player is independent from the globe
- The player is rendered through a React portal directly under `document.body`.
- Its position is no longer calculated from, nested in, or aligned to the globe panel.
- Selecting a channel opens a stable centered player regardless of the globe position.
- Player modes:
  - Normal: centered player with backdrop.
  - Mini: floating bottom-right player; the Live TV page remains usable behind it.
  - Full player: viewport-sized player independent of page layout.
- The player header contains dedicated minimize, full-player, and close controls.
- The existing live controls remain: play/pause, volume, quality/auto, fullscreen/PiP where supported, LIVE state, and retry. Live mode still has no VOD seek/skip/speed controls.

### 2. HLS proxy is header-aware
IPTV-org streams can publish `referrer` and `user_agent` requirements. Local M3U files can also contain:

- `#EXTVLCOPT:http-referrer=...`
- `#EXTVLCOPT:http-user-agent=...`

V7 parses those directives and the server-side proxy forwards them when fetching manifests, playlists, segments, keys, and maps. This addresses a major class of upstream HTTP 403 failures.

### 3. Manifest failover
When the first local IPTV-org feed cannot be fetched, the proxy can try another feed for the same channel before reporting the source unavailable. Once a manifest is selected, child HLS resources remain pinned to that source so media requests do not switch origins mid-playback.

### 4. HLS URL preference
Local stream selection now prefers HLS playlist URLs over raw/non-HLS URLs when candidates are otherwise comparable. This prevents a raw segment or non-playlist endpoint from being incorrectly treated as the channel manifest.

### 5. Stable upstream errors
Live proxy failures are returned as HTTP `502 Bad Gateway` with a structured error instead of an unhandled HTTP 500. A 502 means the application is healthy but the selected external stream source did not accept or serve the request.

### 6. Bounded browser recovery
The React HLS player no longer retries fatal network errors indefinitely. Network and media recovery attempts are bounded and use short backoff; after the limit, the player exposes the existing Retry action instead of hammering an unavailable provider.

### 7. Existing filter contract preserved
- Country + category remains an exact intersection.
- Facet counts remain calculated against the active opposite filter/search.
- Server-side pagination remains 50 channels per page.
- Cache keys use positional parameter references (`#p0`, etc.) so builds do not depend on Java parameter-name retention.

## Files changed in V7

### Backend
- `backend/live-tv-service/src/main/java/com/platform/livetv/service/IptvOrgSyncService.java`
- `backend/live-tv-service/src/main/java/com/platform/livetv/controller/LiveTvController.java`

The V5/V4 Live TV repository/entity/config changes remain preserved in the project, including:
- `backend/live-tv-service/src/main/java/com/platform/livetv/repository/LiveChannelRepository.java`
- `backend/live-tv-service/src/main/java/com/platform/livetv/entity/LiveChannel.java`
- `backend/live-tv-service/src/main/resources/application-dev.yml`
- `backend/api-gateway/src/main/resources/application.yml`

### Frontend
- `frontend/src/pages/LiveTvPage.tsx`
- `frontend/src/pages/LiveTvPage.css`
- `frontend/src/components/video/VideoPlayer.tsx`

## Important runtime requirement
The Live TV service must actually be running on port `8092` when the gateway route uses its default `http://localhost:8092` target. If the service is stopped, the gateway correctly reports an upstream connection failure; V7 does not hide a stopped service as a fake successful response.

The local IPTV-org checkout remains an external/local source directory and is not replaced by generated demo data.

## V7.1 Playback fixes (2026-10-05)

### 1. Live-TV audio/autoplay
- Live-TV no longer forces `initialMuted` from `LiveTvPage`.
- `VideoPlayer` now attempts audible autoplay first.
- If the browser rejects audible autoplay, playback falls back to muted mode without treating the channel as failed.
- A visible `Click to unmute` action is shown after that fallback; the explicit user gesture enables sound.
- Explicit user mute/unmute choices are remembered for subsequent Live-TV channels. An autoplay-policy fallback is **not** persisted as a user preference.

### 2. HLS upstream/header continuity and channel failure handling
- The Live-TV proxy now remembers the exact upstream stream candidate that successfully served the manifest for each playback session.
- Child HLS playlists, segments and encryption keys that use different URLs inherit the selected stream's `Referer` / `User-Agent` instead of losing those headers because the child URL is absent from IPTV-org's catalogue.
- Initial manifest requests continue to fail over across the channel's available IPTV-org stream candidates.
- Failed upstream attempts are logged with channel ID, IPTV TVG ID, candidate URL, referrer and the upstream reason.
- Final 502 responses now identify that the upstream source was unavailable after the attempted candidates, rather than presenting the condition as an unspecified application failure.
