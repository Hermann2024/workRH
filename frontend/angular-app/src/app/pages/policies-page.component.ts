import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { catchError, forkJoin, of } from 'rxjs';
import { AuthService } from '../auth.service';
import {
  TeleworkPolicyRequest,
  TeleworkPolicyResponse,
  WorkRhApiService,
  WorkRhVm
} from '../workrh-api.service';

interface PolicyFormValue {
  countryCode: string;
  annualFiscalLimitDays: number;
  weeklyCompanyLimitDays: number;
  standardDailyWorkMinutes: number;
  socialSecurityStandardThresholdPercent: number;
  socialSecurityFrameworkThresholdPercent: number;
  shortActivityToleranceMinutes: number;
  weeklyLimitEnabled: boolean;
  socialSecurityFrameworkAgreementEligible: boolean;
  partialDayCountsAsFullDay: boolean;
  thresholdProrated: boolean;
  thirdCountryDaysCounted: boolean;
  taxRuleLabel: string;
  legalReference: string;
  taxSourceUrl: string;
  socialSecuritySourceUrl: string;
  notes: string;
  active: boolean;
}

@Component({
  selector: 'app-policies-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './policies-page.component.html',
  styleUrl: './page-styles.css'
})
export class PoliciesPageComponent {
  private readonly api = inject(WorkRhApiService);
  private readonly authService = inject(AuthService);
  private readonly formBuilder = inject(FormBuilder);

  readonly loading = signal(true);
  readonly loadError = signal<string | null>(null);
  readonly saving = signal(false);
  readonly vm = signal<WorkRhVm | null>(null);
  readonly policies = signal<TeleworkPolicyResponse[]>([]);
  readonly selectedCountryCode = signal('');
  readonly policyMessage = signal<string | null>(null);
  readonly policyError = signal<string | null>(null);
  readonly isAdmin = computed(() => this.authService.hasRole('ADMIN'));
  readonly companySummary = computed(() => this.vm()?.companySummary ?? null);
  readonly selectedPolicy = computed(() => {
    const countryCode = this.selectedCountryCode();
    return this.policies().find((policy) => policy.countryCode === countryCode) ?? null;
  });
  readonly policyStats = computed(() => {
    const summary = this.companySummary();
    if (!summary) {
      return [];
    }

    return [
      { label: 'Employés suivis', value: summary.totalEmployeesTracked },
      { label: 'Jours annuels utilisés', value: summary.totalAnnualUsedDays },
      { label: 'Alerte fiscale', value: summary.totalEmployeesOverFiscalLimit },
      { label: 'Alerte hebdo', value: summary.totalEmployeesOverWeeklyPolicy }
    ];
  });
  readonly selectedPolicyLabel = computed(() => {
    const policy = this.selectedPolicy();
    if (!policy) {
      return 'Nouvelle règle';
    }
    return `${policy.countryCode} ${policy.id ? 'personnalisée' : 'standard'}`;
  });
  readonly policyForm = this.formBuilder.nonNullable.group({
    countryCode: ['', [Validators.required, Validators.minLength(2)]],
    annualFiscalLimitDays: [34, [Validators.required, Validators.min(1)]],
    weeklyCompanyLimitDays: [2, [Validators.required, Validators.min(0)]],
    standardDailyWorkMinutes: [480, [Validators.required, Validators.min(60)]],
    socialSecurityStandardThresholdPercent: [25, [Validators.required, Validators.min(1)]],
    socialSecurityFrameworkThresholdPercent: [49, [Validators.required, Validators.min(1)]],
    shortActivityToleranceMinutes: [0, [Validators.required, Validators.min(0)]],
    weeklyLimitEnabled: [true],
    socialSecurityFrameworkAgreementEligible: [true],
    partialDayCountsAsFullDay: [true],
    thresholdProrated: [false],
    thirdCountryDaysCounted: [true],
    taxRuleLabel: [''],
    legalReference: [''],
    taxSourceUrl: [''],
    socialSecuritySourceUrl: [''],
    notes: [''],
    active: [true]
  });

  constructor() {
    forkJoin({
      viewModel: this.api.loadViewModel(),
      policies: this.api.getTeleworkPolicies().pipe(
        catchError(() => {
          this.policyError.set('Impossible de charger les règles de télétravail pour le moment.');
          return of([] as TeleworkPolicyResponse[]);
        })
      )
    }).subscribe({
      next: ({ viewModel, policies }) => {
        this.vm.set(viewModel);
        this.loadError.set(null);
        this.policies.set(policies);
        this.selectPolicy(policies[0]?.countryCode ?? 'DEFAULT');
        this.loading.set(false);
      },
      error: (error) => {
        this.loadError.set(this.readBackendMessage(error, 'Impossible de charger les regles sans donnees reelles.'));
        this.loading.set(false);
      }
    });
  }

  selectPolicy(countryCode: string): void {
    const normalizedCountryCode = countryCode.trim().toUpperCase();
    const policy = this.policies().find((item) => item.countryCode === normalizedCountryCode) ?? null;
    this.selectedCountryCode.set(normalizedCountryCode);
    this.policyMessage.set(null);
    this.policyError.set(null);
    this.policyForm.reset(this.toFormValue(policy ?? this.createDraftPolicy(normalizedCountryCode)));
  }

