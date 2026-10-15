# Live TV V12.2 — Globe Interaction & Country Heatmap Fix

Baseline: `video-platform-live-tv-famelack-v12-complete.zip`

## Changes

1. **Larger Earth**
   - Increased the desktop Earth footprint so it fills more of the center workspace without changing the channel/category layout.

2. **True free 3D rotation**
   - Replaced the longitude-only horizontal rotation model with quaternion-based 3D rotation.
   - Normal left-drag rotates pitch/yaw, so the user can reach the poles including Antarctica.
   - Shift-drag or right-drag rotates around the Z axis (roll).
   - No automatic rotation.

3. **Actual country heatmap**
   - Added local Natural Earth-derived low-resolution country polygons to the frontend bundle.
   - Countries are rendered as individual spherical polygons rather than as a single Earth texture.
   - Fill intensity is based on the channel count returned by the existing Live TV countries API.
   - Hover shows country name/channel count.
   - Clicking a visible country polygon selects that country and invokes the existing filter callback.
   - Tiny/missing polygon cases fall back to a clickable country centroid hotspot using the existing country-centroid data.
   - Antarctica is included in the local country geometry.

## Backend

No Live-TV backend source was changed by this patch. Existing V11/V12 backend, playback, filtering, search, candidate/failover, and player behavior are preserved.

## Validation

- TypeScript compilation (`tsc --noEmit`) passed.
- GeoJSON parses successfully and contains 177 country polygon features, including India, United States, China, Russia, and Antarctica.
- Vite production build was attempted; the supplied archive's existing `node_modules` is Windows/Rollup-platform-specific and the Linux build environment lacks `@rollup/rollup-linux-x64-gnu`. This is the same dependency-environment limitation as the prior frontend build validation; source TypeScript compilation completed successfully before Vite stopped.
