# Live TV UI / Routing Fix

This revision keeps the existing Live TV API and HLS playback path intact and changes only the Live TV presentation/navigation layer.

## Routes
- `/live` — all channels
- `/live/:category` — category page, e.g. `/live/kids`
- `/live/channel/:channelId` — player route from all channels
- `/live/:category/channel/:channelId` — player route from a category

## Behaviour
- Category selections are reflected in the URL.
- Browser Back from `/live/kids` returns to `/live`, not the platform home page.
- Closing a channel player returns to its Live TV category.
- Live TV routes bypass the normal platform header/sidebar layout.
- Globe has a continuous 3D rotation animation with reduced-motion support.
- Local time refreshes automatically.
- Third-party logo hosts that caused repeated DNS/CORS/404 console noise are not requested by the UI; the existing initials fallback is used until local logo assets are supplied.
- Existing channel APIs, pagination, filters, favorites, view recording and HLS player are preserved.

## Validation
- TypeScript: `frontend/node_modules/.bin/tsc --noEmit` passes.
- Vite production build could not be executed in the Linux build environment because the archived Windows `node_modules` tree does not contain Rollup's Linux optional native package (`@rollup/rollup-linux-x64-gnu`). Run `npm ci` on the Windows development machine before `npm run build`.
