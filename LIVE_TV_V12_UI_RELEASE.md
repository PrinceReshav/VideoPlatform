# Live TV V12 UI Release

## Baseline
V12 is based directly on the supplied `video-platform-live-tv-famelack-v11-complete.zip`.

## Scope
Frontend/UI experience changes only. The stable Live-TV backend, IPTV candidate/failover logic, playback implementation, search implementation, and uploaded-VOD player were not modified.

## V12 changes
- Fixed desktop Live-TV composition: left channel guide + centered Earth workspace.
- Removed automatic Earth rotation. The Earth is stationary until the user drags it.
- Country selection is exposed as real buttons for every country returned by the backend, with an independent horizontal country-picker scrollbar.
- Selecting a country updates the server-side channel query using the existing country filter.
- Existing category selection remains server-side and intersects with country selection.
- Category-first then country works: category narrows countries, then country narrows channels.
- Country-first then category works: country narrows categories, then category narrows channels.
- Category rail is hidden by default and opens with a right-edge toggle.
- Category rail is an overlay so opening it does not resize or move the center/globe workspace.
- Only the category list scrolls vertically inside the category rail.
- Channel guide keeps its own independent vertical scroll.
- Main workspace and center Earth area do not create a page-level scrollbar.
- Facet queries retain previous data while the next server-side filter response is loading, reducing visual flashing.
- Channel guide header now displays the active country/category intersection.
- Existing search, pagination, favorites, logos, playback modal, IPTV/YouTube fallback, and player modes remain intact.

## Validation
- TypeScript `tsc --noEmit`: passed.
- Full Vite production build could not be completed in the Linux build container because the supplied V11 `node_modules` contains Windows Rollup optional binaries and the Linux optional Rollup package could not be installed within the available network/build window. No package-lock changes were made.
- Backend sources were not modified for V12.
