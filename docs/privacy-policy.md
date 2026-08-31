# Alpaca — Privacy Policy

_Last updated: 2026-08-31_

Alpaca is a language-learning app. This policy explains what data it handles.
The short version: **accounts are optional — Alpaca works fully without one.**

## What is stored on your device

- Learning progress, streaks, gems, coins and settings (local database)
- A random, anonymous device identifier generated on first use. It is not
  linked to you, your name, or any account.

You can erase everything at any time by clearing the app's data or uninstalling.

## What leaves your device

1. **Optional account (if you create one).** Signing up sends your email
   address, a display name and a password to
   `alpaca-token-server.vercel.app`. The password is salted and hashed with
   scrypt before storage — the plaintext is never stored. The backend stores
   your user id, email, name and password hash, and issues an opaque session
   token (30-day TTL) that the app keeps on your device. Your league score is
   then tied to this account instead of the anonymous device identifier.
2. **Anonymous league score (guests).** Without an account, completing a
   lesson sends your device identifier, the display name you chose, and the XP
   earned to `alpaca-token-server.vercel.app`, which stores the score in a
   weekly leaderboard. Scores expire after eight days.
3. **Voice roleplay (optional feature).** If you use the Speak feature, your
   microphone audio is streamed over an encrypted connection to Google's Gemini
   Live API to generate the tutor's responses, using a short-lived access token
   minted by the same backend. Alpaca's server never stores your audio; Google
   processes it under [Google's terms](https://ai.google.dev/gemini-api/terms).
   The feature is off until you open the Speak screen.
4. **Purchases.** Subscription state is managed by Google Play Billing. Alpaca
   never sees your payment details.

## What Alpaca never does

- No advertising or trackers
- No analytics SDKs
- No third-party data sales
- No account required — sign-up is entirely optional

## Children

Alpaca contains no content directed at children and is rated for general
audiences. It does not knowingly collect data from anyone, of any age.

## Contact

Questions about this policy: open an issue at
https://github.com/zyay/alpaca/issues.
