# Creator Studio local no-login mode

This is a **local development demo mode only**. It bypasses login only for the `/creator-studio` frontend route and stores demo studios in this browser's `localStorage`. It does not call the Creator Studio backend and does not change backend authentication or authorization. Other application routes remain protected as before.

## Enable on your machine

1. In the `frontend` directory, create a file named `.env.local` (do not commit it).
2. Add this exact line:

   ```dotenv
   VITE_CREATOR_STUDIO_DEV_NO_LOGIN=true
   ```

3. Start the Vite development server with `npm run dev` (or restart it if already running).
4. Open `http://localhost:3000/creator-studio`. The header button is shown even when signed out.

## Disable

Remove the line or set it to `false`, then restart Vite. To clear local demo studios, run this in the browser developer console on your local app origin:

```js
localStorage.removeItem('creator-studio-dev-no-login:studios:v1');
```

## Safety boundary

The feature requires both `import.meta.env.DEV` and the explicit environment flag. Vite compiles `import.meta.env.DEV` to `false` in production builds, so setting the variable in a production environment does not enable the bypass. Never use `vite --host 0.0.0.0` for this demo mode on an untrusted network. Demo data is local to one browser and is not saved to the backend or shared with other users.
