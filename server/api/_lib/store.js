// Shared auth + Redis REST helpers for the Alpaca API.
// Files/folders prefixed with "_" inside /api are not routed by Vercel.
import { scryptSync, randomBytes, timingSafeEqual } from "node:crypto";

const SESSION_TTL_SECONDS = 30 * 24 * 60 * 60; // 30 days

export function restEnv() {
  const url = process.env.KV_REST_API_URL || process.env.UPSTASH_REDIS_REST_URL;
  const token = process.env.KV_REST_API_TOKEN || process.env.UPSTASH_REDIS_REST_TOKEN;
  if (!url || !token) return null;
  return { url: url.replace(/\/+$/, ""), token };
}

export async function redisPipeline(rest, commands) {
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

// --- password hashing: scrypt with per-user random salt ---
const SCRYPT = { N: 16384, r: 8, p: 1 };

export function hashPassword(password) {
  const salt = randomBytes(16).toString("hex");
  const key = scryptSync(password, salt, 64, SCRYPT).toString("hex");
  return `s1$${salt}$${key}`;
}

// Precomputed dummy so a login for a missing email burns the same CPU time.
const DUMMY_HASH = hashPassword(randomBytes(16).toString("hex"));

export function verifyPassword(password, stored) {
  const parts = String(stored || "").split("$");
  if (parts.length !== 3 || parts[0] !== "s1") {
    scryptSync(password, "x", 64, SCRYPT);
    return false;
  }
  const [, salt, expected] = parts;
  const actual = scryptSync(password, salt, 64, SCRYPT);
  const expectedBuf = Buffer.from(expected, "hex");
  if (expectedBuf.length !== actual.length) return false;
  return timingSafeEqual(actual, expectedBuf);
}

// --- users ---
export function validEmail(email) {
  return /^[^\s@]{1,64}@[^\s@]{1,255}\.[^\s@]{2,}$/.test(email) && email.length <= 120;
}

export function validPassword(password) {
  return typeof password === "string" && password.length >= 8 && password.length <= 128;
}

export function validName(name) {
  return name.length >= 1 && name.length <= 24;
}

export async function findByEmail(rest, email) {
  const [userId] = await redisPipeline(rest, [["get", `user-email:${email}`]]);
  return userId || null;
}

export async function createUser(rest, email, passHash, name) {
  const userId = randomBytes(16).toString("hex");
  await redisPipeline(rest, [
    ["hset", `user:${userId}`, "email", email, "passHash", passHash, "name", name, "createdAt", new Date().toISOString()],
    ["set", `user-email:${email}`, userId],
  ]);
  return userId;
}

export async function getUser(rest, userId) {
  const [user] = await redisPipeline(rest, [["hgetall", `user:${userId}`]]);
  if (!user || !user.email) return null;
  return user;
}

// --- sessions ---
export async function mintSession(rest, userId) {
  const token = randomBytes(32).toString("hex");
  await redisPipeline(rest, [
    ["set", `session:${token}`, userId, "EX", SESSION_TTL_SECONDS],
  ]);
  return token;
}

export function bearerToken(req) {
  const header = req.headers?.authorization || "";
  const match = /^Bearer\s+(.+)$/i.exec(header);
  return match ? match[1].trim().slice(0, 128) : null;
}

export async function resolveSession(rest, req) {
  const token = bearerToken(req);
  if (!token) return null;
  const [userId] = await redisPipeline(rest, [["get", `session:${token}`]]);
  return userId || null;
}

export async function destroySession(rest, req) {
  const token = bearerToken(req);
  if (!token) return;
  await redisPipeline(rest, [["del", `session:${token}`]]);
}
