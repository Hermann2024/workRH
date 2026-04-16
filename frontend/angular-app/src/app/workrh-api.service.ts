import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, forkJoin, map, of, switchMap } from 'rxjs';
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

export interface EmployeeProfileResponse {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  countryOfResidence: string | null;
  phoneNumber: string | null;
  department: string | null;
  jobTitle: string | null;
  crossBorderWorker: boolean;
  hireDate: string | null;
  active: boolean;
  roles: string[];
  createdAt: string;
  updatedAt: string;
}

export interface EmployeeCreateRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  countryOfResidence?: string | null;
  phoneNumber?: string | null;
  department?: string | null;
  jobTitle?: string | null;
  crossBorderWorker: boolean;
  hireDate?: string | null;
  roles: string[];
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
  previewAllFeaturesActive: boolean;
  entitlements: string[];
}

export interface FeatureCheckResponse {
  tenantId: string;
  feature: string;
  allowed: boolean;
  reason: string;
  planCode: string;
}

export interface NotificationResponse {
  id: number;
  employeeId: number | null;
  channel: string;
  subject: string;
  payload: string;
  sentAt: string;
}

export interface MonthlyStatItemResponse {
  month: number;
  usedDays: number;
  remainingDays: number;
  fiscalAlerts: number;
  weeklyAlerts: number;
  employeesTracked: number;
}

export interface MonthlyStatsResponse {
  year: number;
  trackedEmployees: number;
  peakUsedDays: number;
  totalAlertMonths: number;
  months: MonthlyStatItemResponse[];
}

export interface SupportTicketResponse {
  id: number;
  category: string;
  priority: string;
  status: string;
  requesterName: string | null;
  requesterEmail: string;
  phoneNumber: string | null;
  subject: string;
  message: string;
  slaDueAt: string | null;
  createdAt: string;
  slaBreached: boolean;
}

export interface SlaTicketResponse {
  id: number;
  subject: string;
  priority: string;
  status: string;
  slaDueAt: string;
  breached: boolean;
  remainingMinutes: number;
}

export interface SmsNotificationResponse {
  provider: string;
  status: string;
  sentAt: string;
  phoneNumber: string;
}

export interface TeleworkPolicySnapshotResponse {
  countryCode: string;
  annualFiscalLimitDays: number;
  annualFiscalRemainingDays: number;
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
  weeklyUsedDays: number;
  annualFiscalLimitExceeded: boolean;
  weeklyCompanyLimitExceeded: boolean;
}

