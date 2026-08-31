# Alpaca

**Stop translating. Start talking.**

A Duolingo-style multi-language learning app for Android — 100% native Kotlin +
Jetpack Compose — with real-time AI voice roleplay powered by the
[Gemini Live API](https://ai.google.dev/gemini-api/docs/live-api).

When you slip up, a droplet splatters the screen, a red ✗ stamps in, and it
clears to reveal the correct answer with a one-line explanation of *why*.
No characters, no fluff — just the physics.

## What's inside v0.9.0

| Feature | Details |
|---|---|
| 🌍 **7 courses** | **Spanish** (5 units), **English** (3), **French** (2), **German** (2), **Italian** (2), **Portuguese** (2), **Russian** (2) — switch courses from the trail or Settings; Japanese is *coming soon* |
| 🗺️ **The trail** | Winding gamified lesson path — **18 regions, 90 lessons, ~900 exercises** with sequential unit unlocking per course |
| 📚 **5 exercise types** | multiple choice · match pairs · fill-in-the-blank · listening (TTS) · pronunciation — every wrong-answer exercise carries a one-line grammar explanation |
| 🎙️ **Word-level pronunciation scoring** | Diacritic-folding (café≈cafe, Straße≈strasse, ё≈е) + word-alignment grading that shows exactly which words were clear (green) and which to retry (red) |
| 💬 **Real-World Simulator** | Live voice calls (coffee, ticket, directions, friend, market, hotel, doctor, restaurant) over the Gemini Multimodal Live API — full-duplex audio, barge-in interruption, **live captions for both speakers**, one-shot auto-reconnect on network drops |
| 🎚️ **Difficulty levels & voices** | Beginner / Confident / Advanced personas (vocabulary, pace, correction style) and 4 tutor voices — persisted per device |
| 🧠 **Post-call AI coach** | After each call the transcript goes to a Gemini text model that returns strengths, 3 concrete improvements and vocabulary to remember — with a graceful fallback when the backend is unavailable |
| 🔄 **Self-updater** | Checks GitHub releases for a newer version (max every 6 h), shows an update banner on the trail and a version card in Settings, then downloads the new APK via the system DownloadManager and hands it to the Android installer — sideloaded installs stay current with zero fuss |
| ✨ **Motion everywhere** | Duolingo-grade physics: count-up XP/gems/coins, spring-pop stat chips on change, staggered card entrances on quests/leaderboard/achievements, progress bars that fill on open, gloss sweeps over unlocked badges — plus confetti, pulsing nodes, hearts, and slide+fade screen transitions |
| 👤 **Accounts (optional)** | Email + password sign-up/log-in via the Vercel backend — scrypt-hashed passwords, opaque 30-day sessions; signed-in users race leagues under their account identity, guests stay anonymous |
| 💎 **Gems & daily quests** | 3 deterministic quests per day (XP / lessons / coins); claim gems, spend them in the gem shop |
| ❄️ **Streak Freeze** | Buy up to 2 freezes (200 gems) — a missed day no longer kills your streak; or refill all fleece hearts for 350 gems |
| 🏟️ **Real online leagues** | Anonymous weekly XP race on a Redis-backed Vercel endpoint — top 30 standings, your live rank, promotion zone, resets Monday 00:00 UTC; offline builds fall back to a local practice herd |
| 🛒 **Alpaca Max billing** | Play Billing subscription (`alpaca_max_monthly`) for unlimited fleece energy, wired end-to-end (see [docs/playstore-listing.md](docs/playstore-listing.md)) |
| 🔁 **Mistake review** | Every mistake is logged; a dedicated review lesson rebuilds exercises from your personal error history |
| 🏅 **Achievements** | 8 unlockable badges (first lesson, streaks, perfect lessons, voice calls, XP milestones) |
| 🔥 **Gamification** | XP, daily streaks, coins, Fleece Energy (5 tufts, regrows 1 per 30 min), confetti, spring physics + haptics everywhere |
| 🔊 **Sound design** | Zero-asset sound effects via platform `ToneGenerator` (correct/wrong/select/finish), gated by the sound preference |
| 👋 **Onboarding** | Rotating seven-language hero, floating flags, learns your name, then drops you on the trail |
| 🔐 **Key-less distribution** | Voice calls authenticate with **short-lived ephemeral tokens** minted by a Vercel serverless function — the raw API key never ships in the APK |
| 🎨 **Design** | Duolingo-grade design system: `#58cc02` brand green, weight-800 headings, pill buttons with hard 3D edges, gradient-orb voice avatar, **true dark mode on every screen (cards, borders, text adapt automatically)**, adaptive launcher icon (speech-bubble wordmark, monochrome-ready), branded splash screen, slide+fade navigation transitions |

**Voice-chat latency architecture:** `AudioRecord` (16 kHz PCM) → OkHttp
WebSocket → Gemini Live → `AudioTrack` (24 kHz PCM), all behind an `AudioEngine`
interface so a later Oboe swap needs no caller changes. Pronunciation scoring
sits behind a `PronunciationGrader` interface — today it uses on-device
`SpeechRecognizer` with per-language locales; the seam is ready for a LiteRT
model trained on [Mozilla Common Voice](https://commonvoice.mozilla.org/).

## Try the APK

Grab `app-debug.apk` from the
[latest release](https://github.com/zyay/alpaca/releases/latest) and install it.
Afterwards the app keeps itself current: it checks GitHub for newer releases
(at most every 6 h) and offers a one-tap download + install from the trail
banner or Settings → App version.
Everything except live voice chat works out of the box; the app is fully usable
as a guest — accounts and online leagues light up once the backend has Redis
(see below).

### Voice calls — the safe path (recommended)

Voice calls need a Gemini Live credential. The clean way is the bundled Vercel
token server, so no key ever reaches the APK:

```bash
cd server
vercel login
vercel                       # link the project
vercel env add GEMINI_API_KEY production   # paste your key when prompted
vercel deploy --prod
```

Then in the project root's `local.properties`:

```properties
sdk.dir=C\:\\Users\\you\\AppData\\Local\\Android\\Sdk
VERCEL_BASE_URL=https://your-project.vercel.app
```

Rebuild, and the app mints a single-use token (15 min TTL) per call session.
See [server/README.md](server/README.md).

### Voice calls — local-dev fallback

For quick device testing you can skip the backend and bake a key into
`BuildConfig` (debug builds only, `local.properties` is gitignored — never
commit or redistribute the resulting APK):

```properties
GEMINI_API_KEY=your_key_here
```

Get a key at [Google AI Studio](https://aistudio.google.com/apikey).

Build:

```bash
export JAVA_HOME="C:/Program Files/Android/Android Studio/jbr"   # or your JDK 17+
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Voice testing works best on a physical device; on an emulator enable the
virtual microphone in AVD settings.

## Adding a course

Drop a `<lang>_unitN.json` file into `app/src/main/assets/content/` following
the shape of `spanish_unit1.json` (unitId prefix `<lang>_uN`, language-prefixed
lesson ids), register a `CourseLanguage` in
`data/content/CourseLanguage.kt`, and the picker, trail, TTS, pronunciation
grader and voice personas pick it up automatically. Progress is stored per
lesson id, so new languages need no database migration.

## Project layout

```
app/src/main/java/com/alpaca/app/
├── gemini/      Gemini Live WebSocket client + ephemeral-token client
├── audio/       AudioEngine, TTS, pronunciation grader, sound effects
├── billing/     Play Billing manager (Alpaca Max subscription)
├── data/        Room, DataStore, auth + coach clients, leagues, quests, mistake log, content models + bundled JSON
└── ui/          onboarding · trail · lesson · summary · voice · quests · achievements · leaderboard · auth · settings · languages
server/api/      Vercel functions: ephemeral token minting + AI coach + accounts (email/password) + weekly league (Redis REST)
docs/            Play Store listing draft + privacy policy
```

Architecture: MVVM, manual DI (`AppContainer`), coroutines + Flow, type-safe
Navigation Compose routes, no third-party DI. Content is validated by a JVM
unit test (`ContentParsingTest`) at build time — it checks every unit parses,
lesson ids are unique across all languages, and pairs are well-formed.

## Leagues backend

`GET/POST /api/league` on the Vercel project stores weekly XP in any Redis
REST instance (Vercel KV or Upstash — same protocol). Configure either pair of
env vars on the project and the leaderboard goes live; without them the
endpoint reports `{ available: false }` and the app shows its offline
practice-herd preview. Week windows are ISO weeks, Monday 00:00 UTC.

## Accounts backend

The same Redis REST instance powers optional email + password accounts:
`POST /api/auth/signup`, `POST /api/auth/login`, `GET /api/auth/me`,
`POST /api/auth/logout`. Passwords are hashed with Node's `scrypt` (16-byte
salt, timing-safe verification) — plaintext never touches the server. Sessions
are opaque 32-byte tokens stored in Redis with a 30-day TTL; the app keeps the
token in DataStore and sends it as a bearer header on league calls, so a
signed-in user keeps one league identity across devices and reinstalls.
Without Redis configured every auth endpoint degrades to
`{ available: false }` and the app stays in guest mode.

## AI coach backend

`POST /api/coach` forwards the post-call transcript to a Gemini text model
(`GEMINI_COACH_MODEL`, default `gemini-flash-latest`) and returns structured
feedback: strengths, three concrete improvements and vocabulary with
translations. It uses the same `GEMINI_API_KEY` as token minting; if the key is
missing or the model call fails the endpoint returns 503/502 and the app shows
a graceful fallback. Transcripts are processed in memory only — nothing from a
call is stored.

## Roadmap

- Japanese course
- Friend quests + league promotion/demotion tiers
- LiteRT pronunciation model trained on Common Voice (GPU delegate; NNAPI is deprecated)
- Oboe audio engine for lower latency
- HF-datasets content pipeline (tatoeba/opus100) + RAG for cultural notes
- Play Store release (signing + listing docs are ready in `docs/`)

Made loud in the Andes.
