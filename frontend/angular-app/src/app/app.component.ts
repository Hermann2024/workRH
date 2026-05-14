import { CommonModule } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './auth.service';
import { ToastContainerComponent } from './components/toast-container.component';
import { I18nService } from './i18n/i18n.service';
import type { LocaleId } from './i18n/locale.types';
import { SUPPORTED_LOCALES } from './i18n/locale.types';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive, ToastContainerComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  readonly i18n = inject(I18nService);

  readonly supportedLocales = SUPPORTED_LOCALES;

  readonly session = this.authService.session;
  readonly isAuthenticated = this.authService.isAuthenticated;

  readonly shellSummary = computed(() => {
    this.i18n.locale();
    const t = (k: string) => this.i18n.translate(k);
    if (this.isAuthenticated()) {
      const currentSession = this.session();
      return {
        badge: t('shell.badge_auth'),
        title: t('shell.title_work'),
        statusLabel: currentSession?.email ?? t('shell.status_session')
      };
    }

    return {
      badge: t('shell.badge_public'),
      title: t('shell.title_rh'),
      statusLabel: t('shell.status_guest')
    };
  });

  readonly navItems = computed(() => {
    this.i18n.locale();
    const t = (k: string) => this.i18n.translate(k);
    const items: { label: string; route: string }[] = [{ label: t('nav.offers'), route: '/pricing' }];

    if (this.isAuthenticated()) {
      const isPlatformAdmin = this.authService.hasRole('PLATFORM_ADMIN');
      if (isPlatformAdmin) {
        items.unshift({ label: t('nav.platform'), route: '/platform' });
        return items;
      }

      items.push({ label: t('nav.support'), route: '/support' });
      if (this.authService.hasRole('EMPLOYEE')) {
        items.unshift({ label: t('nav.employee'), route: '/employee' });
      }
      if (this.authService.hasRole('HR') || this.authService.hasRole('ADMIN')) {
        items.unshift({ label: t('nav.dashboard'), route: '/dashboard' });
        items.push({ label: 'Onboarding', route: '/onboarding' });
        items.push({ label: 'Trust', route: '/trust' });
        items.push({ label: t('nav.services'), route: '/services' });
        items.push({ label: t('nav.employees'), route: '/employees' });
        items.push({ label: t('nav.billing'), route: '/billing' });
        items.push({ label: t('nav.policies'), route: '/policies' });
      }
    } else {
      items.unshift({ label: t('nav.login'), route: '/login' });
      items.unshift({ label: t('nav.signup'), route: '/signup' });
    }

    return items;
  });

  setLanguage(locale: LocaleId): void {
    this.i18n.setLocale(locale);
  }

  logout(): void {
    this.authService.logout();
    this.router.navigateByUrl('/login');
  }
}
