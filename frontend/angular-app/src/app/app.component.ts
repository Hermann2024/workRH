import { CommonModule } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './auth.service';
import { ToastContainerComponent } from './components/toast-container.component';

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

  readonly session = this.authService.session;
  readonly isAuthenticated = this.authService.isAuthenticated;
  readonly shellSummary = computed(() => {
    if (this.isAuthenticated()) {
      const currentSession = this.session();
      const primaryRole = currentSession?.roles[0] ?? 'EMPLOYEE';
      return {
        badge: 'Workspace',
        title: 'Pilotage RH frontalier',
        description: `Tenant ${currentSession?.tenantId} connecte avec le role ${primaryRole}.`,
        highlights: ['Dashboard unifie', 'Conformite Luxembourg', 'Billing SaaS'],
        statusLabel: currentSession?.email ?? 'Session active'
      };
    }

    return {
      badge: 'WorkRH Platform',
      title: 'RH, teletravail et abonnement dans le meme cockpit',
      description: 'Une interface unique pour les operations RH, la conformite frontaliere et la monetisation SaaS.',
      highlights: ['Login demo', 'Pricing clair', 'Modules securises'],
      statusLabel: 'Session non connectee'
    };
  });
  readonly navItems = computed(() => {
    const items = [
      { label: 'Pricing', route: '/pricing' }
    ];

    if (this.isAuthenticated()) {
      items.unshift({ label: 'Dashboard', route: '/dashboard' });
      if (this.authService.hasRole('HR') || this.authService.hasRole('ADMIN')) {
        items.push({ label: 'Billing', route: '/billing' });
      }
      if (this.authService.hasRole('HR') || this.authService.hasRole('ADMIN')) {
        items.push({ label: 'Policies', route: '/policies' });
      }
    } else {
      items.unshift({ label: 'Login', route: '/login' });
    }

    return items;
  });

  logout(): void {
    this.authService.logout();
    this.router.navigateByUrl('/login');
  }
}
