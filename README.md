# Alpaca 🦙

**Stop translating. Start talking.**

A Duolingo-style Spanish-learning app for Android — 100% native Kotlin + Jetpack
Compose — with real-time AI voice roleplay powered by the
[Gemini Live API](https://ai.google.dev/gemini-api/docs/live-api).

Meet **Paco**, the neon-green alpaca in a backward cap. He bounces when you're on
a streak, and when you get an answer wrong he plays his signature move: a
**spit-take** — a droplet of water splatters the screen, clears, and reveals the
correct answer with a one-line explanation of *why*.

## What's inside v0.1.0

| Feature | Details |
|---|---|
| 🗺️ **The Andes Trail** | Winding gamified lesson path with sequential unlocking and persistent progress (Room) |
| 📚 **5 full lessons** | Greetings · Numbers · Ordering food · Introductions · Directions — 50 exercises, 5 types: multiple choice, match pairs, fill-in-the-blank, listening (TTS), pronunciation |
| 🔥 **Gamification** | XP, daily streaks, Paco Coins, Fleece Energy (5 tufts, regrows 1 per 30 min), confetti, spring physics + haptics everywhere |
| 💬 **Real-World Simulator** | Live voice calls with Paco (café, train station, directions, new friend) over the Gemini Multimodal Live API — full-duplex audio, barge-in interruption, scenario personas |
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

To enable voice calls:

1. Get a key from [Google AI Studio](https://aistudio.google.com/apikey).
2. Create `local.properties` in the project root:
   ```properties
   sdk.dir=C\:\\Users\\you\\AppData\\Local\\Android\\Sdk
   GEMINI_API_KEY=your_key_here
   ```
3. Build and install (needs JDK 17+ — Android Studio's JBR works):
   ```bash
   export JAVA_HOME="C:/Program Files/Android/Android Studio/jbr"   # or your JDK 17+
   ./gradlew assembleDebug
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

> **Security:** the prototype ships the key in `BuildConfig` from
> `local.properties` (gitignored) for convenience. Production must use a
> backend-issued ephemeral token instead — never embed a real key in a
> distributed app.

Voice testing works best on a physical device; on an emulator enable the
virtual microphone in AVD settings.

## Project layout

```
app/src/main/java/com/alpaca/app/
├── gemini/      Gemini Live WebSocket client + protocol (the spicy part)
├── audio/       AudioEngine, TTS, pronunciation grader
├── data/        Room, DataStore, content models + bundled JSON lessons
└── ui/          trail · lesson · summary · voice · leaderboard · settings
```

Architecture: MVVM, manual DI (`AppContainer`), coroutines + Flow, type-safe
Navigation Compose routes, no third-party DI.

## Roadmap

- Real Herd Leagues + friend quests (backend: Firebase or FastAPI)
- LiteRT pronunciation model trained on Common Voice (GPU delegate; NNAPI is deprecated)
- Oboe audio engine for lower latency
- HF-datasets content pipeline (tatoeba/opus100) + RAG for cultural notes
- Play Billing for **Alpaca Max**, ads on the free tier
- Play Store release: signing, R8, listing, privacy policy

Made with 🦙 in the Andes.
