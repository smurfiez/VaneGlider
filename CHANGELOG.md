# Changelog

## 2.1.0 — 2026-11-28

### Performance

- **Single-writer send path.** Each video packet leaves as one
  `[header | payload]` write under a lock, so a feedback heartbeat can never
  land between a header and its payload.
- **Native ChaCha20-Poly1305 sealer** for the modern video channel, plus
  allocation-free ALAC encoding, RTP packing and header building — the encoder
  and crypto cost per frame drops sharply.
- **Socket-wait-only adaptive bit rate.** The controller cuts on blocked write
  time alone, so CPU load no longer looks like congestion and false-cuts a
  healthy link.
- **Dedicated send threads** (`vane-video-send` at display priority,
  `vane-audio-send` at urgent-audio) with a keyframe watchdog that pauses
  slices when no IDR arrives within a second.

## 2.0.0 — 2026-11-25

VaneGlider is now its own app: new application ID (`com.smurfiez.vaneglider`),
new name, and a new signing key. Because the ID and key changed, this installs
alongside the old Kite build rather than upgrading it.

### Fixed

- **Casting to receivers that announce AirPlay and RAOP on different ports**
  (XGIMI projectors and similar Android-based receivers). The session dialled
  the `_airplay` port, where these devices expose a stub that answers every
  RTSP method with an empty `200 OK`, so `POST /fp-setup` returned zero bytes
  and the cast failed with `m2: length 0, want 142`. Discovery now adopts the
  `_raop` RTSP port for the same host when the RAOP `ft` mask claims mirroring,
  and the duplicate "Audio only" row merges into the single receiver.
- **Double audio.** Playback capture only taps the mix, so the phone kept
  playing what the receiver was also getting. The media stream is muted while
  an audio-carrying cast runs and restored on teardown; the pre-cast level is
  persisted, so a crash or force-stop still restores it on the next start.
  Muting is armed only when an audio encoder actually starts.
- Discovery's removal path now matches receivers by identity rather than by
  endpoint key, so a device that disappears cannot leave a stale row behind.

### Changed

- **New visual identity**: warm ink neutrals with one amber "on air" accent,
  a tighter type scale, compact headers on every screen, an active-receiver
  card state, and contrast-checked colour pairs in both light and dark themes.
  System dynamic colour is off so the identity survives a wallpaper change.
- **Live casting details moved into the ongoing notification** (resolution,
  frame rate, bit rate, expandable). The in-app panel now appears only while
  connecting, pairing, or after a failure.
- Renamed to VaneGlider across the app, all 11 locale string files, the in-app
  GitHub link, README and docs. Kotlin sources moved to
  `com.smurfiez.vaneglider`.
- Settings and Saved receivers use the same 16dp rhythm and 20dp/14dp corner
  set as the main screen.
