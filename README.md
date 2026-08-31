# Alpaca 🦙

**Stop translating. Start talking.**

A Duolingo-style Spanish-learning app for Android — 100% native Kotlin + Jetpack
Compose — with real-time AI voice roleplay powered by the
[Gemini Live API](https://ai.google.dev/gemini-api/docs/live-api).

Meet **Paco**, the neon-green alpaca in a backward cap. He bounces when you're on
a streak, and when you get an answer wrong he plays his signature move: a
**spit-take** — a droplet of water splatters the screen, clears, and reveals the
correct answer with a one-line explanation of *why*.

## What's inside v0.2.0

| Feature | Details |
|---|---|
| 🗺️ **The Andes Trail** | Winding gamified lesson path — **5 regions, 25 lessons, 250 exercises** with sequential unit unlocking |
| 📚 **6 exercise types' worth of content** | multiple choice · match pairs · fill-in-the-blank · listening (TTS) · pronunciation — every wrong-answer exercise carries a one-line grammar explanation |
| 🔁 **Repaso de errores** | Every mistake is logged; a dedicated review lesson rebuilds exercises from your personal error history |
| 🏅 **Achievements** | 8 unlockable badges (first lesson, streaks, perfect lessons, voice calls, XP milestones) |
| 🔥 **Gamification** | XP, daily streaks, Paco Coins, Fleece Energy (5 tufts, regrows 1 per 30 min), confetti, spring physics + haptics everywhere |
| 🔊 **Sound design** | Zero-asset sound effects via platform `ToneGenerator` (correct/wrong/select/finish), gated by the sound preference |
| 👋 **Onboarding** | Paco greets you, learns your name, then drops you on the trail |
| 💬 **Real-World Simulator** | Live voice calls (café, market, hotel, doctor, reservation, ticket, directions, new friend) over the Gemini Multimodal Live API — full-duplex audio, barge-in interruption, scenario personas |
| 🔐 **Key-less distribution** | Voice calls authenticate with **short-lived ephemeral tokens** minted by a Vercel serverless function — the raw API key never ships in the APK |
| 🦙 **Paco, drawn in code** | The mascot is a Canvas-drawn character with 4 animated moods (idle bounce, happy hop, spit-take, sad) — zero image assets |
| 🎨 **Design** | Material 3 + Material You dynamic color, Duolingo-style pill buttons with hard 3D edges |

**Voice-chat latency architecture:** `AudioRecord` (16 kHz PCM) → OkHttp
WebSocket → Gemini Live → `AudioTrack` (24 kHz PCM), all behind an `AudioEngine`
interface so a later Oboe swap needs no caller changes. Pronunciation scoring
sits behind a `PronunciationGrader` interface — today it uses on-device
`SpeechRecognizer`; the seam is ready for a LiteRT model trained on
[Mozilla Common Voice](https://commonvoice.mozilla.org/).

## Try the APK

Grab `app-debug.apk` from the
[latest release](https://github.com/zyay/alpaca/releases/latest) and install it.
Everything except live voice chat works out of the box.

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

## Project layout

```
app/src/main/java/com/alpaca/app/
├── gemini/      Gemini Live WebSocket client + ephemeral-token client
├── audio/       AudioEngine, TTS, pronunciation grader, sound effects
├── data/        Room, DataStore, mistake log, content models + bundled JSON
└── ui/          onboarding · trail · lesson · summary · voice · achievements · leaderboard · settings
server/api/      Vercel function: mints Gemini Live ephemeral tokens
```

Architecture: MVVM, manual DI (`AppContainer`), coroutines + Flow, type-safe
Navigation Compose routes, no third-party DI. Content is validated by a JVM
unit test (`ContentParsingTest`) at build time.

## Roadmap

- Real Herd Leagues + friend quests (backend: Firebase or FastAPI)
- LiteRT pronunciation model trained on Common Voice (GPU delegate; NNAPI is deprecated)
- Oboe audio engine for lower latency
- HF-datasets content pipeline (tatoeba/opus100) + RAG for cultural notes
- Play Billing for **Alpaca Max**, ads on the free tier
- Play Store release: signing, R8, listing, privacy policy

Made with 🦙 in the Andes.
