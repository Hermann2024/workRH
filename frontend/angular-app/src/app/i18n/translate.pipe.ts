import { Pipe, PipeTransform, inject } from '@angular/core';
import { I18nService } from './i18n.service';

/**
 * Reactive translations: `pure: false` so templates refresh when `locale` changes.
 * Usage: `{{ 'login.title' | t }}` or `{{ 'support.success_ticket' | t:{ id: 1, category: 'X', status: 'Y' } }}`
 */
@Pipe({
  name: 't',
  standalone: true,
  pure: false
})
export class TranslatePipe implements PipeTransform {
  private readonly i18n = inject(I18nService);

  transform(key: string, params?: Record<string, string | number> | null): string {
    this.i18n.locale();
    return this.i18n.translate(key, params ?? undefined);
  }
}
