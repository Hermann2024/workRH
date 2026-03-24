import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, forkJoin, of } from 'rxjs';
import { API_BASE_URL } from './config';

export interface DashboardEmployeeItem {
  employeeId: number;
  usedDays: number;
  annualUsedDays: number;
  remainingDays: number;
  weeklyUsedDays: number;
  annualFiscalLimitExceeded: boolean;
  weeklyCompanyLimitExceeded: boolean;
}

export interface DashboardResponse {
  totalEmployeesTracked: number;
  totalUsedDays: number;
  totalRemainingDays: number;
  fiscalAlerts: number;
  weeklyAlerts: number;
  employees: DashboardEmployeeItem[];
}

export interface PlanResponse {
  id: number;
  code: 'STARTER' | 'PRO' | 'PREMIUM' | 'ENTERPRISE';
  name: string;
  monthlyPrice: number;
  stripePriceId?: string | null;
  billingCycle: 'MONTHLY';
  minEmployees: number | null;
  maxEmployees: number | null;
  recommended: boolean;
  customPricing: boolean;
  active: boolean;
  features: string[];
}

export interface SubscriptionResponse {
  id: number;
  tenantId: string;
  planCode: string;
  pendingPlanCode?: string | null;
  status: string;
  seatsPurchased: number;
  smsOptionEnabled: boolean;
  advancedAuditOptionEnabled: boolean;
  advancedExportOptionEnabled: boolean;
  cancelAtPeriodEnd?: boolean;
  cancellationReason?: string | null;
  startsAt: string;
  renewsAt: string;
  cancelledAt?: string | null;
  stripeCustomerEmail?: string | null;
  stripeCheckoutSessionId?: string | null;
  stripeSubscriptionId?: string | null;
  entitlements: string[];
}

export interface CompanySummaryResponse {
  totalEmployeesTracked: number;
  totalAnnualUsedDays: number;
  totalAnnualRemainingDays: number;
  totalEmployeesOverFiscalLimit: number;
  totalEmployeesOverWeeklyPolicy: number;
  employees: Array<{
    employeeId: number;
    annualUsedDays: number;
    annualRemainingDays: number;
    monthUsedDays: number;
    annualFiscalLimitExceeded: boolean;
    policy: {
      countryCode: string;
      annualFiscalLimitDays: number;
      annualFiscalRemainingDays: number;
      weeklyCompanyLimitDays: number;
      weeklyLimitEnabled: boolean;
      weeklyUsedDays: number;
      annualFiscalLimitExceeded: boolean;
      weeklyCompanyLimitExceeded: boolean;
    };
  }>;
}

export interface WorkRhVm {
  dashboard: DashboardResponse;
  plans: PlanResponse[];
  subscription: SubscriptionResponse;
  companySummary: CompanySummaryResponse;
}

@Injectable({ providedIn: 'root' })
export class WorkRhApiService {
  private readonly http = inject(HttpClient);

  loadViewModel(): Observable<WorkRhVm> {
    return forkJoin({
      dashboard: this.http.get<DashboardResponse>(`${API_BASE_URL}/api/reports/dashboard?year=2026&month=3`)
        .pipe(catchError(() => of(this.fallbackDashboard()))),
      plans: this.http.get<PlanResponse[]>(`${API_BASE_URL}/api/subscriptions/plans`)
        .pipe(catchError(() => of(this.fallbackPlans()))),
      subscription: this.http.get<SubscriptionResponse>(`${API_BASE_URL}/api/subscriptions/current`)
        .pipe(catchError(() => of(this.fallbackSubscription()))),
      companySummary: this.http.get<CompanySummaryResponse>(`${API_BASE_URL}/api/telework/company-summary?year=2026&month=3&countryCode=FR`)
        .pipe(catchError(() => of(this.fallbackCompanySummary())))
    });
  }

  private fallbackDashboard(): DashboardResponse {
    return {
      totalEmployeesTracked: 3,
      totalUsedDays: 11,
      totalRemainingDays: 67,
      fiscalAlerts: 1,
      weeklyAlerts: 1,
      employees: [
        { employeeId: 101, usedDays: 4, annualUsedDays: 21, remainingDays: 13, weeklyUsedDays: 2, annualFiscalLimitExceeded: false, weeklyCompanyLimitExceeded: false },
        { employeeId: 102, usedDays: 5, annualUsedDays: 35, remainingDays: 0, weeklyUsedDays: 3, annualFiscalLimitExceeded: true, weeklyCompanyLimitExceeded: true },
        { employeeId: 103, usedDays: 2, annualUsedDays: 14, remainingDays: 20, weeklyUsedDays: 1, annualFiscalLimitExceeded: false, weeklyCompanyLimitExceeded: false }
      ]
    };
  }

