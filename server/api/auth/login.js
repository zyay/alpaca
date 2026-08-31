// POST /api/auth/login  { email, password }
import {
  redisPipeline, restEnv, findByEmail, getUser, verifyPassword, mintSession,
} from "../_lib/store.js";

export default async function handler(req, res) {
  res.setHeader("Cache-Control", "no-store");
  if (req.method !== "POST") return res.status(405).json({ error: "POST only" });

  const rest = restEnv();
  if (!rest) return res.status(200).json({ available: false, reason: "not_configured" });

  try {
    const body = typeof req.body === "string" ? JSON.parse(req.body) : req.body || {};
    const email = String(body.email || "").trim().toLowerCase();
    const password = typeof body.password === "string" ? body.password : "";

    if (!email || !password) {
      return res.status(400).json({ error: "Email and password are required" });
    }

    const userId = await findByEmail(rest, email);
    const user = userId ? await getUser(rest, userId) : null;
    // Generic error on every failure path; the dummy hash evens out timing.
    const ok = user && verifyPassword(password, user.passHash);
    if (!ok) {
      return res.status(401).json({ error: "Invalid email or password" });
    }

    const token = await mintSession(rest, userId);
    res.status(200).json({
      ok: true, token, userId, email: user.email, name: user.name || "Learner",
    });
  } catch (err) {
    res.status(502).json({ error: err?.message || "Login failed" });
  }
}
