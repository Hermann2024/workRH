import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { catchError, switchMap } from 'rxjs';
import { AuthService } from '../auth.service';
import { SubscriptionCheckoutService } from '../subscription-checkout.service';
import { SubscriptionLifecycleService } from '../subscription-lifecycle.service';
import { NotificationResponse, SlaTicketResponse, SupportTicketResponse, WorkRhApiService, WorkRhVm } from '../workrh-api.service';
import { FRONTEND_BASE_URL, SHOW_DEMO_HINTS } from '../config';

@Component({
  selector: 'app-billing-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './billing-page.component.html',
  styleUrl: './page-styles.css'
})
export class BillingPageComponent {
  private autoCheckoutTriggered = false;
  private readonly api = inject(WorkRhApiService);
  private readonly authService = inject(AuthService);
  private readonly checkoutService = inject(SubscriptionCheckoutService);
  private readonly lifecycleService = inject(SubscriptionLifecycleService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly showDemoHints = SHOW_DEMO_HINTS;
  readonly loading = signal(true);
  readonly loadError = signal<string | null>(null);
  readonly checkoutLoading = signal<string | null>(null);
  readonly actionLoading = signal<string | null>(null);
  readonly checkoutError = signal<string | null>(null);
  readonly opsMessage = signal<string | null>(null);
  readonly opsError = signal<string | null>(null);
  readonly supportLoading = signal(false);
  readonly smsLoading = signal(false);
  readonly exportLoading = signal(false);
  readonly vm = signal<WorkRhVm | null>(null);
  readonly notifications = signal<NotificationResponse[]>([]);
  readonly supportTickets = signal<SupportTicketResponse[]>([]);
  readonly slaTickets = signal<SlaTicketResponse[]>([]);
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
  readonly previewModeActive = computed(() => this.vm()?.subscription.previewAllFeaturesActive ?? false);
  readonly canManageSubscriptions = computed(
    () => this.authService.hasRole('ADMIN') || this.authService.hasRole('HR')
  );
  supportDraft = {
    requesterName: '',
    requesterEmail: '',
    phoneNumber: '',
    subject: '',
    message: ''
  };
  smsDraft = {
    employeeId: null as number | null,
    phoneNumber: '',
    message: ''
  };

  constructor() {
    const checkoutParam = this.route.snapshot.queryParamMap.get('checkout');
    const checkoutSessionId = this.route.snapshot.queryParamMap.get('session_id');
    if (checkoutParam === 'success' || checkoutParam === 'cancelled') {
      this.checkoutState.set(checkoutParam);
    }

    const viewModel$ = checkoutParam === 'success' && checkoutSessionId
      ? this.checkoutService.confirmCheckout(checkoutSessionId).pipe(
        switchMap(() => this.api.loadViewModel()),
        catchError((error: HttpErrorResponse) => {
          this.checkoutError.set(this.readBackendMessage(
            error,
            "Le paiement a été validé, mais l'abonnement n'a pas encore pu être synchronisé."
          ));
          return this.api.loadViewModel();
        })
      )
      : this.api.loadViewModel();

    viewModel$.subscribe({
      next: (viewModel: WorkRhVm) => {
        this.vm.set(viewModel);
        this.loadError.set(null);
        this.seedDrafts();
        this.loadOperationsData(viewModel);
        this.loading.set(false);
        this.tryAutoStartCheckout();
      },
      error: (error: HttpErrorResponse) => {
        this.loadError.set(this.readBackendMessage(error, 'Impossible de charger la facturation sans donnees reelles.'));
        this.loading.set(false);
      }
    });
  }

  hasEntitlement(feature: string): boolean {
    return this.vm()?.subscription.entitlements.includes(feature) ?? false;
  }

  startCheckout(planCode: 'STARTER' | 'PRO' | 'PREMIUM' | 'ENTERPRISE'): void {
    if (this.previewModeActive()) {
      this.blockPreviewAction('checkout');
      return;
    }

    const session = this.authService.session();
    if (!session) {
      this.router.navigateByUrl('/login');
      return;
    }
    if (!this.canManageSubscriptions()) {
      this.checkoutError.set('Seuls les utilisateurs RH ou administrateurs peuvent gérer un abonnement.');
      return;
    }

    const currentSeats = this.vm()?.subscription.seatsPurchased ?? 25;
    const targetPlan = this.vm()?.plans.find((plan) => plan.code === planCode);
    const seatsPurchased = Math.max(currentSeats, targetPlan?.minEmployees ?? currentSeats);

    this.checkoutLoading.set(planCode);
    this.checkoutError.set(null);
    this.checkoutService.createCheckout({
      planCode,
      seatsPurchased,
      paymentMethodTypes: ['card'],
      smsOptionEnabled: this.vm()?.subscription.smsOptionEnabled ?? false,
      advancedAuditOptionEnabled: this.vm()?.subscription.advancedAuditOptionEnabled ?? false,
      advancedExportOptionEnabled: this.vm()?.subscription.advancedExportOptionEnabled ?? false,
      customerEmail: session.email,
      successUrl: `${FRONTEND_BASE_URL}/billing?checkout=success&session_id={CHECKOUT_SESSION_ID}`,
      cancelUrl: `${FRONTEND_BASE_URL}/billing?checkout=cancelled`
    }).subscribe({
      next: (response: { sessionId: string; checkoutUrl: string }) => {
        this.checkoutLoading.set(null);
        window.location.href = response.checkoutUrl;
      },
      error: (error: HttpErrorResponse) => {
        this.checkoutLoading.set(null);
        this.checkoutError.set(this.getCheckoutErrorMessage(error));
      }
    });
  }

  upgradeTo(planCode: 'STARTER' | 'PRO' | 'PREMIUM' | 'ENTERPRISE'): void {
    if (this.previewModeActive()) {
      this.blockPreviewAction('plan-change');
      return;
    }
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
    if (this.previewModeActive()) {
      this.blockPreviewAction('plan-change');
      return;
    }
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
    if (this.previewModeActive()) {
      this.blockPreviewAction('subscription');
      return;
    }
    this.actionLoading.set('cancel');
    this.lifecycleService.cancel({ reason: 'Annulation demandée depuis la page de facturation' }).subscribe({
      next: (response: WorkRhVm['subscription']) => {
        this.patchSubscription(response);
        this.actionLoading.set(null);
      },
      error: () => this.actionLoading.set(null)
    });
  }

  reactivateSubscription(): void {
    if (this.previewModeActive()) {
      this.blockPreviewAction('subscription');
      return;
    }
    this.actionLoading.set('reactivate');
    this.lifecycleService.reactivate().subscribe({
      next: (response: WorkRhVm['subscription']) => {
        this.patchSubscription(response);
        this.actionLoading.set(null);
      },
      error: () => this.actionLoading.set(null)
    });
  }

  submitSupport(mode: 'standard' | 'priority' | 'onboarding' | 'sso' | 'security' | 'hosting' | 'custom-development' | 'integration'): void {
    const payload = {
      requesterName: this.supportDraft.requesterName.trim(),
      requesterEmail: this.supportDraft.requesterEmail.trim(),
      phoneNumber: this.supportDraft.phoneNumber.trim() || null,
      subject: this.supportDraft.subject.trim(),
      message: this.supportDraft.message.trim()
    };

    if (!payload.requesterEmail || !payload.subject || !payload.message) {
      this.opsError.set('Email, sujet et message sont obligatoires pour ouvrir un ticket.');
      return;
    }

    this.opsError.set(null);
    this.opsMessage.set(null);
    this.supportLoading.set(true);
    this.api.createSupportTicket(mode, payload).subscribe({
      next: (ticket) => {
        this.supportTickets.update((tickets) => [ticket, ...tickets]);
        this.supportDraft.subject = '';
        this.supportDraft.message = '';
        this.opsMessage.set(`Ticket ${ticket.category} créé avec succès.`);
        this.supportLoading.set(false);
        if (this.hasEntitlement('SLA_SUPPORT')) {
          this.refreshSlaTickets();
        }
      },
      error: (error: HttpErrorResponse) => {
        this.opsError.set(this.readBackendMessage(error, 'Impossible de créer le ticket de support.'));
        this.supportLoading.set(false);
      }
    });
  }

  sendSms(): void {
    if (!this.smsDraft.phoneNumber.trim() || !this.smsDraft.message.trim()) {
      this.opsError.set('Numéro et message SMS sont obligatoires.');
      return;
    }

    this.opsError.set(null);
    this.opsMessage.set(null);
    this.smsLoading.set(true);
    this.api.sendSms({
      employeeId: this.smsDraft.employeeId,
      phoneNumber: this.smsDraft.phoneNumber.trim(),
      message: this.smsDraft.message.trim()
    }).subscribe({
      next: (response) => {
        this.opsMessage.set(`SMS ${response.status.toLowerCase()} via ${response.provider}.`);
        this.smsDraft.message = '';
        this.smsLoading.set(false);
        if (this.hasEntitlement('EMAIL_NOTIFICATIONS')) {
          this.refreshNotifications();
        }
      },
      error: (error: HttpErrorResponse) => {
        this.opsError.set(this.readBackendMessage(error, "Impossible d'envoyer le SMS."));
        this.smsLoading.set(false);
      }
    });
  }

  downloadAccountingExport(): void {
    this.exportLoading.set(true);
    this.api.downloadAccountingExport().subscribe({
      next: (blob) => {
        this.saveBlob(blob, 'workrh-accounting-export.csv');
        this.exportLoading.set(false);
      },
      error: () => {
        this.opsError.set("Impossible de télécharger l'export comptable.");
        this.exportLoading.set(false);
      }
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
    this.loadOperationsData({
      ...currentVm,
      subscription
    });
  }

  private getCheckoutErrorMessage(error: HttpErrorResponse): string {
    if (typeof error.error?.message === 'string' && error.error.message.trim()) {
      return error.error.message;
    }
    if (error.status === 401) {
      return 'Votre session a expiré. Reconnectez-vous puis réessayez.';
    }
    if (error.status === 403) {
      return 'Votre compte ne peut pas lancer le checkout Stripe. Utilisez un compte RH ou administrateur.';
    }
    return 'Le paiement a échoué. Vérifiez la configuration Stripe et réessayez.';
  }

  private loadOperationsData(viewModel: WorkRhVm): void {
    if (viewModel.subscription.entitlements.includes('EMAIL_SUPPORT')) {
      this.api.getSupportTickets().subscribe({
        next: (tickets) => this.supportTickets.set(tickets),
        error: () => this.supportTickets.set([])
      });
    } else {
      this.supportTickets.set([]);
    }

    if (viewModel.subscription.entitlements.includes('SLA_SUPPORT')) {
      this.refreshSlaTickets();
    } else {
      this.slaTickets.set([]);
    }

    if (viewModel.subscription.entitlements.includes('EMAIL_NOTIFICATIONS')) {
      this.refreshNotifications();
    } else {
      this.notifications.set([]);
    }
  }

  private refreshNotifications(): void {
    this.api.getNotifications().subscribe({
      next: (notifications) => this.notifications.set(notifications),
      error: () => this.notifications.set([])
    });
  }

  private refreshSlaTickets(): void {
    this.api.getSlaTickets().subscribe({
      next: (tickets) => this.slaTickets.set(tickets),
      error: () => this.slaTickets.set([])
    });
  }

  private seedDrafts(): void {
    const session = this.authService.session();
    if (!session) {
      return;
    }
    if (!this.supportDraft.requesterEmail) {
      this.supportDraft.requesterEmail = session.email;
    }
  }

  private readBackendMessage(error: HttpErrorResponse, fallback: string): string {
    if (typeof error.error?.message === 'string' && error.error.message.trim()) {
      return error.error.message;
    }
    return fallback;
  }

  private saveBlob(blob: Blob, filename: string): void {
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    link.click();
    window.URL.revokeObjectURL(url);
  }

  private tryAutoStartCheckout(): void {
    const requestedPlan = this.readRequestedPlan();
    if (!requestedPlan || requestedPlan === 'ENTERPRISE' || this.autoCheckoutTriggered || this.checkoutState()) {
      return;
    }
    if (this.previewModeActive() || !this.canManageSubscriptions()) {
      return;
    }
    this.autoCheckoutTriggered = true;
    this.startCheckout(requestedPlan);
  }

  private readRequestedPlan(): 'STARTER' | 'PRO' | 'PREMIUM' | 'ENTERPRISE' | null {
    const value = this.route.snapshot.queryParamMap.get('plan');
    if (value === 'STARTER' || value === 'PRO' || value === 'PREMIUM' || value === 'ENTERPRISE') {
      return value;
    }
    return null;
  }

  previewBannerMessage(): string {
    return this.showDemoHints
      ? "Mode démo actif sur ce compte. Stripe et les changements d'abonnement réels sont neutralisés. Utilisez rh@company.com pour tester le paiement."
      : 'Les paiements en ligne et les changements de plan sont temporairement indisponibles sur cet espace.';
  }

  private blockPreviewAction(action: 'checkout' | 'plan-change' | 'subscription'): void {
    const customerMessageByAction = {
      checkout: 'Les paiements en ligne ne sont pas disponibles sur cet espace pour le moment.',
      'plan-change': 'Les changements de plan sont temporairement indisponibles sur cet espace.',
      subscription: "Les modifications d'abonnement sont temporairement indisponibles sur cet espace."
    } as const;

    const demoMessageByAction = {
      checkout: "Mode démo actif. Aucun checkout Stripe n'est lancé pour ce compte.",
      'plan-change': "Mode démo actif. Les changements de plan sont bloqués pour éviter tout impact réel.",
      subscription: "Mode démo actif. L'abonnement réel n'est pas modifié."
    } as const;

    this.opsMessage.set(this.showDemoHints ? demoMessageByAction[action] : customerMessageByAction[action]);
    this.opsError.set(null);
  }
}
