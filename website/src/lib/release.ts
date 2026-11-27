export interface ReleaseInfo {
  tagName: string;
  version: string;
  apkUrl: string;
  apkName: string;
  apkSizeMb: string | null;
  releasePageUrl: string;
  publishedAt: string | null;
  isFallback: boolean;
}

const REPO = 'smurfiez/VaneGlider';
const API_URL = `https://api.github.com/repos/${REPO}/releases/latest`;
const FALLBACK_URL = `https://github.com/${REPO}/releases/latest`;

const STATIC_FALLBACK: ReleaseInfo = {
  tagName: 'v2.1.0',
  version: '2.1.0',
  apkUrl: FALLBACK_URL,
  apkName: 'VaneGlider-v2.1.0.apk',
  apkSizeMb: null,
  releasePageUrl: `https://github.com/${REPO}/releases`,
  publishedAt: '2026-11-25',
  isFallback: true,
};

interface GitHubAsset {
  name: string;
  browser_download_url: string;
  size: number;
}

interface GitHubReleaseResponse {
  tag_name: string;
  html_url: string;
  published_at?: string;
  assets?: GitHubAsset[];
}

let cachedRelease: Promise<ReleaseInfo> | null = null;

export function getLatestRelease(): Promise<ReleaseInfo> {
  if (!cachedRelease) {
    cachedRelease = fetchLatestRelease();
  }
  return cachedRelease;
}

async function fetchLatestRelease(): Promise<ReleaseInfo> {
  // Optional opt-out for strictly offline local builds
  if (process.env.OFFLINE_BUILD === '1') {
    return STATIC_FALLBACK;
  }

  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), 5000);

  try {
    const headers: Record<string, string> = {
      Accept: 'application/vnd.github+json',
      'User-Agent': 'VaneGlider-Website-Build',
    };
    const token = process.env.GITHUB_TOKEN;
    if (token) {
      headers.Authorization = `Bearer ${token}`;
    }

    const res = await fetch(API_URL, { headers, signal: controller.signal });
    if (!res.ok) {
      return STATIC_FALLBACK;
    }

    const data = (await res.json()) as GitHubReleaseResponse;
    const tagName = data.tag_name || STATIC_FALLBACK.tagName;
    const version = tagName.replace(/^v/i, '');
    const apkAsset =
      data.assets?.find(
        (a) => a.name.toLowerCase().endsWith('.apk') && !a.name.toLowerCase().includes('unsigned'),
      ) ?? data.assets?.find((a) => a.name.toLowerCase().endsWith('.apk'));

    return {
      tagName,
      version,
      apkUrl: apkAsset?.browser_download_url ?? data.html_url ?? FALLBACK_URL,
      apkName: apkAsset?.name ?? `VaneGlider-${tagName}.apk`,
      apkSizeMb: apkAsset ? `${(apkAsset.size / (1024 * 1024)).toFixed(1)} MB` : null,
      releasePageUrl: data.html_url || `https://github.com/${REPO}/releases`,
      publishedAt: data.published_at ? data.published_at.slice(0, 10) : null,
      isFallback: !apkAsset,
    };
  } catch {
    return STATIC_FALLBACK;
  } finally {
    clearTimeout(timer);
  }
}
