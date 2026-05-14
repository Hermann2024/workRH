import { Injectable, computed, signal } from '@angular/core';
import type { LocaleId } from './locale.types';
import { MESSAGES } from './messages';

const STORAGE_KEY = 'workrh_locale';

function browserLocale(): LocaleId {
  if (typeof navigator === 'undefined') {
    return 'fr';
  }
  const raw = (navigator.languages?.[0] ?? navigator.language ?? 'fr').toLowerCase();
  if (raw.startsWith('lb')) {
    return 'lb';
  }
  if (raw.startsWith('en')) {
    return 'en';
  }
  return 'fr';
}

function parseStoredLocale(value: string | null): LocaleId | null {
  if (value === 'fr' || value === 'en' || value === 'lb') {
    return value;
  }
  return null;
}

@Injectable({ providedIn: 'root' })
export class I18nService {
  private readonly localeState = signal<LocaleId>(this.readInitialLocale());

  readonly locale = computed(() => this.localeState());

  constructor() {
    if (typeof document !== 'undefined') {
      document.documentElement.lang = this.localeState();
    }
  }

  translate(key: string, params?: Record<string, string | number>): string {
    const loc = this.localeState();
    const table = MESSAGES[loc] ?? MESSAGES.fr;
    let text = table[key] ?? MESSAGES.fr[key] ?? key;
    if (params) {
      for (const [name, value] of Object.entries(params)) {
        text = text.split(`{{${name}}}`).join(String(value));
      }
    }
    return text;
  }

  /** Short alias for templates */
  t(key: string, params?: Record<string, string | number>): string {
    return this.translate(key, params);
  }

  setLocale(locale: LocaleId): void {
    this.localeState.set(locale);
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem(STORAGE_KEY, locale);
    }
    if (typeof document !== 'undefined') {
      document.documentElement.lang = locale;
    }
  }

  private readInitialLocale(): LocaleId {
    if (typeof localStorage !== 'undefined') {
      const stored = parseStoredLocale(localStorage.getItem(STORAGE_KEY));
      if (stored) {
        return stored;
      }
    }
    return browserLocale();
  }
}