  startNewPolicy(): void {
    this.selectedCountryCode.set('');
    this.policyMessage.set(null);
    this.policyError.set(null);
    this.policyForm.reset(this.toFormValue(this.createDraftPolicy('')));
  }

  savePolicy(): void {
    if (!this.isAdmin()) {
      return;
    }
    if (this.saving()) {
      return;
    }
    if (this.policyForm.invalid) {
      this.policyForm.markAllAsTouched();
      this.policyError.set("Complétez les champs obligatoires avant d'enregistrer la règle.");
      return;
    }

    const currentSelection = this.selectedPolicy();
    const request = this.toRequest();
    const normalizedCountryCode = request.countryCode.trim().toUpperCase();
    const matchingPolicy = this.policies().find((policy) => policy.countryCode === normalizedCountryCode) ?? null;
    const targetPolicy = currentSelection?.id != null ? currentSelection : matchingPolicy;

    this.saving.set(true);
    this.policyMessage.set(null);
    this.policyError.set(null);

    const save$ = targetPolicy?.id != null
      ? this.api.updateTeleworkPolicy(targetPolicy.id, request)
      : this.api.createTeleworkPolicy(request);

    save$.subscribe({
      next: () => {
        this.saving.set(false);
        this.policyMessage.set(`La règle ${normalizedCountryCode} a été enregistrée.`);
        this.reloadPolicies(normalizedCountryCode);
      },
      error: (error) => {
        this.saving.set(false);
        this.policyError.set(error?.error?.message || "Impossible d'enregistrer la règle.");
      }
    });
  }

  resetPolicyForm(): void {
    const policy = this.selectedPolicy();
    this.policyMessage.set(null);
    this.policyError.set(null);
    this.policyForm.reset(this.toFormValue(policy ?? this.createDraftPolicy(this.selectedCountryCode())));
  }

  policySourceLabel(policy: TeleworkPolicyResponse): string {
    return policy.id ? 'Personnalisée' : 'Catalogue par défaut';
  }

  private reloadPolicies(countryCodeToSelect: string): void {
    this.api.getTeleworkPolicies().subscribe({
      next: (policies) => {
        this.policies.set(policies);
        this.selectPolicy(countryCodeToSelect);
      },
      error: () => {
        this.policyError.set("La règle a été enregistrée, mais la liste n'a pas pu être rafraîchie.");
      }
    });
  }

  private toRequest(): TeleworkPolicyRequest {
    const raw = this.policyForm.getRawValue();
    return {
      ...raw,
      countryCode: raw.countryCode.trim().toUpperCase()
    };
  }

  private toFormValue(policy: TeleworkPolicyResponse): PolicyFormValue {
    return {
      countryCode: policy.countryCode,
      annualFiscalLimitDays: policy.annualFiscalLimitDays,
      weeklyCompanyLimitDays: policy.weeklyCompanyLimitDays,
      standardDailyWorkMinutes: policy.standardDailyWorkMinutes,
      socialSecurityStandardThresholdPercent: policy.socialSecurityStandardThresholdPercent,
      socialSecurityFrameworkThresholdPercent: policy.socialSecurityFrameworkThresholdPercent,
      shortActivityToleranceMinutes: policy.shortActivityToleranceMinutes,
      weeklyLimitEnabled: policy.weeklyLimitEnabled,
      socialSecurityFrameworkAgreementEligible: policy.socialSecurityFrameworkAgreementEligible,
      partialDayCountsAsFullDay: policy.partialDayCountsAsFullDay,
      thresholdProrated: policy.thresholdProrated,
      thirdCountryDaysCounted: policy.thirdCountryDaysCounted,
      taxRuleLabel: policy.taxRuleLabel ?? '',
      legalReference: policy.legalReference ?? '',
      taxSourceUrl: policy.taxSourceUrl ?? '',
      socialSecuritySourceUrl: policy.socialSecuritySourceUrl ?? '',
      notes: policy.notes ?? '',
      active: policy.active
    };
  }

  private createDraftPolicy(countryCode: string): TeleworkPolicyResponse {
    return {
      id: null,
      countryCode,
      annualFiscalLimitDays: 34,
      weeklyCompanyLimitDays: 2,
      standardDailyWorkMinutes: 480,
      socialSecurityStandardThresholdPercent: 25,
      socialSecurityFrameworkThresholdPercent: 49,
      shortActivityToleranceMinutes: 0,
      weeklyLimitEnabled: true,
      socialSecurityFrameworkAgreementEligible: true,
      partialDayCountsAsFullDay: true,
      thresholdProrated: false,
      thirdCountryDaysCounted: true,
      taxRuleLabel: '',
      legalReference: '',
      taxSourceUrl: '',
      socialSecuritySourceUrl: '',
      notes: '',
      active: true,
      createdAt: null,
      updatedAt: null
    };
  }

  private readBackendMessage(error: unknown, fallback: string): string {
    const backendMessage = (error as { error?: { message?: string } })?.error?.message;
    return typeof backendMessage === 'string' && backendMessage.trim()
      ? backendMessage
      : fallback;
  }
}
