# Play Store listing draft — Alpaca

Working package: `com.alpaca.app` · contact: set up a Play Console developer account first.

## App details

- **App name:** Alpaca: Learn Spanish & More
- **Short description (80 chars max):**
  Learn Spanish, French, German, Italian & Portuguese with bitesized lessons.
- **Full description:**

  Alpaca turns learning a language into a game you actually want to play.

  Climb illustrated lesson trails through golden villages, Tuscan hills and the
  Atlantic coast. Five languages — Spanish, French, German, Italian and
  Portuguese — each with real conversations, listening practice and speaking
  drills powered by your own voice.

  - Bite-sized lessons that take 3 minutes, not 30
  - Daily quests and gem rewards keep you coming back
  - Streak freezes protect your progress on off days
  - Weekly leagues: race real learners, top 5 get promoted
  - Speak out loud in roleplay conversations with an AI tutor
  - No mascots, no fluff — just you, the trail, and the words

  Download once, learn anywhere. Works fully offline for lessons; voice
  roleplay and leagues need a connection.

## Assets checklist (before submission)

- [ ] App icon 512×512 PNG (use the alpaca glyph, green background #58CC02)
- [ ] Feature graphic 1024×500
- [ ] At least 2 phone screenshots per language bucket (Trail, Lesson, Quests, League)
- [ ] Content rating questionnaire (likely "Everyone")
- [ ] Data safety form — see below
- [ ] Privacy policy URL (host docs/privacy-policy.md on GitHub Pages)

## Data safety form answers

| Question | Answer | Why |
| --- | --- | --- |
| Does the app collect personal data? | No personal data | No accounts, no emails |
| Device or other IDs | Collected — App functionality | Anonymous random `device_id` minted in DataStore, used only for league standings |
| App activity / in-app actions | Collected — App functionality | Anonymous weekly XP (a number) tied to the same device id |
| Audio | Not collected | Voice roleplay audio streams to Google's Gemini API for a response and is not stored by us |
| Purchases | Purchase history — App functionality | Google Play Billing subscription state only |
| Data encrypted in transit? | Yes | HTTPS + WSS everywhere |
| Data deletable? | Yes | Clear app data removes everything; leagues expire weekly |

## In-app products

- Subscription: `alpaca_max_monthly` — "Alpaca Max"
  - Base plan: monthly auto-renew, PPM pricing per target market
  - Entitlement: unlimited fleece energy (enforced client-side; no server entitlement check yet — acceptable for a hobby app, revisit before scaling)

## Build for upload

```
export JAVA_HOME="C:/Program Files/Android/Android Studio/jbr"
./gradlew bundleRelease     # AAB for Play
```

Signing keys are read from `local.properties` (never committed):

```
ALPACA_KEYSTORE_FILE=../keys/alpaca.jks
ALPACA_KEYSTORE_PASSWORD=...
ALPACA_KEY_ALIAS=alpaca
ALPACA_KEY_PASSWORD=...
```

Generate a keystore once with:
`keytool -genkeypair -v -keystore keys/alpaca.jks -alias alpaca -keyalg RSA -keysize 4096 -validity 10000`

Keep the keystore + passwords backed up outside this repo — losing them loses
the store listing.
