# Live TV V8.2 — Playback Resilience & Diagnostics

## Goals
- Preserve the V8.1 logo/search/filter work.
- Treat IPTV playback as a best-effort live relay, not a certificate/health-check gate.
- Keep every IPTV-org M3U candidate for a channel and try all candidates.
- Retry each candidate with multiple browser-compatible request profiles.
- Preserve Referer/User-Agent and derive Origin when a Referer is supplied.
- Keep HLS child playlists, segments, keys and maps inside the same-origin proxy.
- Capture every successful proxy manifest as `LIVE_TV_PLAYBACK_STARTED` in service logs.
- Capture every upstream response/failure with candidate URL, request profile, status and response time.
- Provide `/api/v1/live/channels/{id}/playback-diagnostic` for complete failure classification.

## Failure classification
`UNAVAILABLE` is deliberately conservative. It is returned only when every attempted upstream response is a definitive 404/410 (or there are no stream candidates). Temporary 5xx, timeout, connection, TLS and similar conditions remain `ERROR` so the user is not incorrectly told that a broadcaster is permanently unavailable.

User-facing messages:
- `UNAVAILABLE`: **Channel currently unavailable for free Live Streaming**
- `ERROR`: **Live stream could not be started. Please try again.**

## Important limitation
No software can guarantee playback when an upstream broadcaster blocks the server's network address, is offline, geo-restricts traffic, or requires a proprietary session/token. V8.2 therefore captures the exact upstream response instead of falsely declaring such a channel permanently unavailable.
