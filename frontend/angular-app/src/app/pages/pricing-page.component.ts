import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { throwError, timer } from 'rxjs';
import { catchError, timeout } from 'rxjs/operators';
import { AuthService } from '../auth.service';
import { SubscriptionCheckoutService } from '../subscription-checkout.service';
import { WorkRhApiService, WorkRhVm } from '../workrh-api.service';
import { FRONTEND_BASE_URL } from '../config';
import { ToastService } from '../services/toast.service';
import { CheckoutLoadingComponent } from '../components/checkout-loading.component';

@Component({
  selector: 'app-pricing-page',
  standalone: true,
  imports: [CommonModule, CheckoutLoadingComponent],
  templateUrl: './pricing-page.component.html',
  styleUrl: './page-styles.css'
})
export class PricingPageComponent {
  private readonly api = inject(WorkRhApiService);
  private readonly authService = inject(AuthService);
  private readonly checkoutService = inject(SubscriptionCheckoutService);
  private readonly router = inject(Router);
  private readonly toastService = inject(ToastService);

  readonly loading = signal(true);
  readonly checkoutLoading = signal<string | null>(null);
  readonly vm = signal<WorkRhVm | null>(null);
  readonly visiblePlans = computed(() => this.vm()?.plans.filter((plan) => plan.code !== 'ENTERPRISE') ?? []);

  readonly showCheckoutLoading = signal(false);
  readonly checkoutProgress = signal(0);
  readonly checkoutStep = signal(1);
  readonly checkoutMessage = signal('');

  private readonly checkoutMessages = {
    STARTER: 'Configuration de votre abonnement Starter...',
    PRO: 'Configuration de votre abonnement Pro avec conformite teletravail 34j...',
    PREMIUM: 'Configuration de votre abonnement Premium avec fonctionnalites avancees...',
    ENTERPRISE: 'Configuration de votre abonnement Enterprise personnalise...'
  } as const;

  private readonly errorMessages = {
    TIMEOUT: 'Le delai de reponse a ete depasse. Veuillez reessayer.',
    NETWORK: 'Probleme de connexion. Verifiez votre connexion internet.',
    SERVER: 'Erreur serveur temporaire. Notre equipe a ete notifiee.',
    UNKNOWN: 'Une erreur inattendue s\'est produite. Veuillez reessayer.'
  } as const;

  constructor() {
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

    this.showCheckoutLoading.set(true);
    this.checkoutProgress.set(0);
    this.checkoutStep.set(1);
    this.checkoutMessage.set(this.checkoutMessages[planCode]);

    const progressInterval = setInterval(() => {
      this.checkoutProgress.update((progress) => Math.min(progress + 2, 85));
    }, 50);

    const request = {
      planCode,
      seatsPurchased: 25,
      paymentMethodTypes: ['card', 'sepa_debit'],
      smsOptionEnabled: false,
      advancedAuditOptionEnabled: false,
      advancedExportOptionEnabled: planCode === 'PRO' || planCode === 'PREMIUM',
      customerEmail: session.email,
      successUrl: `${FRONTEND_BASE_URL}/dashboard?checkout=success`,
      cancelUrl: `${FRONTEND_BASE_URL}/pricing?checkout=cancelled`
    };

    timer(300).subscribe(() => {
      this.checkoutStep.set(2);
      this.checkoutMessage.set('Configuration du paiement securise...');
    });

    this.checkoutService.createCheckout(request)
      .pipe(
        timeout(20000),
        catchError((error: HttpErrorResponse) => {
          clearInterval(progressInterval);
          return throwError(() => error);
        })
      )
      .subscribe({
        next: (response) => {
          clearInterval(progressInterval);
          this.checkoutStep.set(3);
          this.checkoutProgress.set(100);
          this.checkoutMessage.set('Redirection vers le paiement securise...');

          timer(800).subscribe(() => {
            this.showCheckoutLoading.set(false);
            window.location.href = response.checkoutUrl;
          });
        },
        error: (error: HttpErrorResponse) => {
          clearInterval(progressInterval);
          this.showCheckoutLoading.set(false);
          this.toastService.error(this.getErrorMessage(error));
        }
      });
  }

  cancelCheckout(): void {
    this.showCheckoutLoading.set(false);
    this.checkoutProgress.set(0);
    this.checkoutStep.set(1);
    this.toastService.info('Configuration du paiement annulee.');
  }

  private getErrorMessage(error: HttpErrorResponse): string {
    const backendMessage = typeof error?.error?.message === 'string' ? error.error.message.trim() : '';
    const errorName = (error as { name?: string } | null)?.name;
    if (backendMessage) {
      return backendMessage;
    }

    if (errorName === 'TimeoutError' || error?.message?.includes('timeout')) {
      return this.errorMessages.TIMEOUT;
    }

    if (!navigator.onLine || error?.status === 0 || error?.message?.includes('network')) {
      return this.errorMessages.NETWORK;
    }

    if (error?.status >= 500) {
      return this.errorMessages.SERVER;
    }

    if (error?.status >= 400 && error?.status < 500) {
      return 'Erreur de configuration du checkout. Verifiez vos informations et la configuration Stripe.';
    }

    return this.errorMessages.UNKNOWN;
  }
}
