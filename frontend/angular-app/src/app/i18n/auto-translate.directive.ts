import {
  AfterViewInit,
  DestroyRef,
  Directive,
  ElementRef,
  effect,
  inject
} from '@angular/core';
import { I18nService } from './i18n.service';
import { MESSAGES } from './messages';
import type { LocaleId } from './locale.types';

const TRANSLATED_ATTRS = ['placeholder', 'title', 'aria-label'];
const SKIPPED_TAGS = new Set(['SCRIPT', 'STYLE', 'TEXTAREA']);

function normalizeLiteral(value: string): string {
  return value.replace(/\s+/g, ' ').trim();
}

function buildLiteralMap(locale: LocaleId): Map<string, string> {
  const map = new Map<string, string>();
  const source = MESSAGES.fr;
  const target = MESSAGES[locale] ?? source;

  for (const [key, frValue] of Object.entries(source)) {
    const sourceText = normalizeLiteral(frValue);
    const targetText = target[key];
    if (!sourceText || !targetText || sourceText.includes('{{')) {
      continue;
    }
    map.set(sourceText, targetText);
  }

  return map;
}

@Directive({
  selector: '[appAutoTranslate]',
  standalone: true
})
export class AutoTranslateDirective implements AfterViewInit {
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly i18n = inject(I18nService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly originalText = new WeakMap<Text, string>();
  private readonly originalAttrs = new WeakMap<Element, Map<string, string>>();
  private observer: MutationObserver | null = null;

  constructor() {
    effect(() => {
      const locale = this.i18n.locale();
      this.translateTree(this.host.nativeElement, buildLiteralMap(locale));
    });
  }

  ngAfterViewInit(): void {
    this.observer = new MutationObserver(() => {
      this.translateTree(this.host.nativeElement, buildLiteralMap(this.i18n.locale()));
    });
    this.observer.observe(this.host.nativeElement, {
      childList: true,
      subtree: true,
      characterData: true
    });
    this.destroyRef.onDestroy(() => this.observer?.disconnect());
  }

  private translateTree(root: Node, literals: Map<string, string>): void {
    this.translateNode(root, literals);
    const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT | NodeFilter.SHOW_ELEMENT);
    let current = walker.nextNode();
    while (current) {
      this.translateNode(current, literals);
      current = walker.nextNode();
    }
  }

  private translateNode(node: Node, literals: Map<string, string>): void {
    if (node.nodeType === Node.TEXT_NODE) {
      this.translateTextNode(node as Text, literals);
      return;
    }

    if (node.nodeType === Node.ELEMENT_NODE) {
      this.translateElementAttrs(node as Element, literals);
    }
  }

  private translateTextNode(node: Text, literals: Map<string, string>): void {
    const parent = node.parentElement;
    if (!parent || SKIPPED_TAGS.has(parent.tagName)) {
      return;
    }

    const raw = node.data;
    const trimmed = normalizeLiteral(raw);
    if (!trimmed) {
      return;
    }

    if (!this.originalText.has(node)) {
      this.originalText.set(node, trimmed);
    }

    const source = this.originalText.get(node) ?? trimmed;
    const translated = literals.get(source);
    if (!translated) {
      return;
    }

    const next = raw.replace(trimmed, translated);
    if (node.data !== next) {
      node.data = next;
    }
  }

  private translateElementAttrs(element: Element, literals: Map<string, string>): void {
    if (SKIPPED_TAGS.has(element.tagName)) {
      return;
    }

    for (const attr of TRANSLATED_ATTRS) {
      const value = element.getAttribute(attr);
      if (!value) {
        continue;
      }

      let attrs = this.originalAttrs.get(element);
      if (!attrs) {
        attrs = new Map<string, string>();
        this.originalAttrs.set(element, attrs);
      }
      if (!attrs.has(attr)) {
        attrs.set(attr, normalizeLiteral(value));
      }

      const source = attrs.get(attr) ?? normalizeLiteral(value);
      const translated = literals.get(source);
      if (translated && value !== translated) {
        element.setAttribute(attr, translated);
      }
    }
  }
}