  private fallbackPlans(): PlanResponse[] {
    return [
      {
        id: 1,
        code: 'STARTER',
        name: 'Starter',
        monthlyPrice: 49,
        stripePriceId: null,
        billingCycle: 'MONTHLY',
        minEmployees: 1,
        maxEmployees: 10,
        recommended: false,
        customPricing: false,
        active: true,
        features: ['EMPLOYEE_MANAGEMENT', 'LEAVE_MANAGEMENT', 'TELEWORK_BASIC', 'DASHBOARD_BASIC', 'EMAIL_SUPPORT']
      },
      {
        id: 2,
        code: 'PRO',
        name: 'Pro',
        monthlyPrice: 99,
        stripePriceId: null,
        billingCycle: 'MONTHLY',
        minEmployees: 10,
        maxEmployees: 50,
        recommended: true,
        customPricing: false,
        active: true,
        features: ['SICKNESS_MANAGEMENT', 'TELEWORK_COMPLIANCE_34', 'AUTO_EXCLUSION', 'THRESHOLD_ALERTS', 'DASHBOARD_ADVANCED', 'MONTHLY_STATS', 'EXPORTS', 'EMAIL_NOTIFICATIONS', 'PRIORITY_SUPPORT']
      },
      {
        id: 3,
        code: 'PREMIUM',
        name: 'Premium',
        monthlyPrice: 199,
        stripePriceId: null,
        billingCycle: 'MONTHLY',
        minEmployees: 50,
        maxEmployees: null,
        recommended: false,
        customPricing: false,
        active: true,
        features: ['ADVANCED_RBAC', 'FULL_REPORTING', 'DECLARATION_AUDIT', 'PUBLIC_API', 'SMS_NOTIFICATIONS', 'COMPANY_BRANDING', 'ACCOUNTING_EXPORT', 'SLA_SUPPORT', 'ONBOARDING_SUPPORT']
      },
      {
        id: 4,
        code: 'ENTERPRISE',
        name: 'Enterprise',
        monthlyPrice: 0,
        stripePriceId: null,
        billingCycle: 'MONTHLY',
        minEmployees: null,
        maxEmployees: null,
        recommended: false,
        customPricing: true,
        active: true,
        features: ['MULTI_TENANT_ADVANCED', 'DEDICATED_HOSTING', 'HARDENED_SECURITY', 'SSO', 'CUSTOM_DEVELOPMENT']
      }
    ];
  }

  private fallbackSubscription(): SubscriptionResponse {
    return {
      id: 1,
      tenantId: 'demo-lu',
      planCode: 'PRO',
      pendingPlanCode: null,
      status: 'ACTIVE',
      seatsPurchased: 25,
      smsOptionEnabled: false,
      advancedAuditOptionEnabled: false,
      advancedExportOptionEnabled: true,
      cancelAtPeriodEnd: false,
      cancellationReason: null,
      startsAt: '2026-03-01',
      renewsAt: '2026-04-01',
      cancelledAt: null,
      stripeCustomerEmail: 'rh@company.com',
      stripeCheckoutSessionId: 'cs_test_123',
      stripeSubscriptionId: 'sub_test_123',
      entitlements: [
        'EMPLOYEE_MANAGEMENT',
        'LEAVE_MANAGEMENT',
        'TELEWORK_BASIC',
        'SICKNESS_MANAGEMENT',
        'TELEWORK_COMPLIANCE_34',
        'AUTO_EXCLUSION',
        'THRESHOLD_ALERTS',
        'DASHBOARD_ADVANCED',
        'MONTHLY_STATS',
        'EXPORTS',
        'EMAIL_NOTIFICATIONS',
        'PRIORITY_SUPPORT'
      ]
    };
  }

  private fallbackCompanySummary(): CompanySummaryResponse {
    return {
      totalEmployeesTracked: 3,
      totalAnnualUsedDays: 70,
      totalAnnualRemainingDays: 33,
      totalEmployeesOverFiscalLimit: 1,
      totalEmployeesOverWeeklyPolicy: 1,
      employees: [
        {
          employeeId: 101,
          annualUsedDays: 21,
          annualRemainingDays: 13,
          monthUsedDays: 4,
          annualFiscalLimitExceeded: false,
          policy: {
            countryCode: 'FR',
            annualFiscalLimitDays: 34,
            annualFiscalRemainingDays: 13,
            weeklyCompanyLimitDays: 2,
            weeklyLimitEnabled: true,
            weeklyUsedDays: 2,
            annualFiscalLimitExceeded: false,
            weeklyCompanyLimitExceeded: false
          }
        },
        {
          employeeId: 102,
          annualUsedDays: 35,
          annualRemainingDays: 0,
          monthUsedDays: 5,
          annualFiscalLimitExceeded: true,
          policy: {
            countryCode: 'FR',
            annualFiscalLimitDays: 34,
            annualFiscalRemainingDays: 0,
            weeklyCompanyLimitDays: 2,
            weeklyLimitEnabled: true,
            weeklyUsedDays: 3,
            annualFiscalLimitExceeded: true,
            weeklyCompanyLimitExceeded: true
          }
        },
        {
          employeeId: 103,
          annualUsedDays: 14,
          annualRemainingDays: 20,
          monthUsedDays: 2,
          annualFiscalLimitExceeded: false,
          policy: {
            countryCode: 'FR',
            annualFiscalLimitDays: 34,
            annualFiscalRemainingDays: 20,
            weeklyCompanyLimitDays: 2,
            weeklyLimitEnabled: true,
            weeklyUsedDays: 1,
            annualFiscalLimitExceeded: false,
            weeklyCompanyLimitExceeded: false
          }
        }
      ]
    };
  }
}
