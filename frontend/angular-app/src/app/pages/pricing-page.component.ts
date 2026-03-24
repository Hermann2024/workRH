import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { timeout, retry, catchError } from 'rxjs/operators';
import { of, timer } from 'rxjs';
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

  // États du loading amélioré
  readonly showCheckoutLoading = signal(false);
  readonly checkoutProgress = signal(0);
  readonly checkoutStep = signal(1);
  readonly checkoutMessage = signal('');

  // Messages informatifs par plan
  private readonly checkoutMessages = {
    'STARTER': 'Configuration de votre abonnement Starter...',
    'PRO': 'Configuration de votre abonnement Pro avec conformité télétravail 34j...',
    'PREMIUM': 'Configuration de votre abonnement Premium avec fonctionnalités avancées...',
    'ENTERPRISE': 'Configuration de votre abonnement Enterprise personnalisé...'
  } as const;

  // Messages d'erreur
  private readonly errorMessages = {
    'TIMEOUT': 'Le délai de réponse a été dépassé. Veuillez réessayer.',
    'NETWORK': 'Problème de connexion. Vérifiez votre connexion internet.',
    'SERVER': 'Erreur serveur temporaire. Notre équipe a été notifiée.',
    'UNKNOWN': 'Une erreur inattendue s\'est produite. Veuillez réessayer.'
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

    // Activer le loading amélioré
    this.showCheckoutLoading.set(true);
    this.checkoutProgress.set(0);
    this.checkoutStep.set(1);
    this.checkoutMessage.set(this.checkoutMessages[planCode]);

    // Animation de progression
    const progressInterval = setInterval(() => {
      this.checkoutProgress.update(p => Math.min(p + 2, 85)); // Max 85% pendant l'appel API
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

    // Étape 2: Configuration du paiement
    timer(300).subscribe(() => {
      this.checkoutStep.set(2);
      this.checkoutMessage.set('Configuration du paiement sécurisé...');
    });

    // Timeout 8 secondes + 2 retries
    this.checkoutService.createCheckout(request)
      .pipe(
        timeout(8000),
        retry(2),
        catchError(error => {
          console.error('Checkout error:', error);
          clearInterval(progressInterval);
          return of(null);
        })
      )
      .subscribe({
        next: (response) => {
          clearInterval(progressInterval);

          if (response) {
            // Étape 3: Activation
            this.checkoutStep.set(3);
            this.checkoutProgress.set(100);
            this.checkoutMessage.set('Redirection vers le paiement sécurisé...');

            // Délai pour montrer le succès avant redirection
            timer(800).subscribe(() => {
              this.showCheckoutLoading.set(false);
              window.location.href = response.checkoutUrl;
            });
          } else {
            // Erreur après retries
            this.showCheckoutLoading.set(false);
            this.toastService.error('Échec de la configuration du paiement. Veuillez réessayer.');
          }
        },
        error: (err) => {
          clearInterval(progressInterval);
          this.showCheckoutLoading.set(false);
          const errorMessage = this.getErrorMessage(err);
          this.toastService.error(errorMessage);
        }
      });
  }

  cancelCheckout(): void {
    this.showCheckoutLoading.set(false);
    this.checkoutProgress.set(0);
    this.checkoutStep.set(1);
    this.toastService.info('Configuration du paiement annulée.');
  }

  private getErrorMessage(err: any): string {
    // Timeout error
    if (err?.name === 'TimeoutError' || err?.message?.includes('timeout')) {
      return this.errorMessages.TIMEOUT;
    }

    // Network errors
    if (!navigator.onLine || err?.status === 0 || err?.message?.includes('network')) {
      return this.errorMessages.NETWORK;
    }

    // Server errors (5xx)
    if (err?.status >= 500) {
      return this.errorMessages.SERVER;
    }

    // Client errors (4xx) - usually user-related
    if (err?.status >= 400 && err?.status < 500) {
      return 'Erreur de configuration. Vérifiez vos informations.';
    }

    // Stripe-specific errors
    if (err?.error?.type === 'card_error' || err?.error?.type === 'invalid_request_error') {
      return 'Erreur de configuration du paiement. Contactez le support.';
    }

    // Default unknown error
    return this.errorMessages.UNKNOWN;
  }
}
