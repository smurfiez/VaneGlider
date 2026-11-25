# Agent Instructions for VaneGlider

## Project Context

The app is **VaneGlider** (`com.smurfiez.vaneglider`), a fork of itsHenry35/Kite.
The Gradle root project, CI artifacts, theme, env vars, thread names and the
receiver-visible identity (`VaneGlider1,1` model, `X-Apple-Client-Name`) all say
**VaneGlider**; the only remaining `Kite` references are the upstream
attribution and the two persisted storage keys deliberately left alone
(`kite_audio` SharedPreferences, `kite_pairings` DataStore). It is an Android
AirPlay **sender** — mirrors the phone's screen and plays its audio on receivers
(Macs, Apple TVs, open receivers like UxPlay) — not a receiver.

**Stack**:

- Kotlin 2.3.21 / AGP 9.2.1 / Gradle 9.6.1, JDK 17 toolchain, `compileSdk 37`,
  `targetSdk 36`, `minSdk 29` (a functional floor — see Constraints)
- Jetpack Compose (Material 3), DataStore, JmDNS, Bouncy Castle
- Modules: `airplay` (protocol, pure Kotlin/JVM-testable), `capture`
  (MediaProjection → H.264, AudioRecord → AAC/ALAC), `app` (UI + service)

**Protocol work**: the modern (HAP-paired) sender flow is a Kotlin port of
[doubletake](https://github.com/omarroth/doubletake) (Go); the legacy path
follows [UxPlay](https://github.com/FDH2/UxPlay) / pyatv. Ported files cite the
Go function they mirror — keep it that way.

## Repository Structure

```
/
├── airplay/                      # Protocol module — no Android UI dependencies
│   └── src/main/java/com/smurfiez/vaneglider/airplay/
│       ├── crypto/               # ChaCha20-Poly1305 (both layouts), HKDF-SHA512, SRP-6a
│       ├── discovery/            # JmDNS + BLE beacon discovery, feature flags
│       ├── pairing/              # HAP pair-setup / pair-verify, TLV8, legacy pairing
│       ├── plist/                # Binary plist reader + writer
│       ├── rtsp/                 # RTSP client, digest auth, encrypted channel
│       └── session/              # Session orchestration + stream layers
│                                 #   MirrorSession / ModernSessionSetup / SetupRequests
│                                 #   MirrorStream (video) / AudioStream (legacy)
│                                 #   ModernAudioStream / AirPlay2Session / RaopSession
│                                 #   Fairplay*.kt / FpsapSession.kt (SAP v3 port)
├── capture/                      # MediaProjection → H.264; AudioRecord → AAC/ALAC
├── app/                          # Compose UI, MirrorService (FGS), settings, tile, logs
└── images/                       # README screenshots
```

## Build & Test Commands

```sh
./gradlew :app:assembleDebug      # debug APK (local debug keystore)
./gradlew test detekt lintDebug :app:assembleDebug   # exactly what PR checks gate on
./gradlew detekt --auto-correct   # formatter; commit its output or CI fails "uncommitted changes"
./gradlew :airplay:testDebugUnitTest --tests "com.smurfiez.vaneglider.airplay.crypto.*"
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

- Unit tests are plain JVM tests under each module's `src/test` — no device,
  no emulator. The `airplay` and `capture` suites are the fast regression net.
- `:app:assembleCanary` is the release-key-signed, debuggable, unminified
  variant CI ships on every `main` push. It needs `VANE_KEYSTORE_PATH` plus
  `VANE_KEYSTORE_PASSWORD` / `VANE_KEY_ALIAS` / `VANE_KEY_PASSWORD`; without
  them the release config stays unsigned. `VANE_VERSION_SUFFIX` appends e.g.
  `-canary.42-ab12cd3`.
- PR titles must be Conventional Commits (`perf(stream): …`); CI rejects
  anything else. Version bumps live in `app/build.gradle.kts` `appVersionName`
  — a release tag must match it.

## Key Development Rules

### 1. Every cast takes one of five chains — know which one you are touching

Routing lives in `MirrorService.handleStart` / `runAudioSession`; sessions live
in the `airplay` module. Chain selection is explicit:

| Chain | Receiver | Session class | Audio pipeline |
|-------|----------|---------------|----------------|
| Modern mirror | macOS, Apple TV (HAP) | `MirrorSession` + `ModernSessionSetup` | `AlacAudioEncoder` → `ModernAudioStream` |
| Legacy mirror | UxPlay, other open receivers | `MirrorSession`, wired by `MirrorService.setupLegacySession` | `AlacAudioEncoder` → `AudioStream` |
| Sound-only (HAP) | macOS | `MirrorSession` with `videoEnabled=false` | same as modern mirror |
| AirPlay 2 audio-only | HomePod-class (bufferedAudio/PTP bits) | `AirPlay2Session` | `AlacAudioEncoder` |
| RAOP audio-only | classic AirPlay 1 receivers | `RaopSession` | `AudioPcmSource` (L16) |

Phase-by-phase sequence diagrams: CONTRIBUTING.md §Casting architecture.

### 2. The modern chain order is protocol-significant

For HAP receivers the order is **pair-verify → fp-setup → SETUPs**: macOS
answers 403 to everything from an unpaired client, so pairing runs first and
the rest travels on the encrypted control channel. In `ModernSessionSetup` the
order is control SETUP (PTP, NTP fallback) → video SETUP (type 110, PD-mixed
`shk`/`shiv`) → audio SETUP (type 96, ALAC, random `shk`, `streamConnections`)
→ volume ×2 → RECORD unless the control response asked to skip it. Video is
optional (`videoEnabled=false` ends the session at the audio phase — that is
what sound-only uses). Do not reorder or conditionally skip phases; each
response seeds the next step (eventPort, ports, ClockID anchor, skipRecord).

### 3. Protocol bodies live in SetupRequests / ModernSetupRequests — and are pinned by tests

All RTSP plist bodies are built in `session/SetupRequests.kt` (legacy) and
`session/ModernSetupRequests.kt` (modern). Adding or changing a field means:
extend the builder + extend `SetupRequestsTest`, which parses the built bytes
back through `BinaryPlistReader` and asserts every field receivers dispatch on
(ct, spf, audioFormat bit, shk length, streamConnections shapes).

### 4. Crypto keys are established at their phase — never re-derived ad hoc

| Material | Established by | Consumed by |
|----------|----------------|-------------|
| Video channel key (modern) | HKDF-SHA512(pair-verify shared secret, `"DataStream-Salt<connectionId>"`, `"DataStream-Output-Encryption-Key"`) | `ChaChaFrameCipher` (IETF 12-byte nonce) |
| Video/audio stream keys (legacy) | FairPlay `fp-setup` → `StreamCipher` | `MirrorStream`, `AudioStream` (AES) |
| Screen-audio key (modern) | random 32-byte `shk`, published in the audio SETUP descriptor | `ModernAudioStream` — draft-agl ChaCha20-Poly1305 (`OriginalChaCha20Poly1305`): 8-byte LE nonce appended after the tag, AAD = RTP timestamp + SSRC |
| Pairing | SRP-6a / Ed25519 / X25519 (`pairing/`, `crypto/`) | HAP flows |

Crypto changes require golden vectors generated by the implementation the
receivers interoperate with — self-consistent implementations can agree with
each other and still disagree with every receiver. Regeneration recipe
(offline, uses the Go module cache):

```sh
# in a clone of doubletake (its go.sum resolves the modules from the cache)
GOFLAGS=-mod=mod GOPROXY=off go run ./tmpdir
```

### 5. Stream classes own their medium end-to-end

`MirrorStream` (video TCP channel, 128-byte headers via `MirrorPacket`,
`NaluRepacker` for avcC config), `AudioStream` (legacy RTP), `ModernAudioStream`
(modern RTP), `EventChannel`, `TimingServer`, `AudioControlServer` — each owns
its socket, cipher, heartbeat/retransmit handling and close semantics. The
service only pumps encoded frames in; new stream behavior belongs in the stream
class, not in `MirrorService`.

Load-bearing details in the modern audio stream (read the class comment before
touching): PT 0x60, SSRC=0, 352-sample RTP step, each frame replayed as
`[N-2, N-1, N]` byte-identically, TimeAnnounce (PT 0xd7) at the first frame and
once per second, 0xd5/0xd6 retransmit responses.

### 6. The video send path is single-writer, and ABR reads one signal

This is the hot path; most past stutter bugs lived here.

- **One writer.** `MirrorService` gives video and audio their own dispatchers
  (`newSendDispatcher`: `vane-video-send` at `THREAD_PRIORITY_DISPLAY`,
  `vane-audio-send` at `URGENT_AUDIO`). Keep send work on them — routing a
  frame back onto `Dispatchers.Default/IO` reintroduces the interleaving and
  scheduling jitter these threads exist to remove. Niceness is applied **inside
  the thread body** (a raised nice leaks when the coroutine resumes elsewhere).
- **Single write, serialised.** Every packet leaves as one
  `[128-byte header | payload]` write under `MirrorStream.writeLock`, so a
  heartbeat can never land between a header and its payload. Feedback
  heartbeats use `tryLock()` and drop rather than stall on a dead receiver.
  Frame ordering and cipher counters still belong to the one video loop.
- **`AdaptiveBitRate` cuts on socket wait only** — `blockedNanos` from
  `MirrorStream.drainPipelineStatistics()` (lock-free, 500 ms windows). Repack
  / crypto / encode time must stay out of that signal: CPU load then looks like
  congestion and false-cuts a healthy link. Put CPU timings in the `[TX]` log
  instead. Any dropped frame cuts immediately; the ceiling comes from
  `VideoFormat` so rotation re-derives it.
- **Drops need an IDR.** A drop means a referenced P-frame is gone, so ask
  `ScreenEncoder.requestKeyFrame()`; a watchdog pauses slices when no IDR
  arrives within a second.

### 7. Media capture

- Video (`ScreenEncoder`): VBR with the explicit `"max-bitrate"` string key
  (`KEY_MAX_BITRATE` does not exist in the SDK). Configuration walks a rung
  ladder — `High@5.2` → `High@4.1` → `Baseline@5.2` → `Baseline@4.1`, each with
  and without the optional low-latency keys — so a vendor encoder that rejects
  cabac/low-latency still starts. `KEY_LOW_LATENCY` is API 30+ only; keyframes
  every 5 s, B-frames never.
- Rotation rebuilds the encoder and re-primes the receiver via the codec-config
  path; use `liveWidth`/`liveHeight` for that geometry, never the startup
  `format`.
- Audio: every path captures through `AudioPlaybackCaptureConfiguration` using
  the **same** MediaProjection as the video capture. The encoder is created
  only when RECORD_AUDIO is granted and a stream exists; otherwise the session
  runs video-only instead of failing.
- Legacy receivers get **ALAC** (ct=2, 352-sample frames) — receivers validate
  AAC-ELD frame headers against Apple-encoder first bytes and drop frames from
  other encoders.
- Bit-rate bounds span two modules: `MirrorSettings.BIT_RATE_MAX_MBPS` (slider,
  200) and `VideoFormat.BIT_RATE_MAX_BPS` (encoder `require()`, 200 Mbps).
  Change one, change the other — `MirrorSettingsTest` asserts their consistency.

### 8. Discovery

- `_raop` resolve callbacks in JmDNS do not fire — audio-only capability comes
  from recording `_raop` instance names at `serviceAdded` and matching the
  `_airplay` TXT `deviceid` against them (strip `:` from both sides).
- One receiver, one entry: merge mDNS services by `host:port`; IPv4 supersedes
  IPv6 for the same deviceId. List keys are `host:port` — never `stableId` or
  `deviceid`.
- BLE entries appear only after `GET /info` enrichment succeeds **and** the
  receiver advertises screen mirroring.

### 9. Settings, strings, and style gates

- Settings are read via `settingsRepository.current()` at cast time; the model
  lives in `MirrorSettings`.
- Every user-visible string ships in all 11 locales (`values/` plus `values-de`,
  `-es`, `-fr`, `-it`, `-ja`, `-ko`, `-pt-rBR`, `-ru`, `-zh-rCN`, `-zh-rTW`).
  `values-night` / `values-v31` are resource qualifiers, not locales. Terse
  copy, strict Material 3; the language follows the system — no in-app picker.
- detekt 2.0-alpha with `autoCorrect=true` gates the build. Baseline XML
  signatures are not guessable in this version — prefer
  `@Suppress("RuleName")` at the declaration with a one-line reason.
- Kotlin's `and` / `or` / `shl` bind lower than `+` / `-` — parenthesise ported
  bit math.

## Constraints

- **`minSdk 29` is a functional floor.** Device-audio capture
  (`AudioPlaybackCapture`) requires Android 10; below 29 the app would be
  silently video-only. The library stack would allow 23 — do not lower it
  without the maintainer's explicit decision.
- Signing is environment-driven: `VANE_KEYSTORE_PATH` +
  `VANE_KEYSTORE_PASSWORD` / `VANE_KEY_ALIAS` / `VANE_KEY_PASSWORD`. Unset
  means the release/canary APK stays unsigned — that is expected locally.
  `signing.env` is gitignored; never commit keystores or passwords.
- Edit files with the dedicated edit tool (precise patches). Do not rewrite
  files with python/sed/heredoc scripts, even for bulk mechanical changes.
- Never decompile the built APK.

## Debugging on Device

Logging is narrative by design. The tags that matter:

```sh
adb logcat -s PairSetup:I PairVerify:I MirrorSession:I RtspClient:I \
  ModernSessionSetup:I ModernAudioStream:I AirPlay2Session:I MirrorStream:I
```

- `[PAIR]` / `[FAIRPLAY]` / `[HTTP]` / `[READ]` — handshake stages
- `[SETUP]` — session setup decisions (timing protocol, ports)
- `[AUDIO-TX]` — modern audio send rate
- `[TX]` — the once-per-window sender report from `MirrorSession`:
  `video=… frames=… audio=… total=… Mbps dropped=N | latency avg/max | repack+crypto | write | blocked%`.
  `blocked%` is the socket wait the ABR cuts on. Event lines from
  `MirrorService` — `dropped=… encoder queue full; requesting IDR` and
  `no IDR in 1 s` — mean the pipeline is already behind; fix the drop/IDR path
  before touching the bitrate.
- Unprivileged `adb logcat` returns only this app's entries; `--pid=<pid>`
  narrows further.
- Audio behaviour can only be verified against a real receiver (a Mac on the
  LAN or UxPlay); emulators cannot capture or render a real stream.

## Validation Before Commit

```sh
./gradlew test detekt lintDebug :app:assembleDebug
```

Manual smoke for audio/video changes: install, cast to a real receiver, and
confirm the expected `[SETUP]`/`[AUDIO-TX]`/`[TX]` lines. Audio counts as tested
only when someone heard sound. For wire-format changes, the pinning test is
mandatory — the test map is CONTRIBUTING.md §Testing.

## Trust These Instructions

Trust these instructions first. Search the codebase further only when they are
incomplete for your task, when they contradict what you observe, or when you
need implementation details they do not cover. CONTRIBUTING.md carries the
phase-by-phase protocol walkthrough, test map and file-location table.
