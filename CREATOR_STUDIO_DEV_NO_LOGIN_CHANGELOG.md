# Creator Studio — local development no-login mode

## What changed

- Added the explicit `VITE_CREATOR_STUDIO_DEV_NO_LOGIN` opt-in, guarded by Vite's `import.meta.env.DEV` compile-time flag.
- In that mode only, `/creator-studio` and `/creator-studio/:studioId` can render without signing in. Other routes and backend authentication are unchanged.
- The `Creator-Studio` header button appears while signed out only when this local development mode is active.
- Studio list, name availability, create, get, update and deactivate operations use browser `localStorage` in this mode. They do not call the backend. Data is local to the current browser profile.
- Added setup and disable instructions in `frontend/CREATOR_STUDIO_DEV_NO_LOGIN.md` and a safe-off `.env.example`.

## Enable locally

Create `frontend/.env.local` (do not commit it) with:

```dotenv
VITE_CREATOR_STUDIO_DEV_NO_LOGIN=true
```

Restart the Vite dev server and open `http://localhost:3000/creator-studio`.

## Safety

The code requires both `import.meta.env.DEV` and the explicit flag. Production builds compile `import.meta.env.DEV` as `false`, so setting the flag alone cannot enable this frontend bypass in a production build. No backend security configuration was changed; the backend continues to require its normal authentication. This mode is a local UI/flow demo, not a multi-user or persisted backend workflow.

## Validation

- Source inspection confirms the bypass condition is limited to the Creator Studio route and the Creator Studio API adapter.
- A full frontend TypeScript/Vite build could not be run in this environment because the dependency cache is incomplete and network package downloads are unavailable (`npm ci --offline` failed for missing cached `zustand@4.5.7`). Run `npm ci` and `npm run build` on the development machine to complete validation.
