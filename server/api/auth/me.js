// GET /api/auth/me  (Authorization: Bearer <session token>)
import { restEnv, resolveSession, getUser } from "../_lib/store.js";

export default async function handler(req, res) {
  res.setHeader("Cache-Control", "no-store");
  if (req.method !== "GET") return res.status(405).json({ error: "GET only" });

  const rest = restEnv();
  if (!rest) return res.status(200).json({ available: false, reason: "not_configured" });

  try {
    const userId = await resolveSession(rest, req);
    if (!userId) return res.status(401).json({ ok: false, error: "Not signed in" });
    const user = await getUser(rest, userId);
    if (!user) return res.status(401).json({ ok: false, error: "Not signed in" });
    res.status(200).json({
      ok: true, userId, email: user.email, name: user.name || "Learner",
    });
  } catch (err) {
    res.status(502).json({ error: err?.message || "Session check failed" });
  }
}
