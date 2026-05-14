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
  annualAlertLevel: 'OK' | 'WARNING' | 'EXCEEDED';
  annualAlertLabel: string;
  riskScorePercent: number;
  riskLevel: 'GREEN' | 'ORANGE' | 'RED';
  riskLabel: string;
  annualFiscalLimitExceeded: boolean;
  weeklyCompanyLimitExceeded: boolean;
}

export type EmploymentContractType = 'CDI' | 'CDD' | 'STAGE' | 'ALTERNANCE' | 'AUTRES';
export type EmployeeGender = 'MASCULIN' | 'FEMININ' | 'AUTRES';

export interface EmployeeProfileResponse {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  countryOfResidence: string | null;
  phoneNumber: string | null;
  department: string | null;
  jobTitle: string | null;
  birthDate: string | null;
  gender: EmployeeGender;
  contractType: EmploymentContractType;
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
  birthDate?: string | null;
  gender: EmployeeGender;
  contractType: EmploymentContractType;
  crossBorderWorker: boolean;
  hireDate?: string | null;
  roles: string[];
}

export interface EmployeeUpdateRequest {
  email: string;
  firstName: string;
  lastName: string;
  countryOfResidence?: string | null;
  phoneNumber?: string | null;
  department?: string | null;
  jobTitle?: string | null;
  birthDate?: string | null;
  gender: EmployeeGender;
  contractType: EmploymentContractType;
  crossBorderWorker: boolean;
  hireDate?: string | null;
  roles: string[];
  active: boolean;
}

export interface EmployeeInvitationRequest {
  email: string;
  firstName: string;
  lastName: string;
  countryOfResidence?: string | null;
  phoneNumber?: string | null;
  department?: string | null;
  jobTitle?: string | null;
  birthDate?: string | null;
  gender?: EmployeeGender | null;
  contractType?: EmploymentContractType | null;
  hireDate?: string | null;
}

export interface EmployeeInvitationResponse {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  countryOfResidence: string | null;
  phoneNumber: string | null;
  department: string | null;
  jobTitle: string | null;
  birthDate: string | null;
  gender: EmployeeGender;
  contractType: EmploymentContractType;
  hireDate: string | null;
  token: string | null;
  emailSent: boolean;
  status: 'PENDING' | 'ACCEPTED' | 'EXPIRED';
  expiresAt: string | null;
  acceptedAt: string | null;
  createdAt: string;
}

export interface TenantWorkspaceResponse {
  tenantId: string;
  companyName: string;
  ownerEmail: string | null;
  planCode: string | null;
  seatsPurchased: number | null;
  seatsUsed: number;
  activeEmployees: number;
  seatLimitExceeded: boolean;
  active: boolean;
  createdAt: string | null;
  updatedAt: string | null;
}

export interface PlatformWorkspaceResponse {
  tenantId: string;
  companyName: string;
  ownerEmail: string | null;
  planCode: string | null;
  seatsPurchased: number | null;
  seatsUsed: number;
  activeEmployees: number;
  seatLimitExceeded: boolean;
  active: boolean;
  createdAt: string | null;
  updatedAt: string | null;
}

export interface DashboardResponse {
  totalEmployeesTracked: number;
  totalUsedDays: number;
  totalRemainingDays: number;
  annualAlerts: number;
  fiscalAlerts: number;
  weeklyAlerts: number;
  employees: DashboardEmployeeItem[];
}

export interface TaxSimulationRequest {
  employeeId: number;
  annualGrossSalary: number;
  annualContractWorkDays: number;
}

