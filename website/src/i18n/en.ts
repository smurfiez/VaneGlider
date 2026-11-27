import type { TranslationSchema } from './types';

export const en: TranslationSchema = {
  meta: {
    title: 'VaneGlider — Android AirPlay Screen Mirroring & Sound',
    description:
      'Cast your Android screen and sound to Mac, Apple TV, projectors and AirPlay speakers on your Wi-Fi — no ads, no double sound, free under LGPL-3.0.',
    ogLocale: 'en_US',
    switchLabel: '简体中文',
    switchAriaLabel: 'Switch to Simplified Chinese',
  },
  nav: {
    features: 'Features',
    devices: 'Devices',
    howItWorks: 'How it works',
    help: 'Help',
    downloadCta: 'Download APK',
    themeToggleLabel: 'Toggle light and dark theme',
    menuOpenLabel: 'Open menu',
    menuCloseLabel: 'Close menu',
  },
  hero: {
    pillPrefix: 'v',
    pillSuffix: ' · Free & Open Source · Android 10+',
    headline: 'Cast your Android screen and sound, effortlessly.',
    subhead:
      'Mirror your phone to a Mac, Apple TV, projector, or AirPlay speaker on the same Wi-Fi — one tap to start, no ads, and no double sound.',
    primaryCta: 'Download APK',
    secondaryCta: 'See compatible devices',
    metaLine: 'Signed APK · Local Wi-Fi only · GNU LGPL-3.0',
    previewLabel: 'Try the "On Air" cast bar',
    previewLivePrefix: 'Casting to',
    previewAudioLockedNote: 'Audio-only speaker — Sound only selected',
    sampleReceivers: [
      {
        id: 'mac',
        name: 'Living Room Mac',
        model: 'MacBook Pro',
        type: 'mac',
        paired: true,
        audioOnly: false,
      },
      {
        id: 'tv',
        name: 'Bedroom Apple TV',
        model: 'Apple TV 4K',
        type: 'tv',
        paired: false,
        audioOnly: false,
      },
      {
        id: 'speaker',
        name: 'Kitchen HomePod',
        model: 'Audio only',
        type: 'speaker',
        paired: false,
        audioOnly: true,
      },
    ],
    modes: [
      { id: 'mirror', label: 'Screen + sound' },
      { id: 'video', label: 'Screen only' },
      { id: 'audio', label: 'Sound only' },
    ],
  },
  benefits: {
    eyebrow: 'WHY VANEGLIDER',
    heading: 'Six everyday wins, no extra settings.',
    items: [
      {
        title: 'Screen, sound, or both',
        body: 'Tap any mirroring receiver to share your screen with sound. Long-press Macs, Apple TVs, or dual receivers to pick Screen without sound or Sound only.',
        icon: 'layers',
      },
      {
        title: 'No double sound',
        body: "VaneGlider automatically mutes your phone's media volume while casting audio so you never hear an echo — and restores your exact volume afterwards, even after a crash.",
        icon: 'mute',
      },
      {
        title: 'Finds receivers automatically',
        body: 'Opens straight to the AirPlay receivers on your Wi-Fi, including projectors and open-source receivers. Pair once with a 4-digit PIN and VaneGlider remembers it.',
        icon: 'wifi',
      },
      {
        title: 'Tuned to your screen & Wi-Fi',
        body: 'Pick up to Native resolution and 120 fps, or leave Bit rate on Auto so VaneGlider adjusts smoothly when Wi-Fi gets busy.',
        icon: 'gauge',
      },
      {
        title: 'Built for daily flow',
        body: 'Start from the Quick Settings tile, monitor resolution and speed in the live notification with a one-tap Stop button, and use the app in 11 languages.',
        icon: 'bolt',
      },
      {
        title: 'Private by design',
        body: 'Everything stays on your local Wi-Fi network. No account, no cloud relay, no ads, and no analytics — released freely under GNU LGPL-3.0.',
        icon: 'shield',
      },
    ],
  },
  castModes: {
    eyebrow: 'THREE WAYS TO CAST',
    heading: 'Share the whole screen, keep it silent, or stream just the music.',
    subtitle: 'What would you like to cast?',
    tabs: [
      {
        id: 'mirror',
        title: 'Screen + sound',
        summary: 'Mirrors your screen and sends the audio playing on your phone.',
        bestFor:
          'Best for movies, YouTube, mobile games, and video calls on a TV or Mac. Your phone automatically mutes its speaker and restores volume when you stop. If a receiver declines audio, VaneGlider smoothly falls back to silent mirroring.',
        triggerHint: 'Default on tap for mirroring receivers',
        phoneAudioBadge: 'Phone speaker muted',
      },
      {
        id: 'video',
        title: 'Screen only',
        summary: 'Mirrors your screen without sending audio.',
        bestFor:
          "Your phone's local playback and volume stay completely untouched — ideal for silent app demos, slides in a meeting room, or keeping music in your earbuds while sharing your screen.",
        triggerHint: 'Long-press a Mac or Apple TV to pick',
        phoneAudioBadge: 'Phone audio untouched',
      },
      {
        id: 'audio',
        title: 'Sound only',
        summary: 'Sends phone audio without mirroring the screen.',
        bestFor:
          'Turns HomePods, AirPlay speakers, or even your Mac into wireless speakers for any Android app. Phone media volume mutes automatically so audio only plays on your speaker.',
        triggerHint: 'Default on tap for speakers · Long-press Macs & Apple TVs',
        phoneAudioBadge: 'Plays on the speaker only',
      },
    ],
  },
  compatibility: {
    eyebrow: 'DEVICE COMPATIBILITY',
    heading: 'Made for Apple receivers, open-source tools, and projectors.',
    subtitle: 'Six receiver classes, three cast modes — check what works before you tap.',
    filters: [
      { id: 'all', label: 'All' },
      { id: 'video', label: 'Screen & Sound' },
      { id: 'audio', label: 'Audio Only' },
    ],
    columns: {
      receiver: 'Receiver',
      screenSound: 'Screen + Sound',
      screenOnly: 'Screen Only',
      soundOnly: 'Sound Only',
      notes: 'Notes',
    },
    badges: {
      yes: 'Yes',
      varies: 'Varies',
      no: 'No',
    },
    rows: [
      {
        id: 'mac',
        name: 'Mac',
        category: 'video',
        screenSound: 'yes',
        screenOnly: 'yes',
        soundOnly: 'yes',
        note: 'One-time 4-digit PIN pairing',
      },
      {
        id: 'apple-tv',
        name: 'Apple TV',
        category: 'video',
        screenSound: 'yes',
        screenOnly: 'yes',
        soundOnly: 'yes',
        note: 'One-time 4-digit PIN pairing',
      },
      {
        id: 'uxplay',
        name: 'UxPlay & open-source receivers',
        category: 'video',
        screenSound: 'yes',
        screenOnly: 'yes',
        soundOnly: 'yes',
        note: 'Legacy pairing or an AirPlay password may apply',
      },
      {
        id: 'projector',
        name: 'Projectors & AirPlay 1 mirroring receivers (e.g., XGIMI)',
        category: 'video',
        screenSound: 'varies',
        screenOnly: 'yes',
        soundOnly: 'varies',
        note: "Depends on receiver's audio support; v2.0 fixed multi-port projectors",
      },
      {
        id: 'homepod',
        name: 'HomePod & AirPlay 2 speakers',
        category: 'audio',
        screenSound: 'no',
        screenOnly: 'no',
        soundOnly: 'yes',
        note: 'Audio only',
      },
      {
        id: 'raop',
        name: 'Classic AirPlay (RAOP) speakers',
        category: 'audio',
        screenSound: 'no',
        screenOnly: 'no',
        soundOnly: 'yes',
        note: 'Audio only; AirPlay password may apply',
      },
    ],
  },
  gettingStarted: {
    eyebrow: 'GETTING STARTED',
    heading: 'Up and running in under a minute.',
    steps: [
      {
        step: '01',
        title: 'Install & connect to the same Wi-Fi',
        body: 'Turn on AirPlay receiving on your Mac, Apple TV, projector, or speaker, and make sure your phone is on the same Wi-Fi network. Install the signed VaneGlider APK (allow installing from your browser/files app if prompted).',
      },
      {
        step: '02',
        title: 'Tap a receiver & allow screen capture',
        body: "Tap a device to start casting (or long-press a Mac/Apple TV to choose a mode). Allow Android's screen-capture prompt — Android asks for this on every cast, including Sound only, because video and audio capture share the same system prompt.",
      },
      {
        step: '03',
        title: 'Enter a 4-digit PIN once — then control from anywhere',
        body: 'On your first connection to a Mac or Apple TV, type the 4-digit code shown on its screen (or its AirPlay password if set). Pairing is remembered next time. Stop anytime from the app, the live notification, or the Quick Settings tile.',
      },
    ],
    permissionsTitle: 'Why VaneGlider asks for permissions',
    permissionsSubtitle: 'Plain-English notes on every Android permission the app can request.',
    permissions: [
      {
        name: 'Microphone',
        scope: 'All (Android 10+)',
        reason:
          'Android uses this permission to let apps capture sound playing inside other apps. VaneGlider never records your microphone.',
      },
      {
        name: 'Nearby devices',
        scope: 'Android 12+ / 13+',
        reason:
          'Used on Android 12+ for Bluetooth discovery and Android 13+ to find receivers on your local Wi-Fi.',
      },
      {
        name: 'Location',
        scope: 'Android 10–11 only',
        reason:
          'Required by older Android versions strictly for Bluetooth discovery. Never stored or shared.',
      },
      {
        name: 'Notifications',
        scope: 'Android 13+',
        reason:
          'Shows live casting resolution, frame rate, bit rate, and a one-tap Stop button.',
      },
      {
        name: 'Screen capture',
        scope: 'Every new cast',
        reason:
          'Required by Android each time you cast — even for Sound only — because screen and app audio share one system grant.',
      },
    ],
  },
  quality: {
    eyebrow: 'QUALITY MADE SIMPLE',
    heading: 'Quality made simple.',
    subtitle:
      'Match the cast to your Wi-Fi — or leave Auto on and let VaneGlider adapt.',
    resolutionLabel: 'Resolution',
    fpsLabel: 'Frame rate',
    bitRateLabel: 'Bit rate',
    bitRateAuto: 'Auto',
    bitRateCustom: 'Custom',
    resolutions: [
      {
        value: 'native',
        label: 'Native',
        hint: 'Crisp pixel-for-pixel match to your phone display; best on fast 5 GHz/6 GHz Wi-Fi.',
      },
      {
        value: '1080p',
        label: '1080p',
        hint: 'Best balance of sharp text and smooth performance for everyday mirroring.',
      },
      {
        value: '720p',
        label: '720p',
        hint: 'Great for older phones, crowded Wi-Fi, or long battery life.',
      },
      {
        value: '540p',
        label: '540p',
        hint: 'Ultra-lightweight for weak or congested routers.',
      },
    ],
    fpsOptions: [24, 30, 60, 90, 120],
    liveEstimateLabel: 'Estimated bit rate',
    autoAdaptNote: 'Adapts down smoothly if Wi-Fi gets busy',
    manualRangeNote: 'Manual range is 2–200 Mbps; the slider starts at 30 Mbps.',
    systemTips: [
      {
        title: 'Battery optimisation',
        status: 'Exempt',
        body: 'Doze can stall the stream for several seconds when the screen turns off or VaneGlider runs in the background. Tap Settings → Battery optimisation to set it to Exempt.',
      },
      {
        title: 'Saved receivers',
        body: 'View or forget paired devices anytime under Settings → Saved receivers.',
      },
      {
        title: 'Logs',
        body: 'Share a clean session log from Settings → Logs if you ever need help debugging a receiver.',
      },
    ],
  },
  faq: {
    eyebrow: 'HELP & FAQ',
    heading: 'Questions? Quick answers.',
    searchPlaceholder: 'Search questions and answers',
    emptyResult:
      'No match yet — try another keyword, or open an issue on GitHub.',
    items: [
      {
        id: 'no-receivers',
        q: 'No receivers show up on my list',
        a: 'Confirm both phone and receiver are on the same Wi-Fi network (not a guest network) and AirPlay receiving is enabled. • Allow Nearby devices (Android 13+ for local Wi-Fi, Android 12+ for Bluetooth) or Location (Android 10–11). • Check that your router does not have AP isolation / Client isolation enabled, which blocks local device discovery.',
      },
      {
        id: 'no-sound',
        q: 'There is no sound on my Mac, TV, or speaker',
        a: "Make sure you started Screen with sound (not Screen without sound). • Confirm Microphone permission is granted (Android uses it to allow app playback capture; your mic is never recorded). • Turn up the receiver's volume — your phone's media volume is intentionally muted during sound-carrying casts. • Note: A few apps (such as certain DRM streaming or banking apps) explicitly opt out of Android playback capture and will remain silent over AirPlay.",
      },
      {
        id: 'lag',
        q: 'Video feels laggy, stutters, or disconnects',
        a: "Open Settings → Battery optimisation inside VaneGlider and allow the Exempt setting so Android Doze doesn't pause the stream when your screen locks or you switch apps. • Keep Bit rate on Auto (or lower Resolution / Frame rate in Settings) and move closer to your Wi-Fi router.",
      },
      {
        id: 'pairing',
        q: "Pairing code isn't working or stopped connecting",
        a: 'Enter the 4-digit code shown on your Mac or Apple TV (and the AirPlay password if one is configured). • If repeated wrong attempts trigger a temporary throttle, toggle AirPlay receiving off and back on (or wait ~30 minutes) and try again. • If VaneGlider reports the stored pairing is no longer valid, open Settings → Saved receivers, forget the device, and pair fresh.',
      },
      {
        id: 'sound-only-permission',
        q: 'Why does Android ask for screen capture even for "Sound only"?',
        a: 'Android groups screen recording and system-audio playback capture under the same MediaProjection permission prompt, and requires fresh confirmation every time a new cast starts.',
      },
      {
        id: 'session-ended',
        q: 'The receiver ended the session, or a cast mode is greyed out',
        a: 'Make sure the receiver is awake and not already busy with another AirPlay stream. Receivers that advertise neither screen mirroring nor audio stay disabled; devices marked Mirroring unsupported only accept sound.',
      },
      {
        id: 'kite-upgrade',
        q: 'Upgrading from Kite — why do I see two apps?',
        a: "VaneGlider 2.0 has its own package ID (com.smurfiez.vaneglider) and signing key, so it installs cleanly alongside the original Kite app instead of overwriting it. Once you're happy with VaneGlider, you can uninstall the older Kite build.",
      },
      {
        id: 'report',
        q: 'Still stuck? How to report an issue',
        a: 'Open an issue on GitHub Issues (https://github.com/smurfiez/VaneGlider/issues) and attach the log bundle from Settings → Logs, along with your VaneGlider version and receiver model.',
      },
    ],
  },
  download: {
    eyebrow: 'DOWNLOAD',
    heading: 'Ready when you are.',
    body: 'Grab the signed APK, install it on Android 10 or newer, and cast to the nearest AirPlay receiver.',
    primaryCta: 'Download APK',
    allReleases: 'All releases',
    kiteTitle: 'Coming from Kite?',
    kiteBody:
      'VaneGlider 2.0 uses its own package ID (com.smurfiez.vaneglider) and signing key, so it installs beside the original Kite app instead of replacing it. Uninstall the old build once you are happy.',
    whatsNewTitle: "What's new in 2.1.0",
    whatsNewItems: [
      'Rewritten send path: one serialised write per video packet, so heartbeats can no longer land mid-frame.',
      'Native ChaCha20-Poly1305 sealer plus allocation-free ALAC encoding and RTP packing — far less CPU per cast.',
      'Adaptive bit rate now reads socket back-pressure only, so a busy CPU no longer cuts a healthy link.',
      'Dedicated high-priority send threads and a keyframe watchdog recover dropped frames faster.',
    ],
  },
  footer: {
    tagline:
      'VaneGlider — Open-source Android AirPlay sender. GNU LGPL-3.0.',
    projectHeading: 'Project & Contributors',
    languagesHeading: 'Languages',
    creditsHeading: 'Acknowledgements',
    links: {
      github: 'GitHub Repository',
      releases: 'All Releases',
      changelog: 'Changelog',
      contributing: 'Technical Design & Contributing',
      issues: 'Report an Issue',
    },
    languagesList:
      'English, 简体中文, 繁體中文, 日本語, 한국어, Deutsch, Français, Español, Português (Brasil), Русский, Italiano',
    creditsNote:
      'Protocol work builds on Kite, doubletake, UxPlay, pyatv, shairport-sync and the AirPlay spec. Thank you to every upstream maintainer.',
    licensesLine:
      'Bundled open-source libraries: AndroidX & Jetpack Compose (Apache-2.0), Kotlin & kotlinx.coroutines (Apache-2.0), Material Design 3 (Apache-2.0), Bouncy Castle (MIT), JmDNS (Apache-2.0).',
  },
};
