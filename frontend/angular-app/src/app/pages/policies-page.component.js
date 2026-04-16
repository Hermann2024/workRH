var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { WorkRhApiService } from '../workrh-api.service';
let PoliciesPageComponent = class PoliciesPageComponent {
    constructor() {
        this.api = inject(WorkRhApiService);
        this.loading = signal(true);
        this.vm = signal(null);
        this.policyStats = computed(() => {
            const summary = this.vm()?.companySummary;
            if (!summary) {
                return [];
            }
            return [
                { label: 'Employes suivis', value: summary.totalEmployeesTracked },
                { label: 'Jours annuels utilises', value: summary.totalAnnualUsedDays },
                { label: 'Alerte fiscale', value: summary.totalEmployeesOverFiscalLimit },
                { label: 'Alerte hebdo', value: summary.totalEmployeesOverWeeklyPolicy }
            ];
        });
        this.api.loadViewModel().subscribe({
            next: (viewModel) => {
                this.vm.set(viewModel);
                this.loading.set(false);
            },
            error: () => this.loading.set(false)
        });
    }
};
PoliciesPageComponent = __decorate([
    Component({
        selector: 'app-policies-page',
        standalone: true,
        imports: [CommonModule],
        templateUrl: './policies-page.component.html',
        styleUrl: './page-styles.css'
    })
], PoliciesPageComponent);
export { PoliciesPageComponent };
