# Live TV V10.2 Playback Fix

## Root cause fixed
External IPTV HLS sources that do not return `Access-Control-Allow-Origin` were being sent directly to the browser. The browser correctly rejected those requests with CORS errors before the compatibility proxy could be used.

## V10.2 behavior
- Direct HLS is attempted only when the backend verifies that the provider explicitly permits the configured browser origin via CORS.
- All other HLS sources use the existing same-origin Java compatibility proxy automatically.
- Proxy fallback remains available for every candidate.
- Added VLC and FFmpeg-style upstream request profiles for provider compatibility.
- IPTV compatibility client now uses HTTP/1.1, which is more tolerant of legacy IPTV servers.
- Browser-compatibility results are cached for 10 minutes to avoid repeatedly probing the same provider.
- Existing multi-candidate failover, HLS master/variant/segment rewriting, diagnostics, search, filters, logos, and YouTube fallback remain intact.
- Uploaded VOD player is untouched.

## Configuration
`livetv.playback.browser-origin` defaults to `http://localhost:3000` in the dev profile. Set `LIVETV_BROWSER_ORIGIN` to the real frontend origin in another environment.
