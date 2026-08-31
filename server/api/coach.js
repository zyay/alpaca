// Post-call AI coach — turns a finished roleplay transcript into structured feedback.
// Uses the same GEMINI_API_KEY as /api/token but the text generation API (not Live).
//
// POST /api/coach
//   body: { language, level, scenario, transcript: [{ role: "tutor"|"user", text }] }
//   200 → { strengths: string[], improvements: [{ title, tip }], vocab: [{ term, translation }] }
//   503 → { error } when GEMINI_API_KEY is missing or the model call fails;
//         the app falls back to local heuristic feedback, nothing crashes.

const GENERATE_URL = (model) =>
  `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent`;

const COACH_MODEL = process.env.GEMINI_COACH_MODEL || "gemini-flash-latest";
const MAX_ENTRIES = 30;
const MAX_ENTRY_CHARS = 300;

function buildPrompt(language, level, scenario, transcript) {
  const lines = transcript
    .slice(-MAX_ENTRIES)
    .map((t) => `${t.role === "tutor" ? "Tutor" : "Learner"}: ${String(t.text).slice(0, MAX_ENTRY_CHARS)}`)
    .join("\n");

  return [
    `You are a concise, encouraging language coach for an app called Alpaca.`,
    `The learner just finished a spoken roleplay in ${language} at a ${level} level.`,
    `Scenario: ${scenario}`,
    `Transcript (Tutor speaks ${language}; Learner is the student):`,
    "",
    lines,
    "",
    "Analyze ONLY the learner's lines. Reply with ONLY a JSON object, no markdown fences, in this exact shape:",
    `{"strengths": [2-3 short sentences about what the learner did well, citing their actual words],`,
    ` "improvements": [{"title": "short label", "tip": "1-2 sentences of concrete, actionable advice with a corrected example"} x 3],`,
    ` "vocab": [{"term": "a useful ${language} word/phrase the learner struggled with or should know", "translation": "English translation"} x 3]}`,
    "Tone: warm, specific, never condescending. All prose in English except the vocab terms.",
  ].join("\n");
}

function coerceFeedback(data) {
  const strengths = Array.isArray(data?.strengths)
    ? data.strengths.filter((s) => typeof s === "string" && s.trim()).slice(0, 4)
    : [];
  const improvements = Array.isArray(data?.improvements)
    ? data.improvements
        .filter((i) => i && typeof i.title === "string" && typeof i.tip === "string")
        .slice(0, 4)
        .map((i) => ({ title: i.title.trim(), tip: i.tip.trim() }))
    : [];
  const vocab = Array.isArray(data?.vocab)
    ? data.vocab
        .filter((v) => v && typeof v.term === "string" && typeof v.translation === "string")
        .slice(0, 5)
        .map((v) => ({ term: v.term.trim(), translation: v.translation.trim() }))
    : [];
  if (!strengths.length && !improvements.length) return null;
  return { strengths, improvements, vocab };
}

function parseModelJson(text) {
  const cleaned = String(text).replace(/```json|```/g, "").trim();
  const start = cleaned.indexOf("{");
  const end = cleaned.lastIndexOf("}");
  if (start === -1 || end <= start) return null;
  try {
    return JSON.parse(cleaned.slice(start, end + 1));
  } catch {
    return null;
  }
}

export default async function handler(req, res) {
  res.setHeader("Cache-Control", "no-store");

  if (req.method !== "POST") {
    res.status(405).json({ error: "POST only" });
    return;
  }

  const apiKey = process.env.GEMINI_API_KEY;
  if (!apiKey) {
    res.status(503).json({ error: "GEMINI_API_KEY is not configured" });
    return;
  }

  const { language, level, scenario, transcript } = req.body || {};
  if (!Array.isArray(transcript) || transcript.length === 0) {
    res.status(400).json({ error: "transcript must be a non-empty array" });
    return;
  }
  if (!transcript.some((t) => t && t.role !== "tutor" && typeof t.text === "string" && t.text.trim())) {
    res.status(400).json({ error: "transcript has no learner lines" });
    return;
  }

  try {
    const resp = await fetch(`${GENERATE_URL(COACH_MODEL)}?key=${encodeURIComponent(apiKey)}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        contents: [
          {
            role: "user",
            parts: [
              {
                text: buildPrompt(
                  String(language || "Spanish"),
                  String(level || "beginner"),
                  String(scenario || "free conversation"),
                  transcript
                ),
              },
            ],
          },
        ],
        generationConfig: { temperature: 0.7, maxOutputTokens: 1024 },
      }),
    });

    const data = await resp.json();
    if (!resp.ok) {
      res.status(502).json({
        error: data?.error?.message || `Coach model call failed (${resp.status})`,
      });
      return;
    }

    const text = data?.candidates?.[0]?.content?.parts
      ?.map((p) => p.text || "")
      .join("");
    const parsed = parseModelJson(text);
    const feedback = parsed && coerceFeedback(parsed);
    if (!feedback) {
      res.status(502).json({ error: "Coach returned an unexpected shape" });
      return;
    }

    res.status(200).json(feedback);
  } catch (err) {
    res.status(502).json({ error: err?.message || "Coach call crashed" });
  }
}
