var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../auth.service';
import { SubscriptionCheckoutService } from '../subscription-checkout.service';
import { SubscriptionLifecycleService } from '../subscription-lifecycle.service';
import { WorkRhApiService } from '../workrh-api.service';
import { FRONTEND_BASE_URL } from '../config';
let BillingPageComponent = class BillingPageComponent {
    constructor() {
        this.api = inject(WorkRhApiService);
        this.authService = inject(AuthService);
        this.checkoutService = inject(SubscriptionCheckoutService);
        this.lifecycleService = inject(SubscriptionLifecycleService);
        this.route = inject(ActivatedRoute);
        this.router = inject(Router);
        this.loading = signal(true);
        this.checkoutLoading = signal(null);
        this.actionLoading = signal(null);
        this.checkoutError = signal(null);
        this.opsMessage = signal(null);
        this.opsError = signal(null);
        this.supportLoading = signal(false);
        this.smsLoading = signal(false);
        this.exportLoading = signal(false);
        this.vm = signal(null);
        this.notifications = signal([]);
        this.supportTickets = signal([]);
        this.slaTickets = signal([]);
        this.checkoutState = signal(null);
        this.visiblePlans = computed(() => this.vm()?.plans.filter((plan) => plan.code !== 'ENTERPRISE') ?? []);
        this.daysRemaining = computed(() => {
            const renewsAt = this.vm()?.subscription.renewsAt;
            if (!renewsAt) {
                return null;
            }
            const diff = Math.ceil((new Date(renewsAt).getTime() - Date.now()) / (1000 * 60 * 60 * 24));
            return diff;
        });
        this.currentPlan = computed(() => {
            const viewModel = this.vm();
            return viewModel?.plans.find((plan) => plan.code === viewModel.subscription.planCode) ?? null;
        });
        this.previewModeActive = computed(() => this.vm()?.subscription.previewAllFeaturesActive ?? false);
        this.canManageSubscriptions = computed(() => this.authService.hasRole('ADMIN') || this.authService.hasRole('HR'));
        this.supportDraft = {
            requesterName: '',
            requesterEmail: '',
            phoneNumber: '',
            subject: '',
            message: ''
        };
        this.smsDraft = {
            employeeId: null,
            phoneNumber: '',
            message: ''
        };
        const checkoutParam = this.route.snapshot.queryParamMap.get('checkout');
        if (checkoutParam === 'success' || checkoutParam === 'cancelled') {
            this.checkoutState.set(checkoutParam);
        }
        this.api.loadViewModel().subscribe({
            next: (viewModel) => {
                this.vm.set(viewModel);
                this.seedDrafts();
                this.loadOperationsData(viewModel);
                this.loading.set(false);
            },
            error: () => this.loading.set(false)
        });
    }
    hasEntitlement(feature) {
        return this.vm()?.subscription.entitlements.includes(feature) ?? false;
    }
    startCheckout(planCode) {
        if (this.previewModeActive()) {
            this.opsMessage.set('Mode preview actif. Aucun checkout Stripe n est lance pour ce compte.');
            this.opsError.set(null);
            return;
        }
        const session = this.authService.session();
        if (!session) {
            this.router.navigateByUrl('/login');
            return;
        }
        if (!this.canManageSubscriptions()) {
            this.checkoutError.set('Seuls les utilisateurs RH ou administrateurs peuvent gerer un abonnement.');
            return;
        }
        this.checkoutLoading.set(planCode);
        this.checkoutError.set(null);
        this.checkoutService.createCheckout({
            planCode,
            seatsPurchased: this.vm()?.subscription.seatsPurchased ?? 25,
            paymentMethodTypes: ['card'],
            smsOptionEnabled: this.vm()?.subscription.smsOptionEnabled ?? false,
            advancedAuditOptionEnabled: this.vm()?.subscription.advancedAuditOptionEnabled ?? false,
            advancedExportOptionEnabled: planCode === 'PRO' || planCode === 'PREMIUM',
            customerEmail: session.email,
            successUrl: `${FRONTEND_BASE_URL}/billing?checkout=success`,
            cancelUrl: `${FRONTEND_BASE_URL}/billing?checkout=cancelled`
        }).subscribe({
            next: (response) => {
                this.checkoutLoading.set(null);
                window.location.href = response.checkoutUrl;
            },
            error: (error) => {
                this.checkoutLoading.set(null);
                this.checkoutError.set(this.getCheckoutErrorMessage(error));
            }
        });
    }
    upgradeTo(planCode) {
        if (this.previewModeActive()) {
            this.opsMessage.set('Mode preview actif. Les changements de plan sont bloques pour eviter tout impact reel.');
            this.opsError.set(null);
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
            next: (response) => {
                this.patchSubscription(response);
                this.actionLoading.set(null);
            },
            error: () => this.actionLoading.set(null)
        });
    }
    downgradeTo(planCode) {
        if (this.previewModeActive()) {
            this.opsMessage.set('Mode preview actif. Les changements de plan sont bloques pour eviter tout impact reel.');
            this.opsError.set(null);
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
            next: (response) => {
                this.patchSubscription(response);
                this.actionLoading.set(null);
            },
            error: () => this.actionLoading.set(null)
        });
    }
    cancelSubscription() {
        if (this.previewModeActive()) {
            this.opsMessage.set('Mode preview actif. L abonnement reel n est pas modifie.');
            this.opsError.set(null);
            return;
        }
        this.actionLoading.set('cancel');
        this.lifecycleService.cancel({ reason: 'Cancelled from billing page' }).subscribe({
            next: (response) => {
                this.patchSubscription(response);
                this.actionLoading.set(null);
            },
            error: () => this.actionLoading.set(null)
        });
    }
    reactivateSubscription() {
        if (this.previewModeActive()) {
            this.opsMessage.set('Mode preview actif. L abonnement reel n est pas modifie.');
            this.opsError.set(null);
            return;
        }
        this.actionLoading.set('reactivate');
        this.lifecycleService.reactivate().subscribe({
            next: (response) => {
                this.patchSubscription(response);
                this.actionLoading.set(null);
            },
            error: () => this.actionLoading.set(null)
        });
    }
    submitSupport(mode) {
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
                this.opsMessage.set(`Ticket ${ticket.category} cree avec succes.`);
                this.supportLoading.set(false);
                if (this.hasEntitlement('SLA_SUPPORT')) {
                    this.refreshSlaTickets();
                }
            },
            error: (error) => {
                this.opsError.set(this.readBackendMessage(error, 'Impossible de creer le ticket de support.'));
                this.supportLoading.set(false);
            }
        });
    }
    sendSms() {
        if (!this.smsDraft.phoneNumber.trim() || !this.smsDraft.message.trim()) {
            this.opsError.set('Numero et message SMS sont obligatoires.');
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
            error: (error) => {
                this.opsError.set(this.readBackendMessage(error, 'Impossible d envoyer le SMS.'));
                this.smsLoading.set(false);
            }
        });
    }
    downloadAccountingExport() {
        this.exportLoading.set(true);
        this.api.downloadAccountingExport().subscribe({
            next: (blob) => {
                this.saveBlob(blob, 'workrh-accounting-export.csv');
                this.exportLoading.set(false);
            },
            error: () => {
                this.opsError.set('Impossible de telecharger l export comptable.');
                this.exportLoading.set(false);
            }
        });
    }
    patchSubscription(subscription) {
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
    getCheckoutErrorMessage(error) {
        if (typeof error.error?.message === 'string' && error.error.message.trim()) {
            return error.error.message;
        }
        if (error.status === 401) {
            return 'Votre session a expire. Reconnectez-vous puis reessayez.';
        }
        if (error.status === 403) {
            return 'Votre compte ne peut pas lancer le checkout Stripe. Utilisez un compte RH ou administrateur.';
        }
        return 'Le paiement a echoue. Verifiez la configuration Stripe et reessayez.';
    }
    loadOperationsData(viewModel) {
        if (viewModel.subscription.entitlements.includes('EMAIL_SUPPORT')) {
            this.api.getSupportTickets().subscribe({
                next: (tickets) => this.supportTickets.set(tickets),
                error: () => this.supportTickets.set([])
            });
        }
        else {
            this.supportTickets.set([]);
        }
        if (viewModel.subscription.entitlements.includes('SLA_SUPPORT')) {
            this.refreshSlaTickets();
        }
        else {
            this.slaTickets.set([]);
        }
        if (viewModel.subscription.entitlements.includes('EMAIL_NOTIFICATIONS')) {
            this.refreshNotifications();
        }
        else {
            this.notifications.set([]);
        }
    }
    refreshNotifications() {
        this.api.getNotifications().subscribe({
            next: (notifications) => this.notifications.set(notifications),
            error: () => this.notifications.set([])
        });
    }
    refreshSlaTickets() {
        this.api.getSlaTickets().subscribe({
            next: (tickets) => this.slaTickets.set(tickets),
            error: () => this.slaTickets.set([])
        });
    }
    seedDrafts() {
        const session = this.authService.session();
        if (!session) {
            return;
        }
        if (!this.supportDraft.requesterEmail) {
            this.supportDraft.requesterEmail = session.email;
        }
    }
    readBackendMessage(error, fallback) {
        if (typeof error.error?.message === 'string' && error.error.message.trim()) {
            return error.error.message;
        }
        return fallback;
    }
    saveBlob(blob, filename) {
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = filename;
        link.click();
        window.URL.revokeObjectURL(url);
    }
};
BillingPageComponent = __decorate([
    Component({
        selector: 'app-billing-page',
        standalone: true,
        imports: [CommonModule, FormsModule],
        templateUrl: './billing-page.component.html',
        styleUrl: './page-styles.css'
    })
], BillingPageComponent);
export { BillingPageComponent };
