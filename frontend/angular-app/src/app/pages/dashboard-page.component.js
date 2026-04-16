var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { LoadingSkeletonComponent } from '../components/loading-skeleton.component';
import { WorkRhApiService } from '../workrh-api.service';
let DashboardPageComponent = class DashboardPageComponent {
    constructor() {
        this.api = inject(WorkRhApiService);
        this.referenceDate = new Date();
        this.loading = signal(true);
        this.vm = signal(null);
        this.notifications = signal([]);
        this.monthlyStats = signal(null);
        this.exportLoading = signal(null);
        this.currentPlan = computed(() => {
            const viewModel = this.vm();
            return viewModel?.plans.find((plan) => plan.code === viewModel.subscription.planCode) ?? null;
        });
        this.companySummary = computed(() => this.vm()?.companySummary ?? null);
        this.latestNotifications = computed(() => this.notifications().slice(0, 6));
        this.currentYear = this.referenceDate.getFullYear();
        this.currentMonth = this.referenceDate.getMonth() + 1;
        this.api.loadViewModel(this.referenceDate).subscribe({
            next: (viewModel) => {
                this.vm.set(viewModel);
                this.loadAdvancedData(viewModel);
                this.loading.set(false);
            },
            error: () => this.loading.set(false)
        });
    }
    isEntitled(feature) {
        return this.vm()?.subscription.entitlements.includes(feature) ?? false;
    }
    monthLabel(month) {
        return new Intl.DateTimeFormat('fr-FR', { month: 'short' }).format(new Date(this.currentYear, month - 1, 1));
    }
    downloadDashboardExport(format) {
        this.exportLoading.set(format);
        this.api.downloadDashboardExport(format, this.currentYear, this.currentMonth).subscribe({
            next: (blob) => {
                const extension = format === 'csv' ? 'csv' : format === 'pdf' ? 'pdf' : 'txt';
                this.saveBlob(blob, `workrh-dashboard-${this.currentYear}-${String(this.currentMonth).padStart(2, '0')}.${extension}`);
                this.exportLoading.set(null);
            },
            error: () => this.exportLoading.set(null)
        });
    }
    loadAdvancedData(viewModel) {
        if (viewModel.subscription.entitlements.includes('MONTHLY_STATS')) {
            this.api.getMonthlyStats(this.currentYear).subscribe({
                next: (response) => this.monthlyStats.set(response),
                error: () => this.monthlyStats.set(null)
            });
        }
        if (viewModel.subscription.entitlements.includes('EMAIL_NOTIFICATIONS')) {
            this.api.getNotifications().subscribe({
                next: (response) => this.notifications.set(response),
                error: () => this.notifications.set([])
            });
        }
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
DashboardPageComponent = __decorate([
    Component({
        selector: 'app-dashboard-page',
        standalone: true,
        imports: [CommonModule, LoadingSkeletonComponent],
        templateUrl: './dashboard-page.component.html',
        styleUrl: './page-styles.css'
    })
], DashboardPageComponent);
export { DashboardPageComponent };
