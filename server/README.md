# Alpaca Token Server

Vercel serverless function that mints **short-lived Gemini Live ephemeral tokens**.
The raw `GEMINI_API_KEY` lives only in Vercel's environment — the Android APK
never contains it.

## How it works

`POST /api/token` →

1. Calls Google's `POST /v1beta/auth_tokens` with the server-side key.
2. Returns `{ token, wsUrl, modelId }` where `wsUrl` is a ready-to-open
   WebSocket URL with the ephemeral token embedded (`access_token=`).
3. Tokens are single-use and expire after 15 minutes.

## Deploy

```bash
cd server
vercel login          # browser OAuth
vercel                # link project (accept defaults)
vercel env add GEMINI_API_KEY production   # paste key when prompted — never in chat
vercel env add GEMINI_MODEL_ID production # optional, defaults to gemini-3.1-flash-live-preview
vercel deploy --prod
```

Then point the app at it: set `VERCEL_BASE_URL=https://<your-project>.vercel.app`
in `local.properties` and rebuild the APK. The app now calls this backend first
and falls back to the (debug-only) direct key in `BuildConfig.GEMINI_API_KEY`.

Docs: https://ai.google.dev/gemini-api/docs/ephemeral-tokens
