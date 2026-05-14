import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { LoadingSkeletonComponent } from '../components/loading-skeleton.component';
import {
  DashboardEmployeeItem,
  EmployeeCreateRequest,
  EmployeeGender,
  EmploymentContractType,
  EmployeeProfileResponse,
  EmploymentSector,
  LeaveResponse,
  MonthlyStatsResponse,
  NotificationResponse,
  SicknessResponse,
  ComplianceCaseStatus,
  ComplianceStepStatus,
  SituationChangeType,
  TaxSimulationResponse,
  TeleworkComplianceCaseRequest,
  TeleworkComplianceCaseResponse,
  TeleworkComplianceChecklistItem,
  TeleworkComplianceEmployeeRisk,
  TeleworkDeclarationResponse,
  WorkRhApiService,
  WorkRhVm
} from '../workrh-api.service';

type CommandCenterLevel = 'OK' | 'WARNING' | 'CRITICAL';

interface CommandCenterAction {
  id: string;
  title: string;
  detail: string;
  source: string;
  level: CommandCenterLevel;
  employeeId?: number;
}

interface CommandCenterVm {
  score: number;
  scoreBackground: string;
  level: CommandCenterLevel;
  levelLabel: string;
  headline: string;
  criticalCount: number;
  warningCount: number;
  missingEvidenceCount: number;
  openCases: number;
  readyCases: number;
  actions: CommandCenterAction[];
}

interface LaunchReadinessItem {
  id: string;
  label: string;
  detail: string;
  status: CommandCenterLevel;
  actionLabel: string;
}

interface LaunchReadinessVm {
  score: number;
  status: CommandCenterLevel;
  statusLabel: string;
  summary: string;
  readyCount: number;
  warningCount: number;
  blockedCount: number;
  nextAction: LaunchReadinessItem | null;
  items: LaunchReadinessItem[];
}

interface ValueProofVm {
  riskExposure: number;
  adminHoursSaved: number;
  payrollBlockersAvoided: number;
  auditEvidenceItems: number;
  narrative: string;
  assumptions: string[];
}

