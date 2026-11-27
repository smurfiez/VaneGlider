import { en } from './en';
import { zhCn } from './zh-cn';
import type { Locale, TranslationSchema } from './types';

export const DICTIONARIES: Record<Locale, TranslationSchema> = {
  en,
  'zh-cn': zhCn,
};

export function getDictionary(locale: Locale): TranslationSchema {
  return DICTIONARIES[locale] ?? DICTIONARIES.en;
}

export function withBase(path: string): string {
  const base = import.meta.env.BASE_URL.replace(/\/+$/, '');
  const clean = path.startsWith('/') ? path : `/${path}`;
  return `${base}${clean}` || '/';
}

export function localeHref(target: Locale): string {
  return target === 'en' ? withBase('/') : withBase('/zh-cn/');
}
