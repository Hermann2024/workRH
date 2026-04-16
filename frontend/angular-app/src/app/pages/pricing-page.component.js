var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../auth.service';
import { PLAN_COMMERCIAL_CONTENT } from '../plan-commercial-content';
import { toCommercialFeatureLabel } from '../plan-feature-labels';
import { ToastService } from '../services/toast.service';
import { WorkRhApiService } from '../workrh-api.service';
let PricingPageComponent = class PricingPageComponent {
    constructor() {
        this.api = inject(WorkRhApiService);
        this.authService = inject(AuthService);
        this.router = inject(Router);
        this.toastService = inject(ToastService);
        this.loading = signal(true);
        this.vm = signal(null);
        this.visiblePlans = computed(() => this.vm()?.plans ?? []);
        this.previewModeActive = computed(() => this.vm()?.subscription.previewAllFeaturesActive ?? false);
        this.canManageSubscriptions = computed(() => this.authService.hasRole('ADMIN') || this.authService.hasRole('HR'));
        this.api.loadViewModel().subscribe({
            next: (viewModel) => {
                this.vm.set(viewModel);
                this.loading.set(false);
            },
            error: () => this.loading.set(false)
        });
    }
    featureLabel(feature) {
        return toCommercialFeatureLabel(feature);
    }
    planContent(planCode) {
        return PLAN_COMMERCIAL_CONTENT[planCode];
    }
    planActionLabel(plan) {
        if (this.previewModeActive()) {
            return 'Ouvrir le workspace';
        }
        if (!this.authService.isAuthenticated()) {
            return plan.customPricing ? 'Se connecter pour contacter' : 'Se connecter pour choisir';
        }
        if (!this.canManageSubscriptions()) {
            return plan.customPricing ? 'Contacter les RH' : 'Acces RH requis';
        }
        return plan.customPricing ? 'Contacter le commercial' : 'Acceder au billing';
    }
    openPlan(plan) {
        if (this.previewModeActive()) {
            this.openPreviewWorkspace();
            return;
        }
        if (!this.authService.isAuthenticated()) {
            this.router.navigate(['/login'], {
                queryParams: {
                    returnUrl: `/billing?plan=${plan.code}`
                }
            });
            return;
        }
        if (!this.canManageSubscriptions()) {
            this.toastService.warning('Seuls les utilisateurs RH ou administrateurs peuvent gerer un abonnement.');
            return;
        }
        this.router.navigate(['/billing'], {
            queryParams: {
                plan: plan.code
            }
        });
    }
    openPreviewWorkspace() {
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
};
PricingPageComponent = __decorate([
    Component({
        selector: 'app-pricing-page',
        standalone: true,
        imports: [CommonModule],
        templateUrl: './pricing-page.component.html',
        styleUrl: './page-styles.css'
    })
], PricingPageComponent);
export { PricingPageComponent };
