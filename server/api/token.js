// Mints a short-lived Gemini Live ephemeral token.
// The raw GEMINI_API_KEY lives only in Vercel env vars — never in the APK.
//
// Docs: https://ai.google.dev/gemini-api/docs/ephemeral-tokens
// POST https://generativelanguage.googleapis.com/v1beta/auth_tokens
//   headers: x-goog-api-key
//   body: { uses, expireTime, liveConnectConstraints: { fieldMask } }
//   response: token value under `name`

const LIVE_ENDPOINT =
  "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent";
const AUTH_TOKENS_URL =
  "https://generativelanguage.googleapis.com/v1beta/auth_tokens";
// Must cover every setup field the app sends in its first WS message.
const FIELD_MASK = [
  "setup.model",
  "setup.generationConfig",
  "setup.systemInstruction",
];
const TOKEN_TTL_MS = 15 * 60 * 1000;

export default async function handler(req, res) {
  res.setHeader("Cache-Control", "no-store");

  if (req.method !== "POST") {
    res.status(405).json({ error: "POST only" });
    return;
  }

  const apiKey = process.env.GEMINI_API_KEY;
  if (!apiKey) {
    res.status(500).json({
      error:
        "GEMINI_API_KEY is not configured. Run: vercel env add GEMINI_API_KEY production",
    });
    return;
  }

  const expireTime = new Date(Date.now() + TOKEN_TTL_MS).toISOString();

  try {
    const resp = await fetch(AUTH_TOKENS_URL, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "x-goog-api-key": apiKey,
      },
      body: JSON.stringify({
        uses: 1,
        expireTime,
        liveConnectConstraints: { fieldMask: FIELD_MASK },
      }),
    });

    const data = await resp.json();
    if (!resp.ok) {
      const message = data?.error?.message || "Token mint failed";
      res.status(resp.status === 401 || resp.status === 403 ? 401 : 502).json({
        error: message,
      });
      return;
    }

    const token = data.name || data.token;
    if (!token) {
      res
        .status(502)
        .json({ error: "Unexpected auth_tokens response shape" });
      return;
    }

    res.status(200).json({
      token,
      wsUrl: `${LIVE_ENDPOINT}?access_token=${encodeURIComponent(token)}`,
      modelId: process.env.GEMINI_MODEL_ID || "gemini-3.1-flash-live-preview",
    });
  } catch (err) {
    res.status(502).json({ error: err?.message || "Token mint crashed" });
  }
}
