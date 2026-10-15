# Live TV V10

V10 rebuilds Live TV around the complete IPTV-org metadata + stream catalogue.

## Catalogue
- Reads `iptv/data/channels.json`, `feeds.json`, `logos.json`, categories/countries/languages when the local git clone exists.
- Recursively imports every `.m3u` under `iptv/streams` (all 325 playlists in the user's clone are supported).
- Does not treat `streams/` as an authority for whether a metadata channel exists. Channels remain visible even when no currently indexed stream exists.
- Keeps every distinct stream URL for a channel; feed IDs, quality, labels, Referer and User-Agent are retained.
- Persists candidate rows in `live_stream_candidates` and exposes them through `/api/v1/live/channels/{id}/stream-candidates`.
- Existing soft-deleted channels are restored when they reappear; V10 does not soft-delete channels just because a sync temporarily misses a stream.

## Health
- Health checks test candidates, not only `LiveChannel.url`.
- Statuses: `playable`, `degraded`, `no_source`, `checking`, `unknown`.
- A failed primary URL no longer makes a channel globally offline when another candidate is available.
- Health checks remain diagnostics and never remove channels from the catalogue.

## Playback
- Dedicated Live TV player: direct HLS first, then same-origin compatibility proxy for each IPTV candidate.
- Candidate failover occurs after repeated fatal HLS failures.
- Master/variant/segment/key URLs are rewritten by the proxy and remain pinned to the selected feed.
- Existing VOD `VideoPlayer` is not changed.
- Live TV has no seek/timeline/speed controls.

## YouTube fallback
- Optional official YouTube Data API lookup is exposed at `/api/v1/live/channels/{id}/youtube-fallback`.
- Search requires a configured `YOUTUBE_DATA_API_KEY`.
- Results are constrained to live, embeddable, syndicated broadcasts and validated through `videos.list`.
- Results are cached to avoid a YouTube API request on every playback attempt.
- Playback uses YouTube's official iframe player; no YouTube media URL extraction is performed.

## Required local IPTV checkout
The runtime expects the user's IPTV-org git clone at one of:
- `./iptv`
- `../iptv`
- `../../iptv`
- `livetv.iptv-org.local-repo-path` / `local-streams-path`

The build environment used to generate this archive does not have access to the user's Windows filesystem, so the user's existing `iptv` clone must be copied beside the project exactly as in the user's working directory. The code does not replace it with a partial catalogue.


# V10.1 hotfix — IPTV catalogue persistence

- Fixed IPTV-org logo/tvg-logo/EPG URL database length limits.
- External URLs are stored as TEXT instead of being truncated.
- Added PostgreSQL Flyway migration `V4__expand_external_url_columns.sql`.
- Updated the initial schema so clean installations receive the corrected type immediately.
- Existing stream URL/candidate limits remain at 4096 characters.
- The local IPTV-org checkout remains the runtime source at `./iptv`; copy the complete Git clone into the project directory before starting Live TV.
