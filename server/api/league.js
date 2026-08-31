// Alpaca weekly leagues — XP race with optional account identity.
//
// Storage: any Redis REST endpoint. Vercel KV (marketplace) and Upstash both
// speak the same REST protocol; set these env vars:
//   KV_REST_API_URL + KV_REST_API_TOKEN          (Vercel KV)
//   UPSTASH_REDIS_REST_URL + UPSTASH_REDIS_REST_TOKEN   (Upstash direct)
// Without them the API reports { available: false } and the app falls back
// to its offline preview — nothing crashes, nothing is stored.
//
// Identity: signed-in players send `Authorization: Bearer <session token>`
// (minted by /api/auth/login|signup) and race under their account id with the
// account name. Everyone else races anonymously under their device id.
//
// Endpoints (single handler):
//   GET  /api/league?deviceId=<id>            → standings (top 30 + your rank)
//   POST /api/league { deviceId, name, xp }   → add XP to the current week

import {
  restEnv, redisPipeline, resolveSession, getUser,
} from "./_lib/store.js";

const WEEK_TTL_SECONDS = 8 * 24 * 60 * 60; // keys self-clean after the week ends
const TOP_N = 30;
const MAX_XP_PER_REPORT = 500;

// ISO-8601 week id (e.g. "2026-W36"); leagues run Monday 00:00 UTC → Sunday.
function isoWeekId(date = new Date()) {
  const t = new Date(Date.UTC(date.getUTCFullYear(), date.getUTCMonth(), date.getUTCDate()));
  const dayNum = t.getUTCDay() || 7;
  t.setUTCDate(t.getUTCDate() + 4 - dayNum);
  const yearStart = new Date(Date.UTC(t.getUTCFullYear(), 0, 1));
  const week = Math.ceil(((t - yearStart) / 86400000 + 1) / 7);
  return `${t.getUTCFullYear()}-W${String(week).padStart(2, "0")}`;
}

// Milliseconds until next Monday 00:00 UTC.
function msUntilReset(date = new Date()) {
  const t = new Date(Date.UTC(date.getUTCFullYear(), date.getUTCMonth(), date.getUTCDate()));
  const dayNum = t.getUTCDay() || 7;
  const nextMonday = new Date(t);
  nextMonday.setUTCDate(t.getUTCDate() + (8 - dayNum));
  return nextMonday.getTime() - date.getTime();
}

/** Resolves the racing identity: account id when signed in, else device id. */
async function resolveIdentity(rest, req, fallbackDeviceId, fallbackName) {
  const userId = await resolveSession(rest, req);
  if (userId) {
    const user = await getUser(rest, userId);
    if (user) return { id: userId, name: user.name || fallbackName || "Learner" };
  }
  const deviceId = String(fallbackDeviceId || "").trim().slice(0, 64);
  if (!deviceId) return null;
  return { id: deviceId, name: String(fallbackName || "").trim().slice(0, 24) || "Anonymous" };
}

export default async function handler(req, res) {
  res.setHeader("Cache-Control", "no-store");

  const rest = restEnv();
  if (!rest) {
    res.status(200).json({ available: false, reason: "not_configured" });
    return;
  }

  const weekId = isoWeekId();
  const xpKey = `league:${weekId}:xp`;
  const namesKey = `league:${weekId}:names`;

  try {
    if (req.method === "POST") {
      const body = typeof req.body === "string" ? JSON.parse(req.body) : req.body || {};
      const xp = Math.floor(Number(body.xp) || 0);
      if (xp <= 0 || xp > MAX_XP_PER_REPORT) {
        res.status(400).json({ error: `xp must be 1..${MAX_XP_PER_REPORT}` });
        return;
      }
      const identity = await resolveIdentity(rest, req, body.deviceId, body.name);
      if (!identity) {
        res.status(400).json({ error: "deviceId required" });
        return;
      }

      await redisPipeline(rest, [
        ["zincrby", xpKey, xp, identity.id],
        ["expire", xpKey, WEEK_TTL_SECONDS],
        ["hset", namesKey, identity.id, identity.name],
        ["expire", namesKey, WEEK_TTL_SECONDS],
      ]);
      res.status(200).json({ available: true, week: weekId });
      return;
    }

    if (req.method === "GET") {
      const body = { deviceId: req.query.deviceId };
      const identity = await resolveIdentity(rest, req, body.deviceId, null);
      const [top, names, rank, score] = await redisPipeline(rest, [
        ["zrevrange", xpKey, 0, TOP_N - 1, "WITHSCORES"],
        ["hgetall", namesKey],
        identity ? ["zrevrank", xpKey, identity.id] : ["ping"],
        identity ? ["zscore", xpKey, identity.id] : ["ping"],
      ]);

      const entries = [];
      for (let i = 0; i < (top || []).length; i += 2) {
        entries.push({
          id: top[i],
          name: (names && names[top[i]]) || "Anonymous",
          xp: Math.round(Number(top[i + 1]) || 0),
        });
      }

      res.status(200).json({
        available: true,
        week: weekId,
        resetsInMs: msUntilReset(),
        entries,
        yourRank: typeof rank === "number" ? rank + 1 : null,
        yourXp: score != null ? Math.round(Number(score)) : null,
      });
      return;
    }

    res.status(405).json({ error: "GET or POST only" });
  } catch (err) {
    res.status(502).json({ available: false, reason: err?.message || "league backend error" });
  }
}
