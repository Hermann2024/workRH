import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, forkJoin, map, Observable, of } from 'rxjs';
import { AuthService } from '../auth.service';
import { SHOW_DEMO_HINTS } from '../config';
import { I18nService } from '../i18n/i18n.service';
import { TranslatePipe } from '../i18n/translate.pipe';
import { CommercialPlanCode, PLAN_COMMERCIAL_CONTENT } from '../plan-commercial-content';
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
  imports: [CommonModule, TranslatePipe],
  templateUrl: './pricing-page.component.html',
  styleUrl: './page-styles.css'
})
export class PricingPageComponent {
  private readonly api = inject(WorkRhApiService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly toastService = inject(ToastService);
  private readonly i18n = inject(I18nService);

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
        this.loadError.set(this.readBackendMessage(error, 'pricing.err_load'));
        this.loading.set(false);
      }
    });
  }

  featureLabel(feature: string): string {
    this.i18n.locale();
    const key = `planFeature.${feature}`;
    const translated = this.i18n.translate(key);
    return translated !== key ? translated : toCommercialFeatureLabel(feature);
  }

  planHeadline(plan: PlanResponse): string {
    return this.planCopy(plan, 'headline');
  }

  planIdealFor(plan: PlanResponse): string {
    return this.planCopy(plan, 'idealFor');
  }

  topBusinessValues(plan: PlanResponse): string[] {
    this.i18n.locale();
    const fallback = this.commercialFallback(plan).businessValue;
    const values: string[] = [];
    for (let index = 0; index < 2; index += 1) {
      const key = `pricing.plan.${plan.code}.bv${index}`;
      const translated = this.i18n.translate(key);
      values.push(translated !== key ? translated : (fallback[index] ?? ''));
    }
    return values.filter((line) => line.length > 0);
  }

  planActionLabel(plan: PlanResponse): string {
    this.i18n.locale();
    if (this.previewModeActive()) {
      return this.showDemoHints
        ? this.i18n.translate('pricing.action_preview_demo')
        : this.i18n.translate('pricing.action_preview_prod');
    }

    if (!this.authService.isAuthenticated()) {
      return plan.customPricing
        ? this.i18n.translate('pricing.action_signup_custom')
        : this.i18n.translate('pricing.action_signup');
    }

    if (!this.stripeCheckoutAvailable() && !plan.customPricing) {
      return this.i18n.translate('pricing.action_payment_soon');
    }

    if (!this.canManageSubscriptions()) {
      return plan.customPricing
        ? this.i18n.translate('pricing.action_hr_only_custom')
        : this.i18n.translate('pricing.action_hr_only');
    }

    return plan.customPricing
      ? this.i18n.translate('pricing.action_contact_sales')
      : this.i18n.translate('pricing.action_billing');
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
      this.toastService.warning(this.i18n.translate('pricing.toast_hr_only'));
      return;
    }

    if (!this.stripeCheckoutAvailable() && !plan.customPricing) {
      this.toastService.warning(this.i18n.translate('pricing.toast_stripe'));
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

  private commercialFallback(plan: PlanResponse) {
    const code = plan.code as CommercialPlanCode;
    return PLAN_COMMERCIAL_CONTENT[code] ?? PLAN_COMMERCIAL_CONTENT.STARTER;
  }

  private planCopy(plan: PlanResponse, field: 'headline' | 'idealFor'): string {
    this.i18n.locale();
    const key = `pricing.plan.${plan.code}.${field}`;
    const translated = this.i18n.translate(key);
    if (translated !== key) {
      return translated;
    }
    return field === 'headline' ? this.commercialFallback(plan).headline : this.commercialFallback(plan).idealFor;
  }

  private readBackendMessage(error: unknown, fallbackKey: string): string {
    this.i18n.locale();
    const backendMessage = (error as { error?: { message?: string } })?.error?.message;
    if (typeof backendMessage === 'string' && backendMessage.trim()) {
      return backendMessage;
    }
    return this.i18n.translate(fallbackKey);
  }
}
