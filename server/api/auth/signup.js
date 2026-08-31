// POST /api/auth/signup  { email, password, name }
// Creates an account (email identifier, scrypt-hashed password) and a session.
import {
  redisPipeline, restEnv, validEmail, validPassword, validName,
  findByEmail, createUser, hashPassword, mintSession,
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
    const name = String(body.name || "").trim().slice(0, 24) || "Learner";

    if (!validEmail(email)) return res.status(400).json({ error: "Enter a valid email address" });
    if (!validPassword(password)) return res.status(400).json({ error: "Password must be 8-128 characters" });
    if (!validName(name)) return res.status(400).json({ error: "Name must be 1-24 characters" });

    if (await findByEmail(rest, email)) {
      return res.status(409).json({ error: "An account with this email already exists" });
    }

    const userId = await createUser(rest, email, hashPassword(password), name);
    const token = await mintSession(rest, userId);
    res.status(200).json({ ok: true, token, userId, email, name });
  } catch (err) {
    res.status(502).json({ error: err?.message || "Signup failed" });
  }
}
