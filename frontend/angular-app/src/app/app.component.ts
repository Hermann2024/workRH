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
      return {
        badge: 'Espace WorkRH',
        title: 'WorkRH',
        statusLabel: currentSession?.email ?? 'Session active'
      };
    }

    return {
      badge: 'WorkRH',
      title: 'Gestion RH',
      statusLabel: 'Non connecté'
    };
  });
  readonly navItems = computed(() => {
    const items = [
      { label: 'Offres', route: '/pricing' }
    ];

    if (this.isAuthenticated()) {
      if (this.authService.hasRole('EMPLOYEE')) {
        items.unshift({ label: 'Mon espace', route: '/employee' });
      }
      if (this.authService.hasRole('HR') || this.authService.hasRole('ADMIN')) {
        items.unshift({ label: 'Tableau de bord', route: '/dashboard' });
        items.push({ label: 'Facturation', route: '/billing' });
        items.push({ label: 'Règles', route: '/policies' });
      }
    } else {
      items.unshift({ label: 'Connexion', route: '/login' });
      items.unshift({ label: 'Inscription', route: '/signup' });
    }

    return items;
  });

  logout(): void {
    this.authService.logout();
    this.router.navigateByUrl('/login');
  }
}
