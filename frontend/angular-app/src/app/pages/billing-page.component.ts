import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../auth.service';
import { SubscriptionCheckoutService } from '../subscription-checkout.service';
import { SubscriptionLifecycleService } from '../subscription-lifecycle.service';
import { WorkRhApiService, WorkRhVm } from '../workrh-api.service';
import { FRONTEND_BASE_URL } from '../config';

@Component({
  selector: 'app-billing-page',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './billing-page.component.html',
  styleUrl: './page-styles.css'
})
export class BillingPageComponent {
  private readonly api = inject(WorkRhApiService);
  private readonly authService = inject(AuthService);
  private readonly checkoutService = inject(SubscriptionCheckoutService);
  private readonly lifecycleService = inject(SubscriptionLifecycleService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly loading = signal(true);
  readonly checkoutLoading = signal<string | null>(null);
  readonly actionLoading = signal<string | null>(null);
  readonly checkoutError = signal<string | null>(null);
  readonly vm = signal<WorkRhVm | null>(null);
  readonly checkoutState = signal<'success' | 'cancelled' | null>(null);
  readonly visiblePlans = computed(() => this.vm()?.plans.filter((plan) => plan.code !== 'ENTERPRISE') ?? []);
  readonly daysRemaining = computed(() => {
    const renewsAt = this.vm()?.subscription.renewsAt;
    if (!renewsAt) {
      return null;
    }
    const diff = Math.ceil((new Date(renewsAt).getTime() - Date.now()) / (1000 * 60 * 60 * 24));
    return diff;
  });
  readonly currentPlan = computed(() => {
    const viewModel = this.vm();
    return viewModel?.plans.find((plan) => plan.code === viewModel.subscription.planCode) ?? null;
  });

  constructor() {
    const checkoutParam = this.route.snapshot.queryParamMap.get('checkout');
    if (checkoutParam === 'success' || checkoutParam === 'cancelled') {
      this.checkoutState.set(checkoutParam);
    }

    this.api.loadViewModel().subscribe({
      next: (viewModel: WorkRhVm) => {
        this.vm.set(viewModel);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  startCheckout(planCode: 'STARTER' | 'PRO' | 'PREMIUM' | 'ENTERPRISE'): void {
    const session = this.authService.session();
    if (!session) {
      this.router.navigateByUrl('/login');
      return;
    }

    this.checkoutLoading.set(planCode);
    this.checkoutError.set(null);
    this.checkoutService.createCheckout({
      planCode,
      seatsPurchased: this.vm()?.subscription.seatsPurchased ?? 25,
      paymentMethodTypes: ['card', 'sepa_debit'],
      smsOptionEnabled: this.vm()?.subscription.smsOptionEnabled ?? false,
      advancedAuditOptionEnabled: this.vm()?.subscription.advancedAuditOptionEnabled ?? false,
      advancedExportOptionEnabled: planCode === 'PRO' || planCode === 'PREMIUM',
      customerEmail: session.email,
      successUrl: `${FRONTEND_BASE_URL}/billing?checkout=success`,
      cancelUrl: `${FRONTEND_BASE_URL}/billing?checkout=cancelled`
    }).subscribe({
      next: (response: { sessionId: string; checkoutUrl: string }) => {
        this.checkoutLoading.set(null);
        window.location.href = response.checkoutUrl;
      },
      error: (error: HttpErrorResponse) => {
        this.checkoutLoading.set(null);
        this.checkoutError.set(error.error?.message ?? 'Le paiement a echoue. Verifiez la configuration Stripe et reessayez.');
      }
    });
  }

  upgradeTo(planCode: 'STARTER' | 'PRO' | 'PREMIUM' | 'ENTERPRISE'): void {
    const current = this.vm()?.subscription;
    if (!current) {
      return;
    }
    this.actionLoading.set(`upgrade-${planCode}`);
    this.lifecycleService.upgrade({
      targetPlanCode: planCode,
      seatsPurchased: current.seatsPurchased,
      smsOptionEnabled: current.smsOptionEnabled,
      advancedAuditOptionEnabled: current.advancedAuditOptionEnabled,
      advancedExportOptionEnabled: current.advancedExportOptionEnabled
    }).subscribe({
      next: (response: WorkRhVm['subscription']) => {
        this.patchSubscription(response);
        this.actionLoading.set(null);
      },
      error: () => this.actionLoading.set(null)
    });
  }

  downgradeTo(planCode: 'STARTER' | 'PRO' | 'PREMIUM' | 'ENTERPRISE'): void {
    const current = this.vm()?.subscription;
    if (!current) {
      return;
    }
    this.actionLoading.set(`downgrade-${planCode}`);
    this.lifecycleService.downgrade({
      targetPlanCode: planCode,
      seatsPurchased: current.seatsPurchased,
      smsOptionEnabled: current.smsOptionEnabled,
      advancedAuditOptionEnabled: current.advancedAuditOptionEnabled,
      advancedExportOptionEnabled: current.advancedExportOptionEnabled
    }).subscribe({
      next: (response: WorkRhVm['subscription']) => {
        this.patchSubscription(response);
        this.actionLoading.set(null);
      },
      error: () => this.actionLoading.set(null)
    });
  }

  cancelSubscription(): void {
    this.actionLoading.set('cancel');
    this.lifecycleService.cancel({ reason: 'Cancelled from billing page' }).subscribe({
      next: (response: WorkRhVm['subscription']) => {
        this.patchSubscription(response);
        this.actionLoading.set(null);
      },
      error: () => this.actionLoading.set(null)
    });
  }

  reactivateSubscription(): void {
    this.actionLoading.set('reactivate');
    this.lifecycleService.reactivate().subscribe({
      next: (response: WorkRhVm['subscription']) => {
        this.patchSubscription(response);
        this.actionLoading.set(null);
      },
      error: () => this.actionLoading.set(null)
    });
  }

  private patchSubscription(subscription: WorkRhVm['subscription']): void {
    const currentVm = this.vm();
    if (!currentVm) {
      return;
    }
    this.vm.set({
      ...currentVm,
      subscription
    });
  }
}
