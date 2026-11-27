# VaneGlider

Mirror your Android screen—and the sound playing on it—to a Mac, Apple TV, or
other AirPlay receiver.

[English](README.md) | [简体中文](README.zh-CN.md)

[![Latest release](https://img.shields.io/github/release/smurfiez/VaneGlider?style=flat-square&label=latest)](https://github.com/smurfiez/VaneGlider/releases)

[Install](#install) · [Compatibility](#receiver-compatibility) · [Troubleshooting](#troubleshooting)

## Screenshots

<p align="center">
  <img src="images/home.png" width="270" alt="Receiver discovery">
  <img src="images/settings.png" width="270" alt="Settings">
</p>

## Features

- Mirror the screen with sound, without sound, or send sound only
- Find AirPlay receivers automatically, including open receivers such as UxPlay
  and Android-based projectors
- Pair once with a Mac or Apple TV, or use a receiver’s AirPlay password
- Adjust resolution, frame rate, and bit rate for the network
- Mute the phone’s media volume during casts that include audio and restore it
  afterwards, even after a crash
- Use the Quick Settings tile, notification controls, and shareable logs

## Install

### Requirements

- Android 10 or later
- A phone and AirPlay receiver on the same Wi-Fi network
- An AirPlay receiver that supports the cast mode you want

### Download

- [Download the latest signed APK](https://github.com/smurfiez/VaneGlider/releases/download/v2.1.0/VaneGlider-2.1.0.apk)
- [See all releases](https://github.com/smurfiez/VaneGlider/releases)

Open the downloaded APK. If Android asks, allow your browser or file manager to
install unknown apps, then confirm the installation.

> **Upgrading from Kite?** VaneGlider 2.0 has a new app ID and signing key, so it
> installs alongside the original Kite app rather than replacing it.

## First cast

1. Turn on AirPlay receiving on your Mac, Apple TV, or other receiver and
   connect it to the same Wi-Fi network as your phone.
2. Open VaneGlider and grant the requested permissions:
   - **Microphone** to let Android capture the audio playing on the phone. No
     microphone input is recorded.
   - **Nearby devices** on Android 12+ for Bluetooth discovery, and on Android
     13+ for local-network discovery
   - **Location** on Android 10–11 for Bluetooth discovery
   - **Notifications** on Android 13+ to show casting status and controls
3. Tap a mirroring receiver to start **Screen with sound**; if it does not
   support audio, the cast starts without sound. Tapping an audio-only receiver
   starts **Sound only**. Long-press a Mac, Apple TV, or other receiver that
   supports both mirroring and audio to choose another mode.
4. Allow Android’s screen-capture prompt. Android asks for this every
   cast—including sound-only—because screen and audio capture share the same
   permission.
5. If this is your first connection and the receiver shows a code, enter the
   four-digit code. Enter its AirPlay password too if one is set.
6. Stop casting from VaneGlider or the ongoing notification.

Pairing is remembered, but Android’s screen-capture permission must still be
granted for each new cast.

## Cast modes

| Mode | What it does |
|---|---|
| **Screen with sound** | Mirrors your screen and sends the audio playing on your phone. |
| **Screen without sound** | Mirrors your screen without sending audio. Local playback is unchanged. |
| **Sound only** | Sends phone audio without mirroring the screen. |

Tap a mirroring receiver to start **Screen with sound**. If the receiver accepts
mirroring but declines audio, the cast continues without sound. Audio-only
receivers start **Sound only** when tapped. Long-press a receiver that supports
both mirroring and audio to choose a mode; mirroring-only and audio-only
receivers have no mode menu. Receivers that support neither mode stay disabled.

Playback capture taps the audio while the phone keeps playing it, so during
**Screen with sound** and **Sound only** VaneGlider mutes the phone’s media
volume to prevent the same audio playing from both devices. It restores the
previous level when the cast ends, including after a crash.

## Receiver compatibility

AirPlay implementations vary by receiver and software version. The matrix shows
the connection paths VaneGlider supports for each receiver class; behavior can
still vary by model and firmware. A receiver may ask for its AirPlay password in
addition to—or instead of—pairing.

| Receiver | Screen with sound | Screen without sound | Sound only | Notes |
|---|:---:|:---:|:---:|---|
| **Mac** | ✓ | ✓ | ✓ | One-time PIN pairing |
| **Apple TV** | ✓ | ✓ | ✓ | One-time PIN pairing |
| **UxPlay and other open-source receivers** | ✓ | ✓ | ✓ | Legacy pairing or an AirPlay password may apply |
| **Android-based projectors and other AirPlay 1 mirroring receivers** | Varies | ✓ | Varies | Depends on the receiver’s advertised audio support |
| **HomePod and other AirPlay 2 audio-only receivers** | — | — | ✓ | Audio only |
| **Classic AirPlay (RAOP) speakers** | — | — | ✓ | Audio only; an AirPlay password may apply |

## Settings and quality

### Resolution

Choose **Native**, **1080p**, **720p**, or **540p**. Native is the sharpest and
the most demanding to encode; lower resolutions reduce bandwidth and encoder
load. The default is 1080p.

### Frame rate

Choose **24**, **30**, **60**, **90**, or **120 fps**. The default is 30 fps.
Higher frame rates make motion smoother but use more bandwidth.

### Bit rate

Leave it on **Auto** to scale the screen-video bit rate with the selected
resolution, or choose a custom value from **2–200 Mbps**. Custom mode starts at
30 Mbps. Higher values preserve more detail but require more network capacity.

While casting, the ongoing notification shows the active resolution, frame rate,
and bit rate. Use it to check what the encoder actually selected.

### Battery optimization

Doze can stall the stream for several seconds. If casting stutters while
VaneGlider is in the background or the screen is off, open **Settings → Battery
optimisation** and allow the exemption.

### Saved receivers

Pairings are stored under **Settings → Saved receivers**. Forgetting a receiver
removes its saved pairing; the next connection must pair again.

### Logs

Use **Settings → Logs** to view or share recent app logs. Share the bundle when
reporting a casting problem so the receiver model and session details are
included.

## Troubleshooting

### No receivers found

- Confirm the phone and receiver are on the same Wi-Fi network.
- Make sure AirPlay receiving is enabled on the receiver.
- On Android 13+, allow **Nearby devices** for local-network discovery.
- On Android 12+, allow **Nearby devices** for Bluetooth discovery; on Android
  10–11, allow location instead.
- Guest networks and router settings such as AP or client isolation can block
  local discovery.

### Screen capture or permissions fail

Android requires a fresh screen-capture grant for every cast. If the prompt is
dismissed or the grant expires, start the cast again and allow it. Also confirm
that microphone permission is granted if you want sound.

### No sound

- Choose **Screen with sound**, not **Screen without sound**.
- Check that microphone permission is granted; Android uses it for playback
  capture even though no microphone input is recorded.
- Raise the receiver’s volume; the phone’s media volume is muted during
  **Screen with sound** and **Sound only**.
- Some apps deliberately exclude their audio from Android playback capture, so
  they remain silent over AirPlay.

### Stutter, lag, or repeated disconnects

- Allow the battery-optimisation exemption in VaneGlider’s settings.
- Lower the resolution, frame rate, or bit rate.
- Move the phone and receiver closer to the router, or use a less congested
  Wi-Fi network.

### Pairing fails

- Enter the four-digit code shown on the receiver; an AirPlay password may also
  be required.
- If pairing is temporarily throttled after failures, turn the receiver’s
  AirPlay service off and on, or wait about 30 minutes.
- If VaneGlider says the stored pairing is no longer valid, open **Settings →
  Saved receivers**, forget the receiver, and pair again.

### The receiver ends the session

Check that the receiver is awake, connected to the network, and not busy with
another AirPlay session. Very old or limited receivers may reject a selected
cast mode.

### The cast mode is unavailable

VaneGlider disables receivers that advertise neither mirroring nor audio
support. A receiver marked **Mirroring unsupported** can still appear in the
list, but cannot receive the screen.

### Report an issue

Open [GitHub Issues](https://github.com/smurfiez/VaneGlider/issues) and include:

- the VaneGlider version
- the receiver type and model
- what you expected and what happened instead
- the log bundle from **Settings → Logs**

## Project

VaneGlider is a fork of [Kite](https://github.com/itsHenry35/Kite), with fixes
for third-party receivers, duplicate phone audio, and a new visual identity.
See the [changelog](CHANGELOG.md) for release details.

- [Contributing and technical design](CONTRIBUTING.md)
- [Report an issue](https://github.com/smurfiez/VaneGlider/issues)
- [All releases](https://github.com/smurfiez/VaneGlider/releases)

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
