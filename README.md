# VaneGlider

Android AirPlay sender. Mirrors your phone's screen — and plays its sound — on
a Mac, Apple TV, or any AirPlay receiver.

A fork of [Kite](https://github.com/itsHenry35/Kite) by Henry, with fixes for
third-party receivers, phone-audio muting and a new visual identity.

[English](README.md) | [简体中文](README.zh-CN.md)

## Releases

[![GitHub Release](https://img.shields.io/github/release/smurfiez/VaneGlider?style=flat-square&label=latest)](https://github.com/smurfiez/VaneGlider/releases)

## Screenshots

<p align="center">
  <img src="images/home.png" width="270" alt="Receiver discovery">
  <img src="images/settings.png" width="270" alt="Settings">
</p>

## Features

- Mirror your screen to a Mac, Apple TV, or any AirPlay receiver
- Sound included: long-press a receiver to choose screen with sound, screen
  without sound, or sound only
- One-time pairing on Mac and Apple TV — enter the PIN shown on screen once,
  then every connection just works
- Adjustable resolution, frame rate and bit rate (up to 200 Mbps)
- Finds receivers on your Wi-Fi automatically, including open receivers like
  UxPlay and Android-based projectors
- Casting details (resolution, frame rate, bit rate) live in the ongoing
  notification, so the app screen stays on the receiver list
- The phone's media volume is muted for the length of a cast and restored when
  it ends — including after a crash or a force-stop
- Quick Settings tile and one-tap log sharing

## What this fork changes

- **Third-party receivers whose AirPlay and RAOP advertisements use different
  ports.** Apple sends AirPlay 1 mirroring over the RTSP server announced by
  `_raop`, but Kite always dialled the `_airplay` port. Receivers such as XGIMI
  projectors publish a stub there that answers every RTSP method with an empty
  `200 OK`, so `POST /fp-setup` came back with zero bytes and the cast died with
  `m2: length 0, want 142`. Discovery now adopts the `_raop` RTSP port for the
  same host (gated on the RAOP `ft` mask claiming mirroring), which also folds
  the duplicate "Audio only" row into the single receiver.
- **No more double audio.** Playback capture taps the audio without owning it,
  so the phone kept playing everything the receiver was also getting. The media
  stream is now muted while an audio-carrying cast runs, and the pre-cast level
  is persisted so a killed process still restores it on the next start.
- **New identity.** Warm ink neutrals with a single amber "on air" accent
  instead of the system-generated palette, tighter typography with monospaced
  metrics, compact headers on every screen, and contrast-checked colour pairs
  in both light and dark themes.
- **Live state where it belongs.** The status panel now appears only for
  connecting, pairing and failures; while casting, the ongoing notification
  shows `864 × 1920 @ 30 fps · 1.4 Mbps` and the app screen is just the
  receiver list plus Stop.

## Performance

- **Low-latency encoding.** The encoder runs High profile (CABAC, no B-frames)
  at realtime priority, with the low-latency codec hint on Android 11+, and
  falls back rung by rung — High 5.2 → 4.1, then Baseline — when a vendor
  encoder rejects a key. Keyframes are 5 s apart and requested on demand.
- **One writer per stream.** Video and audio each own a high-priority send
  thread, and every video packet leaves as a single `[128-byte header |
  payload]` write, so a feedback heartbeat can never land inside a frame.
- **Cheap crypto.** Video is sealed with the platform `ChaCha20-Poly1305`; the
  audio sealer and the ALAC encoder reuse their buffers instead of allocating
  on every 8 ms frame.
- **Congestion control on the right signal.** The adaptive bit rate reacts only
  to time spent blocked on the socket, so CPU peaks no longer read as
  congestion. Any dropped frame cuts the bit rate and asks for a fresh
  keyframe, and nothing decodes a P-frame whose references went missing.
- **Wi-Fi kept awake and prioritised.** A low-latency Wi-Fi lock holds off
  power-save for the length of a cast, video traffic is marked DSCP AF41 and
  audio EF, and the send buffer is sized to about 125 ms of the stream so
  backpressure arrives before the lag does.

## Requirements

- Android 10 or later (minSdk 29)
- A real device on the same Wi-Fi network as the receiver
- An AirPlay receiver

## Building

```sh
./gradlew :app:assembleDebug
```

The Android SDK is located through `ANDROID_HOME` or `local.properties`.

## Releasing

The version lives in `app/build.gradle.kts` (`appVersionName` + `versionCode`);
a release tag must match it. The release build is signed from environment
variables, so the key never enters the repo:

```sh
. ./signing.env          # VANE_KEYSTORE_PATH / VANE_KEYSTORE_PASSWORD /
                         # VANE_KEY_ALIAS / VANE_KEY_PASSWORD (gitignored)
./gradlew :app:assembleRelease
gh release create v2.0.0 app/build/outputs/apk/release/app-release.apk \
  --title "VaneGlider 2.0.0" --notes-file CHANGELOG.md
```

Without those variables `assembleRelease` still works, it just leaves the APK
unsigned. Keep the keystore (`~/.android/vaneglider-release.jks`) backed up: the
same key is required for every future update, and losing it means users have to
uninstall to upgrade.

## Acknowledgements

VaneGlider builds on years of open-source AirPlay work, and on Kite itself.
Thanks to:

- [Kite](https://github.com/itsHenry35/Kite) — the upstream Android AirPlay
  sender this fork is based on
- [doubletake](https://github.com/omarroth/doubletake) — Go AirPlay sender;
  parts of the protocol layer are Kotlin ports of it
- [UxPlay](https://github.com/FDH2/UxPlay) — open-source AirPlay receiver; a
  key reference during development
- [pyatv](https://github.com/postlund/pyatv) — AirPlay 2 protocol reference
- [shairport-sync](https://github.com/mikebrady/shairport-sync) — pairing and
  timing reference
- [openairplay](https://github.com/openairplay/airplay-spec) — early protocol
  documentation

## License

VaneGlider is released under the GNU LGPL-3.0 — see [LICENSE](LICENSE). Ported
files carry doubletake's license. Open-source attributions are also listed in
the app under Settings → About.