export interface TaxSimulationResponse {
  employeeId: number;
  year: number;
  month: number;
  annualTeleworkDays: number;
  annualFiscalLimitDays: number;
  annualContractWorkDays: number;
  thresholdExceeded: boolean;
  annualGrossSalary: number;
  salaryPerWorkDay: number;
  luxembourgTaxableSalary: number;
  foreignTaxableSalary: number;
  calculationRule: string;
  disclaimer: string;
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

export interface CatalogReadinessResponse {
  stripeCheckoutAvailable: boolean;
  supportAcknowledgementEmailAvailable: boolean;
  smsDeliveryAvailable: boolean;
  enterprisePlanAvailable: boolean;
  warnings: string[];
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
  tenantId: string;
  category: string;
  priority: string;
  status: string;
  requesterName: string | null;
  requesterEmail: string;
  phoneNumber: string | null;
  subject: string;
  message: string;
  resolutionMessage: string | null;
  resolvedBy: string | null;
  slaDueAt: string | null;
  resolvedAt: string | null;
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

export interface ServiceModuleResponse {
  feature: string;
  name: string;
  category: string;
  description: string;
  backendScope: string;
  actionLabel: string;
  actionRoute: string;
  enabled: boolean;
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

export interface TeleworkComplianceChecklistItem {
  code: string;
  label: string;
  status: string;
  severity: string;
  detail: string;
  sourceUrl: string | null;
}

export interface TeleworkComplianceEmployeeRisk {
  employeeId: number;
  countryCode: string;
  annualUsedDays: number;
  annualLimitDays: number;
  fiscalLimitExceeded: boolean;
  a1Required: boolean;
  article13Required: boolean;
  likelyApplicableLegislationCountryCode: string;
  riskLevel: string;
  recommendation: string;
}

export interface TeleworkComplianceDossierResponse {
  year: number;
  month: number;
  countryCode: string;
  generatedAt: string;
  overallStatus: string;
  employeesReviewed: number;
  declarationsReviewed: number;
  fiscalAlerts: number;
  socialSecurityAlerts: number;
  a1DeclarationsRequired: number;
  article13Cases: number;
  checklist: TeleworkComplianceChecklistItem[];
  employeeRisks: TeleworkComplianceEmployeeRisk[];
  officialSources: string[];
}

export type ComplianceCaseStatus = 'DRAFT' | 'IN_REVIEW' | 'ACTION_REQUIRED' | 'READY_FOR_PAYROLL' | 'CLOSED';
export type ComplianceStepStatus = 'NOT_STARTED' | 'TO_PREPARE' | 'SUBMITTED' | 'VALIDATED' | 'EXPIRED' | 'NOT_APPLICABLE';
export type EmploymentSector = 'PRIVATE' | 'PUBLIC';
export type SituationChangeType = 'RESIDENCE_COUNTRY_CHANGE' | 'ADDRESS_CHANGE' | 'SECONDARY_ACTIVITY' | 'TELEWORK_RATE_CHANGE' | 'EMPLOYMENT_SECTOR_CHANGE' | 'OTHER';

export interface TeleworkComplianceEvidenceResponse {
  id: number;
  evidenceType: string;
  label: string;
  reference: string | null;
  fileUrl: string | null;
  uploadedBy: string;
  uploadedAt: string;
}

export interface TeleworkSituationChangeResponse {
  id: number;
  employeeId: number;
  type: SituationChangeType;
  effectiveDate: string;
  previousValue: string | null;
  newValue: string | null;
  reason: string | null;
  recordedBy: string;
  recordedAt: string;
}

export interface TeleworkComplianceAuditEntryResponse {
  id: number;
  complianceCaseId: number;
  employeeId: number;
  action: string;
  actor: string;
  beforeValue: string | null;
  afterValue: string | null;
  createdAt: string;
}

export interface TeleworkComplianceCaseResponse {
  id: number;
  employeeId: number;
  year: number;
  month: number;
  countryCode: string;
  employmentSector: EmploymentSector;
  status: ComplianceCaseStatus;
  ccssA1Status: ComplianceStepStatus;
  teleworkAgreementStatus: ComplianceStepStatus;
  equipmentStatus: ComplianceStepStatus;
  healthSafetyStatus: ComplianceStepStatus;
  accidentCoverageStatus: ComplianceStepStatus;
  dataProtectionStatus: ComplianceStepStatus;
  residenceCountryRulesStatus: ComplianceStepStatus;
  legalWatchStatus: ComplianceStepStatus;
  a1SubmittedAt: string | null;
  a1ValidUntil: string | null;
  legalSourcesReviewedAt: string | null;
  nextLegalReviewAt: string | null;
  legalSourcesVersion: string | null;
  notes: string | null;
  validatedBy: string | null;
  validatedAt: string | null;
  createdAt: string;
  updatedAt: string;
  evidence: TeleworkComplianceEvidenceResponse[];
  situationChanges: TeleworkSituationChangeResponse[];
  auditTrail: TeleworkComplianceAuditEntryResponse[];
}

export interface TeleworkComplianceCaseRequest {
  employeeId: number;
  year: number;
  month: number;
  countryCode: string;
  employmentSector: EmploymentSector;
  status: ComplianceCaseStatus;
  ccssA1Status: ComplianceStepStatus;
  teleworkAgreementStatus: ComplianceStepStatus;
  equipmentStatus: ComplianceStepStatus;
  healthSafetyStatus: ComplianceStepStatus;
  accidentCoverageStatus: ComplianceStepStatus;
  dataProtectionStatus: ComplianceStepStatus;
  residenceCountryRulesStatus: ComplianceStepStatus;
  legalWatchStatus: ComplianceStepStatus;
  a1SubmittedAt?: string | null;
  a1ValidUntil?: string | null;
  legalSourcesReviewedAt?: string | null;
  nextLegalReviewAt?: string | null;
  legalSourcesVersion?: string | null;
  notes?: string | null;
}

export type LeaveType =
  | 'PAID'
  | 'UNPAID'
  | 'PATERNITY'
  | 'MOVING'
  | 'MARRIAGE'
  | 'BIRTH_OR_ADOPTION'
  | 'FAMILY_CARE'
  | 'BEREAVEMENT'
  | 'MEDICAL_APPOINTMENT'
  | 'TRAINING'
  | 'ADMINISTRATIVE'
  | 'OTHER';

export interface LeaveResponse {
  id: number;
  employeeId: number;
  type: LeaveType;
  status: string;
  startDate: string;
  endDate: string;
  comment: string | null;
  evidenceRequired: boolean;
  evidenceUploaded: boolean;
  evidenceFileName: string | null;
  evidenceUploadedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface SicknessResponse {
  id: number;
  employeeId: number;
  startDate: string;
  endDate: string;
  comment: string | null;
  evidenceRequired: boolean;
  evidenceUploaded: boolean;
  evidenceFileName: string | null;
  evidenceUploadedAt: string | null;
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
  complianceDossier: TeleworkComplianceDossierResponse | null;
}

@Injectable({ providedIn: 'root' })
export class WorkRhApiService {
  private readonly http = inject(HttpClient);

