// Alpaca weekly leagues — anonymous, Redis-backed XP race.
//
// Storage: any Redis REST endpoint. Vercel KV (marketplace) and Upstash both
// speak the same REST protocol; set these env vars:
//   KV_REST_API_URL + KV_REST_API_TOKEN          (Vercel KV)
//   UPSTASH_REDIS_REST_URL + UPSTASH_REDIS_REST_TOKEN   (Upstash direct)
// Without them the API reports { available: false } and the app falls back
// to its offline preview — nothing crashes, nothing is stored.
//
// Endpoints (single handler):
//   GET  /api/league?deviceId=<id>            → standings (top 30 + your rank)
//   POST /api/league { deviceId, name, xp }   → add XP to the current week

const WEEK_TTL_SECONDS = 8 * 24 * 60 * 60; // keys self-clean after the week ends
const TOP_N = 30;
const MAX_XP_PER_REPORT = 500;

function restEnv() {
  const url = process.env.KV_REST_API_URL || process.env.UPSTASH_REDIS_REST_URL;
  const token = process.env.KV_REST_API_TOKEN || process.env.UPSTASH_REDIS_REST_TOKEN;
  if (!url || !token) return null;
  return { url: url.replace(/\/+$/, ""), token };
}

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

async function redisPipeline(rest, commands) {
  const resp = await fetch(`${rest.url}/pipeline`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${rest.token}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify(commands),
  });
  if (!resp.ok) throw new Error(`Redis REST ${resp.status}`);
  const data = await resp.json();
  return data.map((entry) => (entry && "result" in entry ? entry.result : null));
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
      const deviceId = String(body.deviceId || "").trim().slice(0, 64);
      const name = String(body.name || "").trim().slice(0, 24) || "Anonymous";
      const xp = Math.floor(Number(body.xp) || 0);

      if (!deviceId) {
        res.status(400).json({ error: "deviceId required" });
        return;
      }
      if (xp <= 0 || xp > MAX_XP_PER_REPORT) {
        res.status(400).json({ error: `xp must be 1..${MAX_XP_PER_REPORT}` });
        return;
      }

      await redisPipeline(rest, [
        ["zincrby", xpKey, xp, deviceId],
        ["expire", xpKey, WEEK_TTL_SECONDS],
        ["hset", namesKey, deviceId, name],
        ["expire", namesKey, WEEK_TTL_SECONDS],
      ]);
      res.status(200).json({ available: true, week: weekId });
      return;
    }

    if (req.method === "GET") {
      const deviceId = String(req.query.deviceId || "").trim().slice(0, 64);
      const [top, names, rank, score] = await redisPipeline(rest, [
        ["zrevrange", xpKey, 0, TOP_N - 1, "WITHSCORES"],
        ["hgetall", namesKey],
        deviceId ? ["zrevrank", xpKey, deviceId] : ["ping"],
        deviceId ? ["zscore", xpKey, deviceId] : ["ping"],
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
