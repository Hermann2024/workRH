import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, forkJoin, map, Observable, of } from 'rxjs';
import { AuthService } from '../auth.service';
import { SHOW_DEMO_HINTS } from '../config';
import { toCommercialFeatureLabel } from '../plan-feature-labels';
import { ToastService } from '../services/toast.service';
import { CatalogReadinessResponse, PlanResponse, SubscriptionResponse, WorkRhApiService } from '../workrh-api.service';

interface PricingVm {
  plans: PlanResponse[];
  subscription: SubscriptionResponse | null;
  readiness: CatalogReadinessResponse | null;
}

@Component({
  selector: 'app-pricing-page',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './pricing-page.component.html',
  styleUrl: './page-styles.css'
})
export class PricingPageComponent {
  private readonly api = inject(WorkRhApiService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly toastService = inject(ToastService);

  readonly showDemoHints = SHOW_DEMO_HINTS;
  readonly loading = signal(true);
  readonly loadError = signal<string | null>(null);
  readonly vm = signal<PricingVm | null>(null);
  readonly visiblePlans = computed(() => this.vm()?.plans ?? []);
  readonly previewModeActive = computed(() => this.vm()?.subscription?.previewAllFeaturesActive ?? false);
  readonly readinessWarnings = computed(() => this.vm()?.readiness?.warnings ?? []);
  readonly stripeCheckoutAvailable = computed(() => this.vm()?.readiness?.stripeCheckoutAvailable ?? false);
  readonly canManageSubscriptions = computed(
    () => this.authService.hasRole('ADMIN') || this.authService.hasRole('HR')
  );

  constructor() {
    const request$: Observable<PricingVm> = this.authService.isAuthenticated()
      ? forkJoin({
          plans: this.api.getPlans(),
          subscription: this.api.getCurrentSubscription().pipe(catchError(() => of(null))),
          readiness: this.api.getCatalogReadiness().pipe(catchError(() => of(null)))
        })
      : forkJoin({
          plans: this.api.getPlans(),
          readiness: this.api.getCatalogReadiness().pipe(catchError(() => of(null)))
        }).pipe(
          map((result) => ({
            ...result,
            subscription: null
          }))
        );

    request$.subscribe({
      next: (viewModel) => {
        this.vm.set(viewModel);
        this.loadError.set(null);
        this.loading.set(false);
      },
      error: (error) => {
        this.loadError.set(this.readBackendMessage(error, 'Impossible de charger les offres sans données réelles.'));
        this.loading.set(false);
      }
    });
  }

  featureLabel(feature: string): string {
    return toCommercialFeatureLabel(feature);
  }

  planActionLabel(plan: PlanResponse): string {
    if (this.previewModeActive()) {
      return this.showDemoHints ? "Ouvrir l'espace" : "Accéder à l'espace";
    }

    if (!this.authService.isAuthenticated()) {
      return plan.customPricing ? 'Créer un compte pour contacter' : 'Créer un compte pour choisir';
    }

    if (!this.stripeCheckoutAvailable() && !plan.customPricing) {
      return 'Paiement bientôt disponible';
    }

    if (!this.canManageSubscriptions()) {
      return plan.customPricing ? 'Contacter les RH' : 'Accès RH requis';
    }

    return plan.customPricing ? "Contacter l'équipe commerciale" : 'Accéder à la facturation';
  }

  openPlan(plan: PlanResponse): void {
    if (this.previewModeActive()) {
      this.openPreviewWorkspace();
      return;
    }

    if (!this.authService.isAuthenticated()) {
      this.router.navigate(['/signup'], {
        queryParams: {
          plan: plan.code
        }
      });
      return;
    }

    if (!this.canManageSubscriptions()) {
      this.toastService.warning('Seuls les utilisateurs RH ou administrateurs peuvent gérer un abonnement.');
      return;
    }

    if (!this.stripeCheckoutAvailable() && !plan.customPricing) {
      this.toastService.warning("Le paiement Stripe n'est pas encore actif sur cet environnement.");
      return;
    }

    this.router.navigate(['/billing'], {
      queryParams: {
        plan: plan.code
      }
    });
  }

  openPreviewWorkspace(): void {
    if (!this.authService.isAuthenticated()) {
      this.router.navigate(['/login'], {
        queryParams: {
          returnUrl: '/dashboard'
        }
      });
      return;
    }

    this.router.navigateByUrl('/dashboard');
  }

  private readBackendMessage(error: unknown, fallback: string): string {
    const backendMessage = (error as { error?: { message?: string } })?.error?.message;
    return typeof backendMessage === 'string' && backendMessage.trim()
      ? backendMessage
      : fallback;
  }
}
