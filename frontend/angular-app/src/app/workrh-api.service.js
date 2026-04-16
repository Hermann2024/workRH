var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { catchError, forkJoin, map, of, switchMap } from 'rxjs';
import { API_BASE_URL } from './config';
let WorkRhApiService = class WorkRhApiService {
    constructor() {
        this.http = inject(HttpClient);
    }
    loadViewModel(referenceDate = new Date()) {
        const year = referenceDate.getFullYear();
        const month = referenceDate.getMonth() + 1;
        return forkJoin({
            plans: this.http.get(`${API_BASE_URL}/api/subscriptions/plans`)
                .pipe(catchError(() => of(this.fallbackPlans()))),
            subscription: this.http.get(`${API_BASE_URL}/api/subscriptions/current`)
                .pipe(catchError(() => of(this.fallbackSubscription()))),
        }).pipe(map((viewModel) => ({
            ...viewModel,
            plans: this.normalizePlans(viewModel.plans)
        })), switchMap((viewModel) => forkJoin({
            dashboard: this.getDashboard(year, month).pipe(catchError(() => of(this.fallbackDashboard()))),
            companySummary: viewModel.subscription.entitlements.includes('DASHBOARD_ADVANCED')
                ? this.getCompanySummary(year, month, 'FR').pipe(catchError(() => of(null)))
                : of(null)
        }).pipe(map((extra) => ({
            ...viewModel,
            ...extra
        })))));
    }
    loadEmployeeWorkspace(referenceDate = new Date()) {
        const year = referenceDate.getFullYear();
        const month = referenceDate.getMonth() + 1;
        return this.getCurrentProfile().pipe(switchMap((profile) => forkJoin({
            teleworkHistory: this.getCurrentEmployeeTeleworkHistory().pipe(catchError(() => of([]))),
            leaves: this.getCurrentEmployeeLeaves().pipe(catchError(() => of([]))),
            sickness: this.getCurrentEmployeeSickness().pipe(catchError(() => of([]))),
            compliance: this.checkFeature('TELEWORK_COMPLIANCE_34').pipe(catchError(() => of({
                tenantId: profile.email,
                feature: 'TELEWORK_COMPLIANCE_34',
                allowed: false,
                reason: 'Feature unavailable',
                planCode: 'STARTER'
            })))
        }).pipe(switchMap((base) => {
            const summary$ = base.compliance.allowed
                ? this.getTeleworkSummary(profile.id, year, month, profile.countryOfResidence ?? 'FR').pipe(catchError(() => of(null)))
                : of(null);
            return summary$.pipe(map((teleworkSummary) => ({
                profile,
                teleworkHistory: base.teleworkHistory,
                teleworkSummary,
                leaves: base.leaves,
                sickness: base.sickness,
                complianceAvailable: base.compliance.allowed
            })));
        }))));
    }
    getCurrentProfile() {
        return this.http.get(`${API_BASE_URL}/api/users/me`);
    }
    getDashboard(year, month) {
        return this.http.get(`${API_BASE_URL}/api/reports/dashboard?year=${year}&month=${month}`);
    }
    getMonthlyStats(year) {
        return this.http.get(`${API_BASE_URL}/api/reports/monthly-stats?year=${year}`);
    }
    checkFeature(feature) {
        return this.http.get(`${API_BASE_URL}/api/subscriptions/features/check?feature=${feature}`);
    }
    getCompanySummary(year, month, countryCode) {
        const suffix = countryCode ? `&countryCode=${countryCode}` : '';
        return this.http.get(`${API_BASE_URL}/api/telework/company-summary?year=${year}&month=${month}${suffix}`);
    }
    getCurrentEmployeeTeleworkHistory() {
        return this.http.get(`${API_BASE_URL}/api/telework/me/history`);
    }
    getTeleworkSummary(employeeId, year, month, countryCode) {
        const suffix = countryCode ? `&countryCode=${countryCode}` : '';
        return this.http.get(`${API_BASE_URL}/api/telework/summary/${employeeId}?year=${year}&month=${month}${suffix}`);
    }
    declareTelework(request) {
        return this.http.post(`${API_BASE_URL}/api/telework`, request);
    }
    getCurrentEmployeeLeaves() {
        return this.http.get(`${API_BASE_URL}/api/leaves/me`);
    }
    createLeaveRequest(request) {
        return this.http.post(`${API_BASE_URL}/api/leaves`, request);
    }
    cancelLeave(leaveId, comment) {
        return this.http.post(`${API_BASE_URL}/api/leaves/${leaveId}/cancel`, { comment });
    }
    getCurrentEmployeeSickness() {
        return this.http.get(`${API_BASE_URL}/api/sickness/me`);
    }
    declareSickness(request) {
        return this.http.post(`${API_BASE_URL}/api/sickness`, request);
    }
    getNotifications() {
        return this.http.get(`${API_BASE_URL}/api/notifications`);
    }
    getSupportTickets() {
        return this.http.get(`${API_BASE_URL}/api/support/tickets`);
    }
    getSlaTickets() {
        return this.http.get(`${API_BASE_URL}/api/support/tickets/sla`);
    }
    createSupportTicket(mode, request) {
        const pathByMode = {
            standard: '/api/support/tickets',
            priority: '/api/support/tickets/priority',
            onboarding: '/api/support/tickets/onboarding',
            sso: '/api/support/tickets/sso',
            security: '/api/support/tickets/security',
            hosting: '/api/support/tickets/hosting',
            'custom-development': '/api/support/tickets/custom-development',
            integration: '/api/support/tickets/integration'
        };
        return this.http.post(`${API_BASE_URL}${pathByMode[mode]}`, request);
    }
    sendSms(request) {
        return this.http.post(`${API_BASE_URL}/api/notifications/sms`, request);
    }
    downloadDashboardExport(format, year, month) {
        return this.http.get(`${API_BASE_URL}/api/reports/dashboard/export/${format}?year=${year}&month=${month}`, {
            responseType: 'blob'
        });
    }
    downloadAccountingExport(year, month) {
        const params = [
            year !== undefined ? `year=${year}` : null,
            month !== undefined ? `month=${month}` : null
        ].filter((value) => value !== null);
        const query = params.length ? `?${params.join('&')}` : '';
        return this.http.get(`${API_BASE_URL}/api/subscriptions/invoices/accounting-export/csv${query}`, {
            responseType: 'blob'
        });
    }
    fallbackDashboard() {
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
    fallbackPlans() {
        return [
            {
                id: 1,
                code: 'STARTER',
                name: 'Starter',
                monthlyPrice: 199,
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
                monthlyPrice: 299,
                stripePriceId: null,
                billingCycle: 'MONTHLY',
                minEmployees: 10,
                maxEmployees: 50,
                recommended: true,
                customPricing: false,
                active: true,
                features: ['EMPLOYEE_MANAGEMENT', 'LEAVE_MANAGEMENT', 'TELEWORK_BASIC', 'DASHBOARD_BASIC', 'EMAIL_SUPPORT', 'SICKNESS_MANAGEMENT', 'TELEWORK_COMPLIANCE_34', 'AUTO_EXCLUSION', 'THRESHOLD_ALERTS', 'DASHBOARD_ADVANCED', 'MONTHLY_STATS', 'EXPORTS', 'EMAIL_NOTIFICATIONS', 'PRIORITY_SUPPORT']
            },
            {
                id: 3,
                code: 'PREMIUM',
                name: 'Premium',
                monthlyPrice: 399,
                stripePriceId: null,
                billingCycle: 'MONTHLY',
                minEmployees: 50,
                maxEmployees: null,
                recommended: false,
                customPricing: false,
                active: true,
                features: ['EMPLOYEE_MANAGEMENT', 'LEAVE_MANAGEMENT', 'TELEWORK_BASIC', 'DASHBOARD_BASIC', 'EMAIL_SUPPORT', 'SICKNESS_MANAGEMENT', 'TELEWORK_COMPLIANCE_34', 'AUTO_EXCLUSION', 'THRESHOLD_ALERTS', 'DASHBOARD_ADVANCED', 'MONTHLY_STATS', 'EXPORTS', 'EMAIL_NOTIFICATIONS', 'PRIORITY_SUPPORT', 'DECLARATION_AUDIT', 'SMS_NOTIFICATIONS', 'ACCOUNTING_EXPORT', 'SLA_SUPPORT', 'ONBOARDING_SUPPORT']
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
                features: ['EMPLOYEE_MANAGEMENT', 'LEAVE_MANAGEMENT', 'TELEWORK_BASIC', 'DASHBOARD_BASIC', 'EMAIL_SUPPORT', 'SICKNESS_MANAGEMENT', 'TELEWORK_COMPLIANCE_34', 'AUTO_EXCLUSION', 'THRESHOLD_ALERTS', 'DASHBOARD_ADVANCED', 'MONTHLY_STATS', 'EXPORTS', 'EMAIL_NOTIFICATIONS', 'PRIORITY_SUPPORT', 'DECLARATION_AUDIT', 'SMS_NOTIFICATIONS', 'ACCOUNTING_EXPORT', 'SLA_SUPPORT', 'ONBOARDING_SUPPORT', 'PUBLIC_API', 'DEDICATED_HOSTING', 'SSO', 'HARDENED_SECURITY', 'CUSTOM_DEVELOPMENT']
            }
        ];
    }
    fallbackSubscription() {
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
            previewAllFeaturesActive: false,
            entitlements: [
                'EMPLOYEE_MANAGEMENT',
                'LEAVE_MANAGEMENT',
                'TELEWORK_BASIC',
                'DASHBOARD_BASIC',
                'EMAIL_SUPPORT',
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
    fallbackCompanySummary() {
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
                        standardDailyWorkMinutes: 480,
                        weeklyLimitEnabled: true,
                        socialSecurityStandardThresholdPercent: 25,
                        socialSecurityFrameworkThresholdPercent: 49,
                        shortActivityToleranceMinutes: 0,
                        socialSecurityFrameworkAgreementEligible: true,
                        partialDayCountsAsFullDay: true,
                        thresholdProrated: false,
                        thirdCountryDaysCounted: true,
                        taxRuleLabel: 'Luxembourg-France : tolerance fiscale de 34 jours',
                        legalReference: 'FAQ ACD non-residents ; accord-cadre CCSS teletravail',
                        taxSourceUrl: 'https://impotsdirects.public.lu/fr/az/t/teletravail.html',
                        socialSecuritySourceUrl: 'https://ccss.public.lu/fr/employeurs/secteur-prive/activite-etranger/accord-teletravail.html',
                        notes: 'Les jours hors Luxembourg restent suivis dans le seuil fiscal annuel.',
                        weeklyUsedDays: 2,
                        annualFiscalLimitExceeded: false,
                        weeklyCompanyLimitExceeded: false
                    },
                    fiscal: {
                        thresholdUnitsUsed: 21,
                        thresholdDaysUsed: 21,
                        thresholdDaysRemaining: 13,
                        thresholdLimitDays: 34,
                        residenceTeleworkDays: 21,
                        residenceOtherWorkDays: 0,
                        otherForeignWorkDays: 0,
                        totalTrackedWorkMinutes: 48960,
                        luxembourgWorkMinutes: 38880,
                        outsideLuxembourgWorkMinutes: 10080,
                        luxembourgTaxableWorkMinutes: 48960,
                        foreignTaxableWorkMinutes: 0,
                        luxembourgTaxableSharePercent: 100,
                        foreignTaxableSharePercent: 0,
                        thresholdExceeded: false,
                        toleranceRuleApplied: true,
                        partialDayCountsAsFullDay: true,
                        taxRuleLabel: 'Luxembourg-France : tolerance fiscale de 34 jours',
                        explanation: 'Le seuil annuel n est pas depasse, donc le Luxembourg garde l imposition du salaire entier sur le temps suivi.'
                    },
                    socialSecurity: {
                        totalRelevantWorkMinutes: 10080,
                        sicknessMinutesIncluded: 0,
                        residenceTeleworkMinutes: 10080,
                        residenceOtherWorkMinutes: 0,
                        otherForeignWorkMinutes: 0,
                        residenceTeleworkPercent: 40,
                        residenceActivityPercent: 40,
                        crossBorderActivityDetected: true,
                        frameworkAgreementApplicable: true,
                        article13Required: false,
                        a1Required: true,
                        evaluationMode: 'FRAMEWORK_AGREEMENT',
                        likelyApplicableLegislationCountryCode: 'LU',
                        certificatePath: 'CCSS_FRAMEWORK_AGREEMENT_A1',
                        maxRetroactivityMonths: 3,
                        recommendedDeclarationMonths: 12,
                        warnings: ['Since July 1, 2024, CCSS retroactivity is limited to 3 months.'],
                        explanation: 'Le teletravail de residence reste entre 25% et moins de 50% du temps pertinent.'
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
                        standardDailyWorkMinutes: 480,
                        weeklyLimitEnabled: true,
                        socialSecurityStandardThresholdPercent: 25,
                        socialSecurityFrameworkThresholdPercent: 49,
                        shortActivityToleranceMinutes: 0,
                        socialSecurityFrameworkAgreementEligible: true,
                        partialDayCountsAsFullDay: true,
                        thresholdProrated: false,
                        thirdCountryDaysCounted: true,
                        taxRuleLabel: 'Luxembourg-France : tolerance fiscale de 34 jours',
                        legalReference: 'FAQ ACD non-residents ; accord-cadre CCSS teletravail',
                        taxSourceUrl: 'https://impotsdirects.public.lu/fr/az/t/teletravail.html',
                        socialSecuritySourceUrl: 'https://ccss.public.lu/fr/employeurs/secteur-prive/activite-etranger/accord-teletravail.html',
                        notes: 'Les jours hors Luxembourg restent suivis dans le seuil fiscal annuel.',
                        weeklyUsedDays: 3,
                        annualFiscalLimitExceeded: true,
                        weeklyCompanyLimitExceeded: true
                    },
                    fiscal: {
                        thresholdUnitsUsed: 35,
                        thresholdDaysUsed: 35,
                        thresholdDaysRemaining: 0,
                        thresholdLimitDays: 34,
                        residenceTeleworkDays: 35,
                        residenceOtherWorkDays: 0,
                        otherForeignWorkDays: 0,
                        totalTrackedWorkMinutes: 48960,
                        luxembourgWorkMinutes: 32160,
                        outsideLuxembourgWorkMinutes: 16800,
                        luxembourgTaxableWorkMinutes: 32160,
                        foreignTaxableWorkMinutes: 16800,
                        luxembourgTaxableSharePercent: 65.71,
                        foreignTaxableSharePercent: 34.29,
                        thresholdExceeded: true,
                        toleranceRuleApplied: true,
                        partialDayCountsAsFullDay: true,
                        taxRuleLabel: 'Luxembourg-France : tolerance fiscale de 34 jours',
                        explanation: 'Le seuil annuel est depasse, donc le temps de travail hors Luxembourg doit etre alloue hors Luxembourg.'
                    },
                    socialSecurity: {
                        totalRelevantWorkMinutes: 13440,
                        sicknessMinutesIncluded: 0,
                        residenceTeleworkMinutes: 9600,
                        residenceOtherWorkMinutes: 0,
                        otherForeignWorkMinutes: 0,
                        residenceTeleworkPercent: 71.43,
                        residenceActivityPercent: 71.43,
                        crossBorderActivityDetected: true,
                        frameworkAgreementApplicable: false,
                        article13Required: true,
                        a1Required: true,
                        evaluationMode: 'ARTICLE_13_RESIDENCE_STATE',
                        likelyApplicableLegislationCountryCode: 'FR',
                        certificatePath: 'ARTICLE_13_RESIDENCE_STATE_DETERMINATION',
                        maxRetroactivityMonths: 3,
                        recommendedDeclarationMonths: 12,
                        warnings: ['Since July 1, 2024, CCSS retroactivity is limited to 3 months.'],
                        explanation: 'Une part substantielle du travail est effectuee dans l etat de residence.'
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
                        standardDailyWorkMinutes: 480,
                        weeklyLimitEnabled: true,
                        socialSecurityStandardThresholdPercent: 25,
                        socialSecurityFrameworkThresholdPercent: 49,
                        shortActivityToleranceMinutes: 0,
                        socialSecurityFrameworkAgreementEligible: true,
                        partialDayCountsAsFullDay: true,
                        thresholdProrated: false,
                        thirdCountryDaysCounted: true,
                        taxRuleLabel: 'Luxembourg-France : tolerance fiscale de 34 jours',
                        legalReference: 'FAQ ACD non-residents ; accord-cadre CCSS teletravail',
                        taxSourceUrl: 'https://impotsdirects.public.lu/fr/az/t/teletravail.html',
                        socialSecuritySourceUrl: 'https://ccss.public.lu/fr/employeurs/secteur-prive/activite-etranger/accord-teletravail.html',
                        notes: 'Les jours hors Luxembourg restent suivis dans le seuil fiscal annuel.',
                        weeklyUsedDays: 1,
                        annualFiscalLimitExceeded: false,
                        weeklyCompanyLimitExceeded: false
                    },
                    fiscal: {
                        thresholdUnitsUsed: 14,
                        thresholdDaysUsed: 14,
                        thresholdDaysRemaining: 20,
                        thresholdLimitDays: 34,
                        residenceTeleworkDays: 12,
                        residenceOtherWorkDays: 1,
                        otherForeignWorkDays: 1,
                        totalTrackedWorkMinutes: 48960,
                        luxembourgWorkMinutes: 41280,
                        outsideLuxembourgWorkMinutes: 7680,
                        luxembourgTaxableWorkMinutes: 48960,
                        foreignTaxableWorkMinutes: 0,
                        luxembourgTaxableSharePercent: 100,
                        foreignTaxableSharePercent: 0,
                        thresholdExceeded: false,
                        toleranceRuleApplied: true,
                        partialDayCountsAsFullDay: true,
                        taxRuleLabel: 'Luxembourg-France : tolerance fiscale de 34 jours',
                        explanation: 'Le seuil annuel n est pas depasse, donc le Luxembourg garde l imposition du salaire entier sur le temps suivi.'
                    },
                    socialSecurity: {
                        totalRelevantWorkMinutes: 9600,
                        sicknessMinutesIncluded: 480,
                        residenceTeleworkMinutes: 3840,
                        residenceOtherWorkMinutes: 480,
                        otherForeignWorkMinutes: 480,
                        residenceTeleworkPercent: 40,
                        residenceActivityPercent: 45,
                        crossBorderActivityDetected: true,
                        frameworkAgreementApplicable: false,
                        article13Required: true,
                        a1Required: true,
                        evaluationMode: 'ARTICLE_13_RESIDENCE_STATE',
                        likelyApplicableLegislationCountryCode: 'FR',
                        certificatePath: 'ARTICLE_13_RESIDENCE_STATE_DETERMINATION',
                        maxRetroactivityMonths: 3,
                        recommendedDeclarationMonths: 12,
                        warnings: [
                            'Since July 1, 2024, CCSS retroactivity is limited to 3 months.',
                            'Other habitual work carried out in the state of residence excludes the framework agreement.',
                            'Activity in another country besides Luxembourg and the residence state excludes the framework agreement.'
                        ],
                        explanation: 'La pluriactivite doit etre instruite via la procedure Article 13.'
                    }
                }
            ]
        };
    }
    normalizePlans(plans) {
        return plans.map((plan) => {
            if (plan.code === 'STARTER') {
                return {
                    ...plan,
                    name: 'Starter',
                    monthlyPrice: 199,
                    recommended: false,
                    customPricing: false
                };
            }
            if (plan.code === 'PRO') {
                return {
                    ...plan,
                    name: 'Pro',
                    monthlyPrice: 299,
                    recommended: true,
                    customPricing: false
                };
            }
            if (plan.code === 'PREMIUM') {
                return {
                    ...plan,
                    name: 'Premium',
                    monthlyPrice: 399,
                    recommended: false,
                    customPricing: false
                };
            }
            return plan;
        });
    }
};
WorkRhApiService = __decorate([
    Injectable({ providedIn: 'root' })
], WorkRhApiService);
export { WorkRhApiService };