export interface TeleworkPolicyRequest {
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

export interface TeleworkPolicyResponse {
  id: number | null;
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
  createdAt: string | null;
  updatedAt: string | null;
}

export interface TeleworkFiscalSummaryResponse {
  thresholdUnitsUsed: number;
  thresholdDaysUsed: number;
  thresholdDaysRemaining: number;
  thresholdLimitDays: number;
  residenceTeleworkDays: number;
  residenceOtherWorkDays: number;
  otherForeignWorkDays: number;
  totalTrackedWorkMinutes: number;
  luxembourgWorkMinutes: number;
  outsideLuxembourgWorkMinutes: number;
  luxembourgTaxableWorkMinutes: number;
  foreignTaxableWorkMinutes: number;
  luxembourgTaxableSharePercent: number;
  foreignTaxableSharePercent: number;
  thresholdExceeded: boolean;
  toleranceRuleApplied: boolean;
  partialDayCountsAsFullDay: boolean;
  taxRuleLabel: string;
  explanation: string;
}

export interface TeleworkSocialSecuritySummaryResponse {
  totalRelevantWorkMinutes: number;
  sicknessMinutesIncluded: number;
  residenceTeleworkMinutes: number;
  residenceOtherWorkMinutes: number;
  otherForeignWorkMinutes: number;
  residenceTeleworkPercent: number;
  residenceActivityPercent: number;
  crossBorderActivityDetected: boolean;
  frameworkAgreementApplicable: boolean;
  article13Required: boolean;
  a1Required: boolean;
  evaluationMode: string;
  likelyApplicableLegislationCountryCode: string;
  certificatePath: string;
  maxRetroactivityMonths: number;
  recommendedDeclarationMonths: number;
  warnings: string[];
  explanation: string;
}

export interface TeleworkSummaryResponse {
  employeeId: number;
  annualUsedDays: number;
  annualRemainingDays: number;
  monthUsedDays: number;
  annualFiscalLimitExceeded: boolean;
  policy: TeleworkPolicySnapshotResponse;
  fiscal: TeleworkFiscalSummaryResponse;
  socialSecurity: TeleworkSocialSecuritySummaryResponse;
}

export interface TeleworkDeclarationResponse {
  id: number;
  employeeId: number;
  workDate: string;
  countryCode: string;
  status: string;
  totalWorkMinutes: number;
  residenceTeleworkMinutes: number;
  residenceNonTeleworkMinutes: number;
  otherForeignWorkMinutes: number;
  otherForeignCountryCode: string | null;
  connectedToEmployerInfrastructure: boolean;
  countsTowardFiscalThreshold: boolean;
  countsTowardSocialSecurityTelework: boolean;
  monthUsedDays: number;
  annualUsedDays: number;
  annualRemainingDays: number;
  weeklyUsedDays: number;
  annualFiscalLimitExceeded: boolean;
  weeklyCompanyLimitExceeded: boolean;
}

export type LeaveType =
  | 'PAID'
  | 'UNPAID'
  | 'PATERNITY'
  | 'MOVING'
  | 'MARRIAGE'
  | 'BIRTH_OR_ADOPTION'
  | 'FAMILY_CARE';

export interface LeaveResponse {
  id: number;
  employeeId: number;
  type: LeaveType;
  status: string;
  startDate: string;
  endDate: string;
  comment: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface SicknessResponse {
  id: number;
  employeeId: number;
  startDate: string;
  endDate: string;
  comment: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface EmployeeWorkspaceVm {
  profile: EmployeeProfileResponse;
  teleworkHistory: TeleworkDeclarationResponse[];
  teleworkSummary: TeleworkSummaryResponse | null;
  leaves: LeaveResponse[];
  sickness: SicknessResponse[];
  complianceAvailable: boolean;
}

export interface CompanySummaryResponse {
  totalEmployeesTracked: number;
  totalAnnualUsedDays: number;
  totalAnnualRemainingDays: number;
  totalEmployeesOverFiscalLimit: number;
  totalEmployeesOverWeeklyPolicy: number;
  employees: TeleworkSummaryResponse[];
}

export interface WorkRhVm {
  dashboard: DashboardResponse;
  plans: PlanResponse[];
  subscription: SubscriptionResponse;
  companySummary: CompanySummaryResponse | null;
}

@Injectable({ providedIn: 'root' })
export class WorkRhApiService {
  private readonly http = inject(HttpClient);

  loadViewModel(referenceDate = new Date()): Observable<WorkRhVm> {
    const year = referenceDate.getFullYear();
    const month = referenceDate.getMonth() + 1;

    return forkJoin({
      plans: this.http.get<PlanResponse[]>(`${API_BASE_URL}/api/subscriptions/plans`),
      subscription: this.http.get<SubscriptionResponse>(`${API_BASE_URL}/api/subscriptions/current`),
    }).pipe(
      map((viewModel) => ({
        ...viewModel,
        plans: this.normalizePlans(viewModel.plans)
      })),
      switchMap((viewModel) =>
        forkJoin({
          dashboard: this.getDashboard(year, month),
          companySummary: viewModel.subscription.entitlements.includes('DASHBOARD_ADVANCED')
            ? this.getCompanySummary(year, month, 'FR')
            : of(null)
        }).pipe(
          map((extra) => ({
            ...viewModel,
            ...extra
          }))
        )
      )
    );
  }

  loadEmployeeWorkspace(referenceDate = new Date()): Observable<EmployeeWorkspaceVm> {
    const year = referenceDate.getFullYear();
    const month = referenceDate.getMonth() + 1;

    return this.getCurrentProfile().pipe(
      switchMap((profile) =>
        forkJoin({
          teleworkHistory: this.getCurrentEmployeeTeleworkHistory(),
          leaves: this.getCurrentEmployeeLeaves(),
          sickness: this.getCurrentEmployeeSickness(),
          compliance: this.checkFeature('TELEWORK_COMPLIANCE_34')
        }).pipe(
          switchMap((base) => {
            const summary$: Observable<TeleworkSummaryResponse | null> = base.compliance.allowed
              ? this.getTeleworkSummary(profile.id, year, month, profile.countryOfResidence ?? 'FR')
              : of(null);

            return summary$.pipe(
              map((teleworkSummary) => ({
                profile,
                teleworkHistory: base.teleworkHistory,
                teleworkSummary,
                leaves: base.leaves,
                sickness: base.sickness,
                complianceAvailable: base.compliance.allowed
              }))
            );
          })
        )
      )
    );
  }

  getCurrentProfile(): Observable<EmployeeProfileResponse> {
    return this.http.get<EmployeeProfileResponse>(`${API_BASE_URL}/api/users/me`);
  }

  getEmployees(): Observable<EmployeeProfileResponse[]> {
    return this.http.get<EmployeeProfileResponse[]>(`${API_BASE_URL}/api/users`);
  }

  createEmployee(request: EmployeeCreateRequest): Observable<EmployeeProfileResponse> {
    return this.http.post<EmployeeProfileResponse>(`${API_BASE_URL}/api/users`, request);
  }

  activateEmployee(employeeId: number): Observable<EmployeeProfileResponse> {
    return this.http.patch<EmployeeProfileResponse>(`${API_BASE_URL}/api/users/${employeeId}/activate`, {});
  }

  deactivateEmployee(employeeId: number): Observable<EmployeeProfileResponse> {
    return this.http.patch<EmployeeProfileResponse>(`${API_BASE_URL}/api/users/${employeeId}/deactivate`, {});
  }

  getDashboard(year: number, month: number): Observable<DashboardResponse> {
    return this.http.get<DashboardResponse>(`${API_BASE_URL}/api/reports/dashboard?year=${year}&month=${month}`);
  }

  getMonthlyStats(year: number): Observable<MonthlyStatsResponse> {
    return this.http.get<MonthlyStatsResponse>(`${API_BASE_URL}/api/reports/monthly-stats?year=${year}`);
  }

  checkFeature(feature: string): Observable<FeatureCheckResponse> {
    return this.http.get<FeatureCheckResponse>(`${API_BASE_URL}/api/subscriptions/features/check?feature=${feature}`);
  }

  getCompanySummary(year: number, month: number, countryCode?: string | null): Observable<CompanySummaryResponse> {
    const suffix = countryCode ? `&countryCode=${countryCode}` : '';
    return this.http.get<CompanySummaryResponse>(`${API_BASE_URL}/api/telework/company-summary?year=${year}&month=${month}${suffix}`);
  }

  getTeleworkPolicies(): Observable<TeleworkPolicyResponse[]> {
    return this.http.get<TeleworkPolicyResponse[]>(`${API_BASE_URL}/api/telework/policies`);
  }

  getEffectiveTeleworkPolicy(countryCode?: string | null): Observable<TeleworkPolicyResponse> {
    const suffix = countryCode ? `?countryCode=${countryCode}` : '';
    return this.http.get<TeleworkPolicyResponse>(`${API_BASE_URL}/api/telework/policies/effective${suffix}`);
  }

  createTeleworkPolicy(request: TeleworkPolicyRequest): Observable<TeleworkPolicyResponse> {
    return this.http.post<TeleworkPolicyResponse>(`${API_BASE_URL}/api/telework/policies`, request);
  }

  updateTeleworkPolicy(policyId: number, request: TeleworkPolicyRequest): Observable<TeleworkPolicyResponse> {
    return this.http.put<TeleworkPolicyResponse>(`${API_BASE_URL}/api/telework/policies/${policyId}`, request);
  }

  getCurrentEmployeeTeleworkHistory(): Observable<TeleworkDeclarationResponse[]> {
    return this.http.get<TeleworkDeclarationResponse[]>(`${API_BASE_URL}/api/telework/me/history`);
  }

  getTeleworkHistory(employeeId: number): Observable<TeleworkDeclarationResponse[]> {
    return this.http.get<TeleworkDeclarationResponse[]>(`${API_BASE_URL}/api/telework/history/${employeeId}`);
  }

  getTeleworkSummary(employeeId: number, year: number, month: number, countryCode?: string | null): Observable<TeleworkSummaryResponse> {
    const suffix = countryCode ? `&countryCode=${countryCode}` : '';
    return this.http.get<TeleworkSummaryResponse>(
      `${API_BASE_URL}/api/telework/summary/${employeeId}?year=${year}&month=${month}${suffix}`
    );
  }

  declareTelework(request: {
    employeeId: number;
    workDate: string;
    countryCode: string;
    totalWorkMinutes?: number;
    residenceTeleworkMinutes?: number;
    residenceNonTeleworkMinutes?: number;
    otherForeignWorkMinutes?: number;
    otherForeignCountryCode?: string | null;
    connectedToEmployerInfrastructure?: boolean;
  }): Observable<TeleworkDeclarationResponse> {
    return this.http.post<TeleworkDeclarationResponse>(`${API_BASE_URL}/api/telework`, request);
  }

  getCurrentEmployeeLeaves(): Observable<LeaveResponse[]> {
    return this.http.get<LeaveResponse[]>(`${API_BASE_URL}/api/leaves/me`);
  }

  getLeaves(employeeId?: number | null): Observable<LeaveResponse[]> {
    const suffix = employeeId != null ? `?employeeId=${employeeId}` : '';
    return this.http.get<LeaveResponse[]>(`${API_BASE_URL}/api/leaves${suffix}`);
  }

  createLeaveRequest(request: {
    employeeId: number;
    type: LeaveType;
    startDate: string;
    endDate: string;
    comment: string;
  }): Observable<LeaveResponse> {
    return this.http.post<LeaveResponse>(`${API_BASE_URL}/api/leaves`, request);
  }

  cancelLeave(leaveId: number, comment: string): Observable<LeaveResponse> {
    return this.http.post<LeaveResponse>(`${API_BASE_URL}/api/leaves/${leaveId}/cancel`, { comment });
  }

  approveLeave(leaveId: number, comment: string): Observable<LeaveResponse> {
    return this.http.post<LeaveResponse>(`${API_BASE_URL}/api/leaves/${leaveId}/approve`, { comment });
  }

  rejectLeave(leaveId: number, comment: string): Observable<LeaveResponse> {
    return this.http.post<LeaveResponse>(`${API_BASE_URL}/api/leaves/${leaveId}/reject`, { comment });
  }

  getCurrentEmployeeSickness(): Observable<SicknessResponse[]> {
    return this.http.get<SicknessResponse[]>(`${API_BASE_URL}/api/sickness/me`);
  }

  getSickness(employeeId?: number | null): Observable<SicknessResponse[]> {
    const suffix = employeeId != null ? `?employeeId=${employeeId}` : '';
    return this.http.get<SicknessResponse[]>(`${API_BASE_URL}/api/sickness${suffix}`);
  }

  declareSickness(request: {
    employeeId: number;
    startDate: string;
    endDate: string;
    comment: string;
  }): Observable<SicknessResponse> {
    return this.http.post<SicknessResponse>(`${API_BASE_URL}/api/sickness`, request);
  }

  getNotifications(): Observable<NotificationResponse[]> {
    return this.http.get<NotificationResponse[]>(`${API_BASE_URL}/api/notifications`);
  }

  getSupportTickets(): Observable<SupportTicketResponse[]> {
    return this.http.get<SupportTicketResponse[]>(`${API_BASE_URL}/api/support/tickets`);
  }

  getSlaTickets(): Observable<SlaTicketResponse[]> {
    return this.http.get<SlaTicketResponse[]>(`${API_BASE_URL}/api/support/tickets/sla`);
  }

  createSupportTicket(
    mode: 'standard' | 'priority' | 'onboarding' | 'sso' | 'security' | 'hosting' | 'custom-development' | 'integration',
    request: {
      requesterName: string;
      requesterEmail: string;
      phoneNumber?: string | null;
      subject: string;
      message: string;
    }
  ): Observable<SupportTicketResponse> {
    const pathByMode: Record<string, string> = {
      standard: '/api/support/tickets',
      priority: '/api/support/tickets/priority',
      onboarding: '/api/support/tickets/onboarding',
      sso: '/api/support/tickets/sso',
      security: '/api/support/tickets/security',
      hosting: '/api/support/tickets/hosting',
      'custom-development': '/api/support/tickets/custom-development',
      integration: '/api/support/tickets/integration'
    };
    return this.http.post<SupportTicketResponse>(`${API_BASE_URL}${pathByMode[mode]}`, request);
  }

  sendSms(request: {
    employeeId?: number | null;
    phoneNumber: string;
    message: string;
  }): Observable<SmsNotificationResponse> {
    return this.http.post<SmsNotificationResponse>(`${API_BASE_URL}/api/notifications/sms`, request);
  }

  downloadDashboardExport(format: 'csv' | 'pdf' | 'pdf-placeholder', year: number, month: number): Observable<Blob> {
    return this.http.get(`${API_BASE_URL}/api/reports/dashboard/export/${format}?year=${year}&month=${month}`, {
      responseType: 'blob'
    });
  }

  downloadAccountingExport(year?: number, month?: number): Observable<Blob> {
    const params = [
      year !== undefined ? `year=${year}` : null,
      month !== undefined ? `month=${month}` : null
    ].filter((value): value is string => value !== null);
    const query = params.length ? `?${params.join('&')}` : '';
    return this.http.get(`${API_BASE_URL}/api/subscriptions/invoices/accounting-export/csv${query}`, {
      responseType: 'blob'
    });
  }

  private normalizePlans(plans: PlanResponse[]): PlanResponse[] {
    const planOrder: Record<PlanResponse['code'], number> = {
      STARTER: 0,
      PRO: 1,
      PREMIUM: 2,
      ENTERPRISE: 3
    };

    return [...plans].sort((left, right) => planOrder[left.code] - planOrder[right.code]);
  }
}