  getPlans(): Observable<PlanResponse[]> {
    return this.http.get<PlanResponse[]>(`${API_BASE_URL}/api/subscriptions/plans`).pipe(
      map((plans) => this.normalizePlans(plans))
    );
  }

  getCurrentSubscription(): Observable<SubscriptionResponse> {
    return this.http.get<SubscriptionResponse>(`${API_BASE_URL}/api/subscriptions/current`);
  }

  getCatalogReadiness(): Observable<CatalogReadinessResponse> {
    return this.http.get<CatalogReadinessResponse>(`${API_BASE_URL}/api/subscriptions/catalog/readiness`);
  }

  getServiceModules(): Observable<ServiceModuleResponse[]> {
    return this.http.get<ServiceModuleResponse[]>(`${API_BASE_URL}/api/subscriptions/services`);
  }

  loadViewModel(referenceDate = new Date()): Observable<WorkRhVm> {
    const year = referenceDate.getFullYear();
    const month = referenceDate.getMonth() + 1;

    return forkJoin({
      plans: this.getPlans(),
      subscription: this.getCurrentSubscription(),
    }).pipe(
      switchMap((viewModel) =>
        forkJoin({
          dashboard: this.getDashboard(year, month),
          companySummary: viewModel.subscription.entitlements.includes('DASHBOARD_ADVANCED')
            ? this.getCompanySummary(year, month, 'FR')
            : of(null),
          complianceDossier: viewModel.subscription.entitlements.includes('DECLARATION_AUDIT')
            ? this.getTeleworkComplianceDossier(year, month, 'FR')
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

  loadEmployeeWorkspace(referenceDate = new Date(), employeeId?: number | null): Observable<EmployeeWorkspaceVm> {
    const year = referenceDate.getFullYear();
    const month = referenceDate.getMonth() + 1;

    return this.getEmployeeProfile(employeeId).pipe(
      switchMap((profile) =>
        forkJoin({
          leaves: employeeId != null ? this.getLeaves(employeeId) : this.getCurrentEmployeeLeaves(),
          sickness: employeeId != null ? this.getSickness(employeeId) : this.getCurrentEmployeeSickness(),
          compliance: this.checkFeature('TELEWORK_COMPLIANCE_34'),
          audit: this.checkFeature('DECLARATION_AUDIT')
        }).pipe(
          switchMap((base) => {
            const summary$: Observable<TeleworkSummaryResponse | null> = base.compliance.allowed
              ? this.getTeleworkSummary(profile.id, year, month, profile.countryOfResidence ?? 'FR')
              : of(null);
            const teleworkHistory$: Observable<TeleworkDeclarationResponse[]> = employeeId != null
              ? base.audit.allowed ? this.getTeleworkHistory(employeeId) : of([])
              : this.getCurrentEmployeeTeleworkHistory();

            return forkJoin({
              teleworkSummary: summary$,
              teleworkHistory: teleworkHistory$
            }).pipe(
              map(({ teleworkSummary, teleworkHistory }) => ({
                profile,
                teleworkHistory,
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

  getEmployee(employeeId: number): Observable<EmployeeProfileResponse> {
    return this.http.get<EmployeeProfileResponse>(`${API_BASE_URL}/api/users/${employeeId}`);
  }

  getEmployees(): Observable<EmployeeProfileResponse[]> {
    return this.http.get<EmployeeProfileResponse[]>(`${API_BASE_URL}/api/users`);
  }

  getWorkspace(): Observable<TenantWorkspaceResponse> {
    return this.http.get<TenantWorkspaceResponse>(`${API_BASE_URL}/api/users/workspace`);
  }

  updateWorkspace(request: { companyName: string }): Observable<TenantWorkspaceResponse> {
    return this.http.put<TenantWorkspaceResponse>(`${API_BASE_URL}/api/users/workspace`, request);
  }

  createEmployee(request: EmployeeCreateRequest): Observable<EmployeeProfileResponse> {
    return this.http.post<EmployeeProfileResponse>(`${API_BASE_URL}/api/users`, request);
  }

  getEmployeeInvitations(): Observable<EmployeeInvitationResponse[]> {
    return this.http.get<EmployeeInvitationResponse[]>(`${API_BASE_URL}/api/users/invitations`);
  }

  inviteEmployee(request: EmployeeInvitationRequest): Observable<EmployeeInvitationResponse> {
    return this.http.post<EmployeeInvitationResponse>(`${API_BASE_URL}/api/users/invitations`, request);
  }

  updateEmployee(employeeId: number, request: EmployeeUpdateRequest): Observable<EmployeeProfileResponse> {
    return this.http.put<EmployeeProfileResponse>(`${API_BASE_URL}/api/users/${employeeId}`, request);
  }

  deleteEmployee(employeeId: number): Observable<void> {
    return this.http.delete<void>(`${API_BASE_URL}/api/users/${employeeId}`);
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

  simulateTeleworkTaxImpact(year: number, month: number, request: TaxSimulationRequest): Observable<TaxSimulationResponse> {
    return this.http.post<TaxSimulationResponse>(`${API_BASE_URL}/api/reports/tax-simulation?year=${year}&month=${month}`, request);
  }

  checkFeature(feature: string): Observable<FeatureCheckResponse> {
    return this.http.get<FeatureCheckResponse>(`${API_BASE_URL}/api/subscriptions/features/check?feature=${feature}`);
  }

  getCompanySummary(year: number, month: number, countryCode?: string | null): Observable<CompanySummaryResponse> {
    const suffix = countryCode ? `&countryCode=${countryCode}` : '';
    return this.http.get<CompanySummaryResponse>(`${API_BASE_URL}/api/telework/company-summary?year=${year}&month=${month}${suffix}`);
  }

  getTeleworkComplianceDossier(year: number, month: number, countryCode?: string | null): Observable<TeleworkComplianceDossierResponse> {
    const suffix = countryCode ? `&countryCode=${countryCode}` : '';
    return this.http.get<TeleworkComplianceDossierResponse>(
      `${API_BASE_URL}/api/telework/compliance-dossier?year=${year}&month=${month}${suffix}`
    );
  }

  getTeleworkComplianceCases(year: number, month: number): Observable<TeleworkComplianceCaseResponse[]> {
    return this.http.get<TeleworkComplianceCaseResponse[]>(`${API_BASE_URL}/api/telework/compliance/cases?year=${year}&month=${month}`);
  }

  saveTeleworkComplianceCase(request: TeleworkComplianceCaseRequest): Observable<TeleworkComplianceCaseResponse> {
    return this.http.post<TeleworkComplianceCaseResponse>(`${API_BASE_URL}/api/telework/compliance/cases`, request);
  }

  addTeleworkComplianceEvidence(caseId: number, request: {
    evidenceType: string;
    label: string;
    reference?: string | null;
    fileUrl?: string | null;
  }): Observable<TeleworkComplianceCaseResponse> {
    return this.http.post<TeleworkComplianceCaseResponse>(`${API_BASE_URL}/api/telework/compliance/cases/${caseId}/evidence`, request);
  }

  validateTeleworkComplianceCase(caseId: number): Observable<TeleworkComplianceCaseResponse> {
    return this.http.post<TeleworkComplianceCaseResponse>(`${API_BASE_URL}/api/telework/compliance/cases/${caseId}/validate`, {});
  }

  recordTeleworkSituationChange(request: {
    employeeId: number;
    type: SituationChangeType;
    effectiveDate: string;
    previousValue?: string | null;
    newValue?: string | null;
    reason?: string | null;
  }): Observable<TeleworkSituationChangeResponse> {
    return this.http.post<TeleworkSituationChangeResponse>(`${API_BASE_URL}/api/telework/compliance/situation-changes`, request);
  }

  downloadTeleworkComplianceCases(year: number, month: number): Observable<Blob> {
    return this.http.get(`${API_BASE_URL}/api/telework/compliance/cases/export.csv?year=${year}&month=${month}`, {
      responseType: 'blob'
    });
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

  getRecentTeleworkDeclarations(): Observable<TeleworkDeclarationResponse[]> {
    return this.http.get<TeleworkDeclarationResponse[]>(`${API_BASE_URL}/api/telework/recent`);
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

  uploadLeaveEvidence(leaveId: number, file: File): Observable<LeaveResponse> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<LeaveResponse>(`${API_BASE_URL}/api/leaves/${leaveId}/evidence`, formData);
  }

  downloadLeaveEvidence(leaveId: number): Observable<Blob> {
    return this.http.get(`${API_BASE_URL}/api/leaves/${leaveId}/evidence`, { responseType: 'blob' });
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

  uploadSicknessEvidence(sicknessId: number, file: File): Observable<SicknessResponse> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<SicknessResponse>(`${API_BASE_URL}/api/sickness/${sicknessId}/evidence`, formData);
  }

  downloadSicknessEvidence(sicknessId: number): Observable<Blob> {
    return this.http.get(`${API_BASE_URL}/api/sickness/${sicknessId}/evidence`, { responseType: 'blob' });
  }

  getNotifications(): Observable<NotificationResponse[]> {
    return this.http.get<NotificationResponse[]>(`${API_BASE_URL}/api/notifications`);
  }

  getSupportTickets(): Observable<SupportTicketResponse[]> {
    return this.http.get<SupportTicketResponse[]>(`${API_BASE_URL}/api/support/tickets`);
  }

  getPlatformSupportTickets(): Observable<SupportTicketResponse[]> {
    return this.http.get<SupportTicketResponse[]>(`${API_BASE_URL}/api/support/platform/tickets`);
  }

  getPlatformWorkspaces(): Observable<PlatformWorkspaceResponse[]> {
    return this.http.get<PlatformWorkspaceResponse[]>(`${API_BASE_URL}/api/platform/workspaces`);
  }

  resolvePlatformSupportTicket(ticketId: number, message: string): Observable<SupportTicketResponse> {
    return this.http.post<SupportTicketResponse>(
      `${API_BASE_URL}/api/support/platform/tickets/${ticketId}/resolve`,
      { message }
    );
  }

  resolveTenantSupportTicket(ticketId: number, message: string): Observable<SupportTicketResponse> {
    return this.http.post<SupportTicketResponse>(
      `${API_BASE_URL}/api/support/tickets/${ticketId}/resolve`,
      { message }
    );
  }

  deletePlatformSupportTicket(ticketId: number): Observable<void> {
    return this.http.delete<void>(`${API_BASE_URL}/api/support/platform/tickets/${ticketId}`);
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

  private getEmployeeProfile(employeeId?: number | null): Observable<EmployeeProfileResponse> {
    return employeeId != null ? this.getEmployee(employeeId) : this.getCurrentProfile();
  }
}

