// POST /api/auth/logout  (Authorization: Bearer <session token>)
import { restEnv, destroySession } from "../_lib/store.js";

export default async function handler(req, res) {
  res.setHeader("Cache-Control", "no-store");
  if (req.method !== "POST") return res.status(405).json({ error: "POST only" });

  const rest = restEnv();
  if (!rest) return res.status(200).json({ available: false, reason: "not_configured" });

  try {
    await destroySession(rest, req);
    res.status(200).json({ ok: true });
  } catch (err) {
    res.status(502).json({ error: err?.message || "Logout failed" });
  }
}