@Component({
  selector: 'app-dashboard-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, LoadingSkeletonComponent],
  templateUrl: './dashboard-page.component.html',
  styleUrl: './page-styles.css'
})
export class DashboardPageComponent {
  private readonly api = inject(WorkRhApiService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly referenceDate = new Date();

  readonly loading = signal(true);
  readonly loadError = signal<string | null>(null);
  readonly opsMessage = signal<string | null>(null);
  readonly opsError = signal<string | null>(null);
  readonly vm = signal<WorkRhVm | null>(null);
  readonly notifications = signal<NotificationResponse[]>([]);
  readonly monthlyStats = signal<MonthlyStatsResponse | null>(null);
  readonly employees = signal<EmployeeProfileResponse[]>([]);
  readonly leaveRequests = signal<LeaveResponse[]>([]);
  readonly sicknessRecords = signal<SicknessResponse[]>([]);
  readonly recentTeleworkDeclarations = signal<TeleworkDeclarationResponse[]>([]);
  readonly complianceCases = signal<TeleworkComplianceCaseResponse[]>([]);
  readonly auditHistory = signal<TeleworkDeclarationResponse[]>([]);
  readonly selectedEmployeeId = signal<number | null>(null);
  readonly complianceSaving = signal(false);
  readonly complianceExportLoading = signal(false);
  readonly employeeSaving = signal(false);
  readonly employeeToggleId = signal<number | null>(null);
  readonly leaveActionId = signal<string | null>(null);
  readonly auditLoading = signal(false);
  readonly exportLoading = signal<'csv' | 'pdf' | 'pdf-placeholder' | null>(null);
  readonly taxSimulation = signal<TaxSimulationResponse | null>(null);
  readonly taxSimulationLoading = signal(false);
  readonly currentPlan = computed(() => {
    const viewModel = this.vm();
    return viewModel?.plans.find((plan) => plan.code === viewModel.subscription.planCode) ?? null;
  });
  readonly companySummary = computed(() => this.vm()?.companySummary ?? null);
  readonly contractDistribution = computed(() => this.buildContractDistribution());
  readonly contractSphereGradient = computed(() => this.buildContractSphereGradient());
  readonly genderDistribution = computed(() => this.buildGenderDistribution());
  readonly complianceDossier = computed(() => this.vm()?.complianceDossier ?? null);
  readonly commandCenter = computed(() => this.buildCommandCenter());
  readonly launchReadiness = computed(() => this.buildLaunchReadiness());
  readonly valueProof = computed(() => this.buildValueProof());
  readonly latestNotifications = computed(() => this.notifications().slice(0, 6));
  readonly pendingLeaveRequests = computed(
    () => this.leaveRequests().filter((leave) => leave.status === 'REQUESTED')
  );
  readonly latestSicknessRecords = computed(() => this.sicknessRecords().slice(0, 8));
  readonly latestTeleworkDeclarations = computed(() => this.recentTeleworkDeclarations().slice(0, 12));
  readonly selectedComplianceCase = computed(() => {
    const employeeId = this.selectedEmployeeId();
    return this.complianceCases().find((item) => item.employeeId === employeeId) ?? null;
  });
  readonly latestAuditHistory = computed(() => this.auditHistory().slice(0, 12));
  readonly selectedEmployee = computed(() => {
    const employeeId = this.selectedEmployeeId();
    return this.employees().find((employee) => employee.id === employeeId) ?? null;
  });
  readonly currentYear = this.referenceDate.getFullYear();
  readonly currentMonth = this.referenceDate.getMonth() + 1;
  readonly contractTypes: EmploymentContractType[] = ['CDI', 'CDD', 'STAGE', 'ALTERNANCE', 'AUTRES'];
  readonly genderTypes: EmployeeGender[] = ['FEMININ', 'MASCULIN', 'AUTRES'];

  readonly employeeForm = this.formBuilder.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
    firstName: ['', [Validators.required]],
    lastName: ['', [Validators.required]],
    countryOfResidence: ['FR', [Validators.required, Validators.minLength(2)]],
    phoneNumber: [''],
    department: [''],
    jobTitle: [''],
    birthDate: [''],
    gender: ['AUTRES' as EmployeeGender, [Validators.required]],
    contractType: ['CDI' as EmploymentContractType, [Validators.required]],
    hireDate: [this.toDateInput(new Date()), [Validators.required]]
  });

  readonly complianceCaseForm = this.formBuilder.nonNullable.group({
    employmentSector: ['PRIVATE' as EmploymentSector, [Validators.required]],
    status: ['IN_REVIEW' as ComplianceCaseStatus, [Validators.required]],
    ccssA1Status: ['TO_PREPARE' as ComplianceStepStatus, [Validators.required]],
    teleworkAgreementStatus: ['TO_PREPARE' as ComplianceStepStatus, [Validators.required]],
    equipmentStatus: ['TO_PREPARE' as ComplianceStepStatus, [Validators.required]],
    healthSafetyStatus: ['TO_PREPARE' as ComplianceStepStatus, [Validators.required]],
    accidentCoverageStatus: ['TO_PREPARE' as ComplianceStepStatus, [Validators.required]],
    dataProtectionStatus: ['TO_PREPARE' as ComplianceStepStatus, [Validators.required]],
    residenceCountryRulesStatus: ['TO_PREPARE' as ComplianceStepStatus, [Validators.required]],
    legalWatchStatus: ['TO_PREPARE' as ComplianceStepStatus, [Validators.required]],
    a1SubmittedAt: [''],
    a1ValidUntil: [''],
    legalSourcesReviewedAt: [this.toDateInput(new Date())],
    nextLegalReviewAt: [this.toDateInput(new Date(this.currentYear + 1, this.referenceDate.getMonth(), this.referenceDate.getDate()))],
    legalSourcesVersion: ['2026-05-08'],
    notes: ['']
  });

  readonly evidenceForm = this.formBuilder.nonNullable.group({
    evidenceType: ['A1_CERTIFICATE', [Validators.required]],
    label: ['', [Validators.required]],
    reference: [''],
    fileUrl: ['']
  });

  readonly situationChangeForm = this.formBuilder.nonNullable.group({
    type: ['RESIDENCE_COUNTRY_CHANGE' as SituationChangeType, [Validators.required]],
    effectiveDate: [this.toDateInput(new Date()), [Validators.required]],
    previousValue: [''],
    newValue: [''],
    reason: ['']
  });

  readonly taxSimulationForm = this.formBuilder.nonNullable.group({
    employeeId: [0, [Validators.required, Validators.min(1)]],
    annualGrossSalary: [0, [Validators.required, Validators.min(1)]],
    annualContractWorkDays: [220, [Validators.required, Validators.min(1)]]
  });

  constructor() {
    this.api.loadViewModel(this.referenceDate).subscribe({
      next: (viewModel: WorkRhVm) => {
        this.vm.set(viewModel);
        this.loadError.set(null);
        this.loadAdvancedData(viewModel);
        this.loading.set(false);
      },
      error: (error) => {
        this.loadError.set(this.readBackendMessage(
          error,
          'Impossible de charger le dashboard RH sans donnees de secours.'
        ));
        this.loading.set(false);
      }
    });
  }

  isEntitled(feature: string): boolean {
    return this.vm()?.subscription.entitlements.includes(feature) ?? false;
  }

  monthLabel(month: number): string {
    return new Intl.DateTimeFormat('fr-FR', { month: 'short' }).format(new Date(this.currentYear, month - 1, 1));
  }

  employeeLabel(employee: EmployeeProfileResponse | null): string {
    if (!employee) {
      return 'Employe non selectionne';
    }
    return `${employee.firstName} ${employee.lastName}`.trim();
  }

  employeeName(employeeId: number): string {
    const employee = this.employees().find((item) => item.id === employeeId) ?? null;
    return employee ? this.employeeLabel(employee) : `#${employeeId}`;
  }

  openEmployeeAbsences(employeeId: number): void {
    void this.router.navigate(['/employee', employeeId], {
      queryParams: { tab: 'leave' }
    });
  }

  leaveTypeLabel(type: LeaveResponse['type']): string {
    const labels: Record<LeaveResponse['type'], string> = {
      PAID: 'Conge paye',
      UNPAID: 'Conge sans solde',
      PATERNITY: 'Paternite',
      MOVING: 'Demenagement',
      MARRIAGE: 'Mariage / PACS',
      BIRTH_OR_ADOPTION: 'Naissance / adoption',
      FAMILY_CARE: 'Assistance familiale',
      BEREAVEMENT: 'Deces / deuil',
      MEDICAL_APPOINTMENT: 'Rendez-vous medical',
      TRAINING: 'Formation',
      ADMINISTRATIVE: 'Demarche administrative',
      OTHER: 'Autre absence'
    };
    return labels[type] ?? type;
  }

  complianceStatusLabel(status: string): string {
    const labels: Record<string, string> = {
      OK: 'OK',
      READY_FOR_REVIEW: 'Pret pour revue',
      ACTION_REQUIRED: 'Action requise',
      MISSING_DATA: 'Donnees manquantes',
      MANUAL_REVIEW: 'Revue manuelle'
    };
    return labels[status] ?? status;
  }

  checklistBadgeClass(item: TeleworkComplianceChecklistItem): string {
    return item.status === 'OK' ? 'badge-success' : 'badge-alert';
  }

  riskBadgeClass(risk: TeleworkComplianceEmployeeRisk): string {
    return risk.riskLevel === 'LOW' ? 'badge-success' : 'badge-alert';
  }

  stepStatusLabel(status: string): string {
    const labels: Record<string, string> = {
      NOT_STARTED: 'Non demarre',
      TO_PREPARE: 'A preparer',
      SUBMITTED: 'Soumis',
      VALIDATED: 'Valide',
      EXPIRED: 'Expire',
      NOT_APPLICABLE: 'Non applicable'
    };
    return labels[status] ?? status;
  }

  saveComplianceCase(): void {
    const employee = this.selectedEmployee();
    if (!employee) {
      this.opsError.set('Selectionnez un salarie avant de creer un dossier conformite.');
      return;
    }

    const raw = this.complianceCaseForm.getRawValue();
    const request: TeleworkComplianceCaseRequest = {
      employeeId: employee.id,
      year: this.currentYear,
      month: this.currentMonth,
      countryCode: employee.countryOfResidence ?? 'FR',
      employmentSector: raw.employmentSector,
      status: raw.status,
      ccssA1Status: raw.ccssA1Status,
      teleworkAgreementStatus: raw.teleworkAgreementStatus,
      equipmentStatus: raw.equipmentStatus,
      healthSafetyStatus: raw.healthSafetyStatus,
      accidentCoverageStatus: raw.accidentCoverageStatus,
      dataProtectionStatus: raw.dataProtectionStatus,
      residenceCountryRulesStatus: raw.residenceCountryRulesStatus,
      legalWatchStatus: raw.legalWatchStatus,
      a1SubmittedAt: raw.a1SubmittedAt || null,
      a1ValidUntil: raw.a1ValidUntil || null,
      legalSourcesReviewedAt: raw.legalSourcesReviewedAt || null,
      nextLegalReviewAt: raw.nextLegalReviewAt || null,
      legalSourcesVersion: raw.legalSourcesVersion || null,
      notes: raw.notes || null
    };

    this.complianceSaving.set(true);
    this.api.saveTeleworkComplianceCase(request).subscribe({
      next: () => {
        this.complianceSaving.set(false);
        this.opsMessage.set('Dossier conformite mis a jour.');
        this.refreshComplianceCases();
      },
      error: (error) => {
        this.complianceSaving.set(false);
        this.opsError.set(this.readBackendMessage(error, 'Impossible de mettre a jour le dossier conformite.'));
      }
    });
  }

  addComplianceEvidence(): void {
    const complianceCase = this.selectedComplianceCase();
    if (!complianceCase) {
      this.opsError.set('Creez le dossier conformite avant d ajouter une preuve.');
      return;
    }
    if (this.evidenceForm.invalid) {
      this.evidenceForm.markAllAsTouched();
      return;
    }

    const raw = this.evidenceForm.getRawValue();
    this.complianceSaving.set(true);
    this.api.addTeleworkComplianceEvidence(complianceCase.id, {
      evidenceType: raw.evidenceType,
      label: raw.label,
      reference: raw.reference || null,
      fileUrl: raw.fileUrl || null
    }).subscribe({
      next: () => {
        this.complianceSaving.set(false);
        this.opsMessage.set('Preuve ajoutee au dossier.');
        this.evidenceForm.reset({ evidenceType: 'A1_CERTIFICATE', label: '', reference: '', fileUrl: '' });
        this.refreshComplianceCases();
      },
      error: (error) => {
        this.complianceSaving.set(false);
        this.opsError.set(this.readBackendMessage(error, 'Impossible d ajouter la preuve.'));
      }
    });
  }

  recordSituationChange(): void {
    const employee = this.selectedEmployee();
    if (!employee) {
      this.opsError.set('Selectionnez un salarie avant d enregistrer un changement.');
      return;
    }

    const raw = this.situationChangeForm.getRawValue();
    this.complianceSaving.set(true);
    this.api.recordTeleworkSituationChange({
      employeeId: employee.id,
      type: raw.type,
      effectiveDate: raw.effectiveDate,
      previousValue: raw.previousValue || null,
      newValue: raw.newValue || null,
      reason: raw.reason || null
    }).subscribe({
      next: () => {
        this.complianceSaving.set(false);
        this.opsMessage.set('Changement de situation journalise.');
        this.refreshComplianceCases();
      },
      error: (error) => {
        this.complianceSaving.set(false);
        this.opsError.set(this.readBackendMessage(error, 'Impossible de journaliser le changement.'));
      }
    });
  }

  validateComplianceCase(caseId: number): void {
    this.complianceSaving.set(true);
    this.api.validateTeleworkComplianceCase(caseId).subscribe({
      next: () => {
        this.complianceSaving.set(false);
        this.opsMessage.set('Dossier valide pour paie/RH.');
        this.refreshComplianceCases();
      },
      error: (error) => {
        this.complianceSaving.set(false);
        this.opsError.set(this.readBackendMessage(error, 'Impossible de valider le dossier.'));
      }
    });
  }

  exportComplianceCases(): void {
    this.complianceExportLoading.set(true);
    this.api.downloadTeleworkComplianceCases(this.currentYear, this.currentMonth).subscribe({
      next: (blob) => {
        this.saveBlob(blob, `workrh-telework-compliance-${this.currentYear}-${String(this.currentMonth).padStart(2, '0')}.csv`);
        this.complianceExportLoading.set(false);
      },
      error: (error) => {
        this.complianceExportLoading.set(false);
        this.opsError.set(this.readBackendMessage(error, 'Impossible d exporter les dossiers conformite.'));
      }
    });
  }

  downloadDashboardExport(format: 'csv' | 'pdf' | 'pdf-placeholder'): void {
    this.exportLoading.set(format);
    this.api.downloadDashboardExport(format, this.currentYear, this.currentMonth).subscribe({
      next: (blob) => {
        const extension = format === 'csv' ? 'csv' : format === 'pdf' ? 'pdf' : 'txt';
        this.saveBlob(blob, `workrh-dashboard-${this.currentYear}-${String(this.currentMonth).padStart(2, '0')}.${extension}`);
        this.exportLoading.set(null);
      },
      error: (error) => {
        this.opsError.set(this.readBackendMessage(error, "Impossible d'exporter le dashboard."));
        this.exportLoading.set(null);
      }
    });
  }

  createEmployee(): void {
    if (this.employeeForm.invalid) {
      this.employeeForm.markAllAsTouched();
      this.opsError.set('Completez les informations obligatoires pour creer le salarie.');
      return;
    }

    this.opsError.set(null);
    this.opsMessage.set(null);
    this.employeeSaving.set(true);

    const raw = this.employeeForm.getRawValue();
    const request: EmployeeCreateRequest = {
      email: raw.email.trim().toLowerCase(),
      password: raw.password,
      firstName: raw.firstName.trim(),
      lastName: raw.lastName.trim(),
      countryOfResidence: raw.countryOfResidence.trim().toUpperCase(),
      phoneNumber: raw.phoneNumber.trim() || null,
      department: raw.department.trim() || null,
      jobTitle: raw.jobTitle.trim() || null,
      birthDate: raw.birthDate || null,
      gender: raw.gender,
      contractType: raw.contractType,
      crossBorderWorker: raw.countryOfResidence.trim().toUpperCase() !== 'LU',
      hireDate: raw.hireDate,
      roles: ['EMPLOYEE']
    };

    this.api.createEmployee(request).subscribe({
      next: () => {
        this.employeeSaving.set(false);
        this.opsMessage.set('salarie cree avec succes.');
        this.employeeForm.reset({
          email: '',
          password: '',
          firstName: '',
          lastName: '',
          countryOfResidence: 'FR',
          phoneNumber: '',
          department: '',
          jobTitle: '',
          birthDate: '',
          gender: 'AUTRES',
          contractType: 'CDI',
          hireDate: this.toDateInput(new Date())
        });
        this.refreshEmployees(true);
      },
      error: (error) => {
        this.employeeSaving.set(false);
        this.opsError.set(this.readBackendMessage(error, 'Impossible de creer le salarie.'));
      }
    });
  }

  toggleEmployeeActive(employee: EmployeeProfileResponse): void {
    this.employeeToggleId.set(employee.id);
    this.opsError.set(null);
    this.opsMessage.set(null);

    const request$ = employee.active
      ? this.api.deactivateEmployee(employee.id)
      : this.api.activateEmployee(employee.id);

    request$.subscribe({
      next: () => {
        this.employeeToggleId.set(null);
        this.opsMessage.set(employee.active ? 'Compte salarie desactive.' : 'Compte salarie reactive.');
        this.refreshEmployees(false);
      },
      error: (error) => {
        this.employeeToggleId.set(null);
        this.opsError.set(this.readBackendMessage(error, 'Impossible de modifier le statut du salarie.'));
      }
    });
  }

  selectEmployee(employeeId: number): void {
    this.selectedEmployeeId.set(employeeId);
    this.taxSimulationForm.patchValue({ employeeId });
    this.hydrateComplianceForm();
    if (this.isEntitled('DECLARATION_AUDIT')) {
      this.refreshAuditHistory();
    }
  }

  approveLeave(leave: LeaveResponse): void {
    this.leaveActionId.set(`approve-${leave.id}`);
    this.opsError.set(null);
    this.opsMessage.set(null);
    this.api.approveLeave(leave.id, 'Valide depuis le dashboard RH').subscribe({
      next: () => {
        this.leaveActionId.set(null);
        this.opsMessage.set('Demande de conge approuvee.');
        this.refreshLeaves();
      },
      error: (error) => {
        this.leaveActionId.set(null);
        this.opsError.set(this.readBackendMessage(error, "Impossible d'approuver cette demande."));
      }
    });
  }

  rejectLeave(leave: LeaveResponse): void {
    this.leaveActionId.set(`reject-${leave.id}`);
    this.opsError.set(null);
    this.opsMessage.set(null);
    this.api.rejectLeave(leave.id, 'Refuse depuis le dashboard RH').subscribe({
      next: () => {
        this.leaveActionId.set(null);
        this.opsMessage.set('Demande de conge rejetee.');
        this.refreshLeaves();
      },
      error: (error) => {
        this.leaveActionId.set(null);
        this.opsError.set(this.readBackendMessage(error, 'Impossible de rejeter cette demande.'));
      }
    });
  }

  simulateTaxImpact(): void {
    if (this.taxSimulationForm.invalid) {
      this.taxSimulationForm.markAllAsTouched();
      return;
    }

    this.taxSimulationLoading.set(true);
    this.taxSimulation.set(null);
    this.api.simulateTeleworkTaxImpact(this.currentYear, this.currentMonth, this.taxSimulationForm.getRawValue()).subscribe({
      next: (response) => {
        this.taxSimulation.set(response);
        this.taxSimulationLoading.set(false);
      },
      error: (error) => {
        this.taxSimulationLoading.set(false);
        this.opsError.set(this.readBackendMessage(error, "Impossible de simuler l'impact fiscal."));
      }
    });
  }

  downloadLeaveEvidence(leave: LeaveResponse): void {
    this.api.downloadLeaveEvidence(leave.id).subscribe({
      next: (blob) => this.saveBlob(blob, leave.evidenceFileName || `justificatif-absence-${leave.id}`),
      error: (error) => {
        this.opsError.set(this.readBackendMessage(error, 'Impossible de telecharger le justificatif.'));
      }
    });
  }

  downloadSicknessEvidence(record: SicknessResponse): void {
    this.api.downloadSicknessEvidence(record.id).subscribe({
      next: (blob) => this.saveBlob(blob, record.evidenceFileName || `justificatif-maladie-${record.id}`),
      error: (error) => {
        this.opsError.set(this.readBackendMessage(error, 'Impossible de telecharger le justificatif maladie.'));
      }
    });
  }

  contractTypeLabel(type: EmploymentContractType | string): string {
    const labels: Record<string, string> = {
      CDI: 'CDI',
      CDD: 'CDD',
      STAGE: 'Stage',
      ALTERNANCE: 'Alternance',
      AUTRES: 'Autres'
    };
    return labels[type] ?? type;
  }

  genderLabel(gender: EmployeeGender | string): string {
    const labels: Record<string, string> = {
      FEMININ: 'Feminin',
      MASCULIN: 'Masculin',
      AUTRES: 'Autres'
    };
    return labels[gender] ?? gender;
  }

  annualFiscalPercent(employee: DashboardEmployeeItem): number {
    if (Number.isFinite(employee.riskScorePercent)) {
      return employee.riskScorePercent;
    }

    const annualLimit = employee.annualUsedDays + employee.remainingDays;
    if (annualLimit <= 0) {
      return 0;
    }
    return Math.round((employee.annualUsedDays * 100) / annualLimit);
  }

  fiscalStatusLabel(employee: DashboardEmployeeItem): string {
    const status = employee.annualFiscalLimitExceeded ? 'Alerte' : 'OK';
    return `${status} ${this.annualFiscalPercent(employee)}%`;
  }

  commandLevelClass(level: CommandCenterLevel): string {
    return {
      OK: 'level-ok',
      WARNING: 'level-warning',
      CRITICAL: 'level-critical'
    }[level];
  }

  downloadLaunchPack(): void {
    const readiness = this.launchReadiness();
    const command = this.commandCenter();
    const viewModel = this.vm();
    const lines = [
      'WorkRH - pack de lancement client',
      `Date: ${this.toDateInput(new Date())}`,
      `Plan: ${this.currentPlan()?.name ?? viewModel?.subscription.planCode ?? 'Non renseigne'}`,
      `Score deploiement: ${readiness.score}/100 - ${readiness.statusLabel}`,
      `Score controle: ${command.score}/100 - ${command.levelLabel}`,
      '',
      'Checklist de lancement',
      ...readiness.items.map((item) => `- [${item.status}] ${item.label}: ${item.detail}`),
      '',
      'Actions prioritaires',
      ...(command.actions.length
        ? command.actions.map((action) => `- [${action.level}] ${action.source} - ${action.title}: ${action.detail}`)
        : ['- Aucune action prioritaire detectee.']),
      '',
      'Indicateurs',
      `- Employes charges: ${this.employees().length}`,
      `- Jours teletravail utilises: ${viewModel?.dashboard.totalUsedDays ?? 0}`,
      `- Alertes fiscales: ${viewModel?.dashboard.fiscalAlerts ?? 0}`,
      `- Justificatifs manquants: ${command.missingEvidenceCount}`,
      `- Dossiers conformite ouverts: ${command.openCases}`,
      `- Exposition estimee evitee: ${this.valueProof().riskExposure} EUR`,
      `- Temps RH estime gagne: ${this.valueProof().adminHoursSaved} h`,
      '',
      'Note commerciale',
      "Ce document sert de support d'onboarding et de revue avant demo client. Les controles fiscaux et sociaux doivent rester valides par les experts habilites avant usage officiel."
    ];

    this.saveBlob(new Blob([lines.join('\n')], { type: 'text/plain;charset=utf-8' }), `workrh-pack-lancement-${this.currentYear}-${String(this.currentMonth).padStart(2, '0')}.txt`);
  }

  runLaunchAction(item: LaunchReadinessItem): void {
    switch (item.id) {
      case 'subscription':
        void this.router.navigate(['/billing']);
        break;
      case 'employees':
      case 'profiles':
        void this.router.navigate(['/employees']);
        break;
      case 'compliance':
      case 'payroll':
        void this.router.navigate(['/policies']);
        break;
      case 'evidence':
        if (this.commandCenter().actions.some((action) => action.employeeId != null)) {
          const employeeId = this.commandCenter().actions.find((action) => action.employeeId != null)?.employeeId;
          if (employeeId != null) {
            this.openEmployeeAbsences(employeeId);
            return;
          }
        }
        break;
      case 'exports':
        this.downloadLaunchPack();
        break;
      case 'connectors':
        void this.router.navigate(['/services']);
        break;
      default:
        break;
    }
  }

  private loadAdvancedData(viewModel: WorkRhVm): void {
    if (viewModel.subscription.entitlements.includes('MONTHLY_STATS')) {
      this.api.getMonthlyStats(this.currentYear).subscribe({
        next: (response) => this.monthlyStats.set(response),
        error: (error) => this.opsError.set(this.readBackendMessage(error, 'Statistiques mensuelles indisponibles.'))
      });
    } else {
      this.monthlyStats.set(null);
    }

    if (viewModel.subscription.entitlements.includes('EMAIL_NOTIFICATIONS')) {
      this.api.getNotifications().subscribe({
        next: (response) => this.notifications.set(response),
        error: (error) => this.opsError.set(this.readBackendMessage(error, 'Journal des notifications indisponible.'))
      });
    } else {
      this.notifications.set([]);
    }

    if (viewModel.subscription.entitlements.includes('EMPLOYEE_MANAGEMENT')) {
      this.refreshEmployees(true);
    } else {
      this.employees.set([]);
      this.selectedEmployeeId.set(null);
    }

    if (viewModel.subscription.entitlements.includes('LEAVE_MANAGEMENT')) {
      this.refreshLeaves();
    } else {
      this.leaveRequests.set([]);
    }

    if (viewModel.subscription.entitlements.includes('SICKNESS_MANAGEMENT')) {
      this.refreshSickness();
    } else {
      this.sicknessRecords.set([]);
    }

    if (viewModel.subscription.entitlements.includes('TELEWORK_BASIC')) {
      this.refreshRecentTeleworkDeclarations();
    } else {
      this.recentTeleworkDeclarations.set([]);
    }

    if (!viewModel.subscription.entitlements.includes('DECLARATION_AUDIT')) {
      this.auditHistory.set([]);
      this.complianceCases.set([]);
    } else {
      this.refreshComplianceCases();
    }
  }

  private refreshEmployees(selectFirstIfNeeded: boolean): void {
    this.api.getEmployees().subscribe({
      next: (employees) => {
        this.employees.set(employees);
        const currentSelected = this.selectedEmployeeId();
        const fallbackEmployee = employees.find((employee) => employee.active) ?? employees[0] ?? null;
        const nextSelection = currentSelected != null && employees.some((employee) => employee.id === currentSelected)
          ? currentSelected
          : selectFirstIfNeeded ? fallbackEmployee?.id ?? null : currentSelected;

        this.selectedEmployeeId.set(nextSelection);
        if (nextSelection != null && this.taxSimulationForm.controls.employeeId.value === 0) {
          this.taxSimulationForm.patchValue({ employeeId: nextSelection });
        }
        if (this.isEntitled('DECLARATION_AUDIT') && nextSelection != null) {
          this.refreshAuditHistory();
        }
      },
      error: (error) => {
        this.opsError.set(this.readBackendMessage(error, 'Gestion des salaries indisponible.'));
      }
    });
  }

  private refreshLeaves(): void {
    this.api.getLeaves().subscribe({
      next: (leaves) => this.leaveRequests.set(leaves),
      error: (error) => {
        this.opsError.set(this.readBackendMessage(error, 'Gestion des conges indisponible.'));
      }
    });
  }

  private refreshSickness(): void {
    this.api.getSickness().subscribe({
      next: (records) => this.sicknessRecords.set(records),
      error: (error) => {
        this.opsError.set(this.readBackendMessage(error, 'Suivi des arrets maladie indisponible.'));
      }
    });
  }

  private refreshRecentTeleworkDeclarations(): void {
    this.api.getRecentTeleworkDeclarations().subscribe({
      next: (records) => this.recentTeleworkDeclarations.set(records),
      error: (error) => {
        this.opsError.set(this.readBackendMessage(error, 'Declarations teletravail indisponibles.'));
      }
    });
  }

  private refreshAuditHistory(): void {
    const employeeId = this.selectedEmployeeId();
    if (employeeId == null) {
      this.auditHistory.set([]);
      return;
    }

    this.auditLoading.set(true);
    this.api.getTeleworkHistory(employeeId).subscribe({
      next: (history) => {
        this.auditHistory.set(history);
        this.auditLoading.set(false);
      },
      error: (error) => {
        this.auditLoading.set(false);
        this.opsError.set(this.readBackendMessage(error, "Historique d'audit indisponible."));
      }
    });
  }

  private refreshComplianceCases(): void {
    this.api.getTeleworkComplianceCases(this.currentYear, this.currentMonth).subscribe({
      next: (cases) => {
        this.complianceCases.set(cases);
        this.hydrateComplianceForm();
      },
      error: (error) => this.opsError.set(this.readBackendMessage(error, 'Dossiers conformite indisponibles.'))
    });
  }

  private hydrateComplianceForm(): void {
    const complianceCase = this.selectedComplianceCase();
    if (!complianceCase) {
      return;
    }

    this.complianceCaseForm.patchValue({
      employmentSector: complianceCase.employmentSector,
      status: complianceCase.status,
      ccssA1Status: complianceCase.ccssA1Status,
      teleworkAgreementStatus: complianceCase.teleworkAgreementStatus,
      equipmentStatus: complianceCase.equipmentStatus,
      healthSafetyStatus: complianceCase.healthSafetyStatus,
      accidentCoverageStatus: complianceCase.accidentCoverageStatus,
      dataProtectionStatus: complianceCase.dataProtectionStatus,
      residenceCountryRulesStatus: complianceCase.residenceCountryRulesStatus,
      legalWatchStatus: complianceCase.legalWatchStatus,
      a1SubmittedAt: complianceCase.a1SubmittedAt ?? '',
      a1ValidUntil: complianceCase.a1ValidUntil ?? '',
      legalSourcesReviewedAt: complianceCase.legalSourcesReviewedAt ?? '',
      nextLegalReviewAt: complianceCase.nextLegalReviewAt ?? '',
      legalSourcesVersion: complianceCase.legalSourcesVersion ?? '',
      notes: complianceCase.notes ?? ''
    });
  }

  private buildContractDistribution(): Array<{ type: EmploymentContractType; label: string; count: number; percent: number; offset: number }> {
    const employees = this.employees();
    let offset = 0;
    return this.contractTypes.map((type) => {
      const count = employees.filter((employee) => employee.contractType === type).length;
      const percent = employees.length ? Math.round((count * 100) / employees.length) : 0;
      const item = { type, label: this.contractTypeLabel(type), count, percent, offset };
      offset += percent;
      return item;
    });
  }

  private buildContractSphereGradient(): string {
    const colors: Record<EmploymentContractType, string> = {
      CDI: '#1746a2',
      CDD: '#18a0fb',
      STAGE: '#ef3340',
      ALTERNANCE: '#f59e0b',
      AUTRES: '#64748b'
    };
    const distribution = this.contractDistribution();
    const total = distribution.reduce((sum, item) => sum + item.count, 0);
    if (total === 0) {
      return 'conic-gradient(#d8dee9 0deg 360deg)';
    }

    let cursor = 0;
    const colorStops = distribution
      .filter((item) => item.count > 0)
      .map((item) => {
        const start = cursor;
        const end = Math.min(cursor + (item.count / total) * 360, 360);
        cursor = end;
        return `${colors[item.type]} ${start.toFixed(2)}deg ${end.toFixed(2)}deg`;
      });
    return `conic-gradient(${colorStops.join(', ')})`;
  }

  private buildGenderDistribution(): Array<{ type: EmployeeGender; label: string; count: number; percent: number }> {
    const employees = this.employees();
    return this.genderTypes.map((type) => {
      const count = employees.filter((employee) => (employee.gender ?? 'AUTRES') === type).length;
      const percent = employees.length ? Math.round((count * 100) / employees.length) : 0;
      return { type, label: this.genderLabel(type), count, percent };
    });
  }

  private buildCommandCenter(): CommandCenterVm {
    const viewModel = this.vm();
    const dashboardEmployees = viewModel?.dashboard.employees ?? [];
    const dossier = this.complianceDossier();
    const complianceCases = this.complianceCases();
    const actions: CommandCenterAction[] = [];

    dashboardEmployees.forEach((employee) => {
      if (employee.annualFiscalLimitExceeded || employee.riskLevel === 'RED') {
        actions.push({
          id: `fiscal-${employee.employeeId}`,
          title: `${this.employeeName(employee.employeeId)} approche du seuil fiscal`,
          detail: `${employee.annualUsedDays} jours annuels, ${employee.remainingDays} jours restants, risque ${this.annualFiscalPercent(employee)}%.`,
          source: 'Teletravail',
          level: 'CRITICAL',
          employeeId: employee.employeeId
        });
      } else if (employee.weeklyCompanyLimitExceeded || employee.riskLevel === 'ORANGE') {
        actions.push({
          id: `policy-${employee.employeeId}`,
          title: `${this.employeeName(employee.employeeId)} demande une revue RH`,
          detail: `${employee.weeklyUsedDays} jours cette semaine. ${employee.riskLabel}.`,
          source: 'Politique interne',
          level: 'WARNING',
          employeeId: employee.employeeId
        });
      }
    });

    (dossier?.employeeRisks ?? []).forEach((risk) => {
      if (risk.a1Required || risk.article13Required || risk.fiscalLimitExceeded || this.isHighRisk(risk.riskLevel)) {
        actions.push({
          id: `dossier-${risk.employeeId}-${risk.countryCode}`,
          title: `${this.employeeName(risk.employeeId)} a un point legal a traiter`,
          detail: risk.recommendation || `Pays ${risk.countryCode}, ${risk.annualUsedDays}/${risk.annualLimitDays} jours declares.`,
          source: risk.a1Required ? 'A1 / CCSS' : risk.article13Required ? 'Article 13' : 'Fiscal',
          level: risk.fiscalLimitExceeded || this.isHighRisk(risk.riskLevel) ? 'CRITICAL' : 'WARNING',
          employeeId: risk.employeeId
        });
      }
    });

    this.leaveRequests()
      .filter((leave) => leave.evidenceRequired && !leave.evidenceUploaded)
      .forEach((leave) => {
        actions.push({
          id: `leave-${leave.id}`,
          title: `Justificatif conge manquant pour ${this.employeeName(leave.employeeId)}`,
          detail: `${this.leaveTypeLabel(leave.type)} du ${leave.startDate} au ${leave.endDate}.`,
          source: 'Absences',
          level: 'WARNING',
          employeeId: leave.employeeId
        });
      });

    this.sicknessRecords()
      .filter((record) => record.evidenceRequired && !record.evidenceUploaded)
      .forEach((record) => {
        actions.push({
          id: `sickness-${record.id}`,
          title: `Justificatif maladie manquant pour ${this.employeeName(record.employeeId)}`,
          detail: `Arret du ${record.startDate} au ${record.endDate}.`,
          source: 'Maladie',
          level: 'WARNING',
          employeeId: record.employeeId
        });
      });

    complianceCases.forEach((complianceCase) => {
      const expiredStepCount = this.complianceStepStatuses(complianceCase).filter((status) => status === 'EXPIRED').length;
      const preparationStepCount = this.complianceStepStatuses(complianceCase)
        .filter((status) => status === 'NOT_STARTED' || status === 'TO_PREPARE')
        .length;
      if (complianceCase.status === 'ACTION_REQUIRED' || expiredStepCount > 0) {
        actions.push({
          id: `case-critical-${complianceCase.id}`,
          title: `Dossier conformite bloque pour ${this.employeeName(complianceCase.employeeId)}`,
          detail: `${expiredStepCount} piece(s) expiree(s), statut ${this.complianceStatusLabel(complianceCase.status)}.`,
          source: 'Dossier legal',
          level: 'CRITICAL',
          employeeId: complianceCase.employeeId
        });
      } else if (preparationStepCount > 0 || complianceCase.status === 'DRAFT' || complianceCase.status === 'IN_REVIEW') {
        actions.push({
          id: `case-warning-${complianceCase.id}`,
          title: `Dossier conformite a finaliser pour ${this.employeeName(complianceCase.employeeId)}`,
          detail: `${preparationStepCount} etape(s) a preparer avant paie ou revue.`,
          source: 'Workflow',
          level: 'WARNING',
          employeeId: complianceCase.employeeId
        });
      }
    });

    (dossier?.checklist ?? [])
      .filter((item) => item.status !== 'OK')
      .forEach((item, index) => {
        actions.push({
          id: `checklist-${index}`,
          title: item.label,
          detail: item.detail,
          source: 'Cadre legal',
          level: item.status === 'ACTION_REQUIRED' ? 'CRITICAL' : 'WARNING'
        });
      });

    const missingEvidenceCount = this.leaveRequests().filter((leave) => leave.evidenceRequired && !leave.evidenceUploaded).length
      + this.sicknessRecords().filter((record) => record.evidenceRequired && !record.evidenceUploaded).length;
    const criticalCount = actions.filter((action) => action.level === 'CRITICAL').length;
    const warningCount = actions.filter((action) => action.level === 'WARNING').length;
    const openCases = complianceCases.filter((item) => item.status !== 'READY_FOR_PAYROLL' && item.status !== 'CLOSED').length;
    const readyCases = complianceCases.filter((item) => item.status === 'READY_FOR_PAYROLL' || item.status === 'CLOSED').length;
    const dossierPenalty = Math.min(
      (dossier?.fiscalAlerts ?? 0) * 5
      + (dossier?.socialSecurityAlerts ?? 0) * 5
      + (dossier?.a1DeclarationsRequired ?? 0) * 2
      + (dossier?.article13Cases ?? 0) * 2,
      24
    );
    const actionPenalty = Math.min(criticalCount * 7, 35) + Math.min(warningCount * 3, 24);
    const missingEvidencePenalty = Math.min(missingEvidenceCount * 2, 12);
    const score = Math.max(5, Math.min(100, 100 - actionPenalty - missingEvidencePenalty - dossierPenalty));
    const level: CommandCenterLevel = score < 70 || criticalCount > 0 ? 'CRITICAL' : score < 88 || warningCount > 0 ? 'WARNING' : 'OK';
    const sortedActions = actions
      .sort((left, right) => this.actionPriority(right.level) - this.actionPriority(left.level))
      .slice(0, 6);

    return {
      score,
      scoreBackground: this.scoreBackground(score, level),
      level,
      levelLabel: level === 'OK' ? 'Sous controle' : level === 'WARNING' ? 'A surveiller' : 'Action urgente',
      headline: level === 'OK'
        ? 'Aucun blocage critique detecte sur les donnees disponibles.'
        : `${criticalCount + warningCount} action(s) prioritaire(s) avant paie, audit ou validation client.`,
      criticalCount,
      warningCount,
      missingEvidenceCount,
      openCases,
      readyCases,
      actions: sortedActions
    };
  }

  private complianceStepStatuses(complianceCase: TeleworkComplianceCaseResponse): ComplianceStepStatus[] {
    return [
      complianceCase.ccssA1Status,
      complianceCase.teleworkAgreementStatus,
      complianceCase.equipmentStatus,
      complianceCase.healthSafetyStatus,
      complianceCase.accidentCoverageStatus,
      complianceCase.dataProtectionStatus,
      complianceCase.residenceCountryRulesStatus,
      complianceCase.legalWatchStatus
    ];
  }

  private isHighRisk(riskLevel: string): boolean {
    return ['HIGH', 'RED', 'CRITICAL'].includes(riskLevel);
  }

  private actionPriority(level: CommandCenterLevel): number {
    return level === 'CRITICAL' ? 3 : level === 'WARNING' ? 2 : 1;
  }

  private scoreBackground(score: number, level: CommandCenterLevel): string {
    const color = level === 'CRITICAL' ? '#dc2626' : level === 'WARNING' ? '#f59e0b' : '#16a34a';
    return `conic-gradient(${color} 0% ${score}%, #e2e8f0 ${score}% 100%)`;
  }

  private buildLaunchReadiness(): LaunchReadinessVm {
    const viewModel = this.vm();
    const employees = this.employees();
    const command = this.commandCenter();
    const dossier = this.complianceDossier();
    const seats = viewModel?.subscription.seatsPurchased ?? 0;
    const activeEmployees = employees.filter((employee) => employee.active).length;
    const employeesWithProfile = employees.filter((employee) =>
      Boolean(employee.birthDate)
      && Boolean(employee.gender)
      && Boolean(employee.contractType)
      && Boolean(employee.hireDate)
      && Boolean(employee.countryOfResidence)
    ).length;
    const cases = this.complianceCases();
    const readyCases = cases.filter((item) => item.status === 'READY_FOR_PAYROLL' || item.status === 'CLOSED').length;
    const nativeConnectorsReady = false;

    const items: LaunchReadinessItem[] = [
      {
        id: 'subscription',
        label: 'Abonnement et droits',
        detail: viewModel ? `${this.currentPlan()?.name ?? viewModel.subscription.planCode} actif avec ${seats} siege(s).` : 'Aucun abonnement charge.',
        status: viewModel && viewModel.subscription.status === 'ACTIVE' ? 'OK' : 'CRITICAL',
        actionLabel: 'Voir billing'
      },
      {
        id: 'employees',
        label: 'Base salaries',
        detail: employees.length ? `${activeEmployees}/${employees.length} salarie(s) actifs.` : 'Aucun salarie charge dans le tenant.',
        status: employees.length > 0 && activeEmployees > 0 ? 'OK' : 'CRITICAL',
        actionLabel: 'Gerer salaries'
      },
      {
        id: 'profiles',
        label: 'Profils RH exploitables',
        detail: employees.length ? `${employeesWithProfile}/${employees.length} profil(s) avec naissance, genre, contrat, embauche et residence.` : 'Les profils RH ne sont pas encore renseignes.',
        status: employees.length > 0 && employeesWithProfile === employees.length ? 'OK' : employeesWithProfile > 0 ? 'WARNING' : 'CRITICAL',
        actionLabel: 'Completer'
      },
      {
        id: 'compliance',
        label: 'Dossier legal teletravail',
        detail: dossier ? `${dossier.declarationsReviewed} declaration(s), ${dossier.fiscalAlerts} alerte(s) fiscale(s), ${dossier.socialSecurityAlerts} alerte(s) sociale(s).` : 'Dossier legal non disponible sur le plan ou les donnees actuelles.',
        status: dossier && dossier.overallStatus === 'READY_FOR_REVIEW' && command.criticalCount === 0 ? 'OK' : dossier ? 'WARNING' : 'CRITICAL',
        actionLabel: 'Ouvrir legal'
      },
      {
        id: 'evidence',
        label: 'Justificatifs RH',
        detail: command.missingEvidenceCount === 0 ? 'Aucun justificatif obligatoire manquant.' : `${command.missingEvidenceCount} justificatif(s) obligatoire(s) a recuperer.`,
        status: command.missingEvidenceCount === 0 ? 'OK' : 'WARNING',
        actionLabel: 'Traiter'
      },
      {
        id: 'payroll',
        label: 'Passage paie',
        detail: cases.length ? `${readyCases}/${cases.length} dossier(s) conformite prets paie ou clotures.` : 'Aucun dossier paie/conformite cree.',
        status: cases.length === 0 ? 'WARNING' : readyCases === cases.length ? 'OK' : 'WARNING',
        actionLabel: 'Preparer'
      },
      {
        id: 'exports',
        label: 'Exports et preuve de valeur',
        detail: this.isEntitled('EXPORTS') ? 'Exports CSV, PDF et pack de lancement disponibles.' : 'Les exports ne sont pas inclus dans le plan actif.',
        status: this.isEntitled('EXPORTS') ? 'OK' : 'WARNING',
        actionLabel: 'Exporter'
      },
      {
        id: 'connectors',
        label: 'Connecteurs SIRH / paie',
        detail: nativeConnectorsReady
          ? 'Synchronisation native disponible.'
          : 'Connecteurs visibles commercialement, mais synchronisation native OAuth et mapping provider a finaliser.',
        status: nativeConnectorsReady ? 'OK' : 'WARNING',
        actionLabel: 'Voir services'
      }
    ];

    const readyCount = items.filter((item) => item.status === 'OK').length;
    const warningCount = items.filter((item) => item.status === 'WARNING').length;
    const blockedCount = items.filter((item) => item.status === 'CRITICAL').length;
    const score = Math.round((readyCount / items.length) * 100 - blockedCount * 8 - warningCount * 3);
    const normalizedScore = Math.max(0, Math.min(100, score));
    const status: CommandCenterLevel = blockedCount > 0 || normalizedScore < 65 ? 'CRITICAL' : warningCount > 0 || normalizedScore < 90 ? 'WARNING' : 'OK';
    const nextAction = items.find((item) => item.status === 'CRITICAL')
      ?? items.find((item) => item.status === 'WARNING')
      ?? null;

    return {
      score: normalizedScore,
      status,
      statusLabel: status === 'OK' ? 'Pret client' : status === 'WARNING' ? 'Vendable avec cadrage' : 'A renforcer',
      summary: status === 'OK'
        ? 'Le tenant peut etre presente comme environnement client pilote.'
        : 'Le produit a une base solide, mais certains points doivent etre cadres avant vente recurrente.',
      readyCount,
      warningCount,
      blockedCount,
      nextAction,
      items
    };
  }

  private buildValueProof(): ValueProofVm {
    const viewModel = this.vm();
    const command = this.commandCenter();
    const dossier = this.complianceDossier();
    const employees = this.employees();
    const dashboardEmployees = viewModel?.dashboard.employees ?? [];
    const fiscalRiskEmployees = dashboardEmployees.filter((employee) =>
      employee.annualFiscalLimitExceeded || employee.riskLevel === 'RED'
    ).length;
    const socialRiskCount = (dossier?.socialSecurityAlerts ?? 0)
      + (dossier?.a1DeclarationsRequired ?? 0)
      + (dossier?.article13Cases ?? 0);
    const openWorkflowCount = command.openCases + command.missingEvidenceCount + command.warningCount;
    const auditEvidenceItems = this.complianceCases().reduce((sum, item) => sum + item.evidence.length + item.auditTrail.length, 0)
      + this.leaveRequests().filter((item) => item.evidenceUploaded).length
      + this.sicknessRecords().filter((item) => item.evidenceUploaded).length;
    const riskExposure = fiscalRiskEmployees * 2500
      + socialRiskCount * 1800
      + command.criticalCount * 1200
      + command.missingEvidenceCount * 300;
    const adminHoursSaved = Math.max(
      employees.length,
      Math.round(employees.length * 0.6 + auditEvidenceItems * 0.25 + openWorkflowCount * 0.35)
    );
    const payrollBlockersAvoided = command.criticalCount + command.missingEvidenceCount + (dossier?.fiscalAlerts ?? 0);

    return {
      riskExposure,
      adminHoursSaved,
      payrollBlockersAvoided,
      auditEvidenceItems,
      narrative: riskExposure > 0
        ? 'WorkRH transforme les alertes en prevention mesurable avant paie, audit ou controle frontalier.'
        : 'WorkRH documente la conformite et maintient une preuve exploitable meme sans alerte critique.',
      assumptions: [
        '2 500 EUR d exposition indicative par salarie au seuil fiscal critique.',
        '1 800 EUR d exposition indicative par point social A1 ou Article 13.',
        '0,25 h gagnee par preuve deja centralisee et 0,35 h par workflow guide.'
      ]
    };
  }

  private readBackendMessage(error: unknown, fallback: string): string {
    const backendMessage = (error as { error?: { message?: string } })?.error?.message;
    return typeof backendMessage === 'string' && backendMessage.trim()
      ? backendMessage
      : fallback;
  }

  private saveBlob(blob: Blob, filename: string): void {
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    link.click();
    window.URL.revokeObjectURL(url);
  }

  private toDateInput(date: Date): string {
    return date.toISOString().slice(0, 10);
  }
}
