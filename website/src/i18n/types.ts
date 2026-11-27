export type Locale = 'en' | 'zh-cn';

export type SupportBadge = 'yes' | 'varies' | 'no';

export interface ReceiverRow {
  id: string;
  name: string;
  category: 'video' | 'audio';
  screenSound: SupportBadge;
  screenOnly: SupportBadge;
  soundOnly: SupportBadge;
  note: string;
}

export interface TranslationSchema {
  meta: {
    title: string;
    description: string;
    ogLocale: string;
    switchLabel: string;
    switchAriaLabel: string;
  };
  nav: {
    features: string;
    devices: string;
    howItWorks: string;
    help: string;
    downloadCta: string;
    themeToggleLabel: string;
    menuOpenLabel: string;
    menuCloseLabel: string;
  };
  hero: {
    pillPrefix: string;
    pillSuffix: string;
    headline: string;
    subhead: string;
    primaryCta: string;
    secondaryCta: string;
    metaLine: string;
    previewLabel: string;
    previewLivePrefix: string;
    previewAudioLockedNote: string;
    sampleReceivers: {
      id: string;
      name: string;
      model: string;
      type: 'mac' | 'tv' | 'speaker';
      paired: boolean;
      audioOnly: boolean;
    }[];
    modes: {
      id: 'mirror' | 'video' | 'audio';
      label: string;
    }[];
  };
  benefits: {
    eyebrow: string;
    heading: string;
    items: { title: string; body: string; icon: string }[];
  };
  castModes: {
    eyebrow: string;
    heading: string;
    subtitle: string;
    tabs: {
      id: 'mirror' | 'video' | 'audio';
      title: string;
      summary: string;
      bestFor: string;
      triggerHint: string;
      phoneAudioBadge: string;
    }[];
  };
  compatibility: {
    eyebrow: string;
    heading: string;
    subtitle: string;
    filters: { id: 'all' | 'video' | 'audio'; label: string }[];
    columns: {
      receiver: string;
      screenSound: string;
      screenOnly: string;
      soundOnly: string;
      notes: string;
    };
    badges: {
      yes: string;
      varies: string;
      no: string;
    };
    rows: ReceiverRow[];
  };
  gettingStarted: {
    eyebrow: string;
    heading: string;
    steps: { step: string; title: string; body: string }[];
    permissionsTitle: string;
    permissionsSubtitle: string;
    permissions: { name: string; scope: string; reason: string }[];
  };
  quality: {
    eyebrow: string;
    heading: string;
    subtitle: string;
    resolutionLabel: string;
    fpsLabel: string;
    bitRateLabel: string;
    bitRateAuto: string;
    bitRateCustom: string;
    resolutions: { value: 'native' | '1080p' | '720p' | '540p'; label: string; hint: string }[];
    fpsOptions: number[];
    liveEstimateLabel: string;
    autoAdaptNote: string;
    manualRangeNote: string;
    systemTips: { title: string; status?: string; body: string }[];
  };
  faq: {
    eyebrow: string;
    heading: string;
    searchPlaceholder: string;
    emptyResult: string;
    items: { id: string; q: string; a: string }[];
  };
  download: {
    eyebrow: string;
    heading: string;
    body: string;
    primaryCta: string;
    allReleases: string;
    kiteTitle: string;
    kiteBody: string;
    whatsNewTitle: string;
    whatsNewItems: string[];
  };
  footer: {
    tagline: string;
    projectHeading: string;
    languagesHeading: string;
    creditsHeading: string;
    links: {
      github: string;
      releases: string;
      changelog: string;
      contributing: string;
      issues: string;
    };
    languagesList: string;
    creditsNote: string;
    licensesLine: string;
  };
}
