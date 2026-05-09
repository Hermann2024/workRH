import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { LoadingSkeletonComponent } from '../components/loading-skeleton.component';
import {
  EmployeeCreateRequest,
  EmployeeProfileResponse,
  EmploymentSector,
  LeaveResponse,
  MonthlyStatsResponse,
  NotificationResponse,
  SicknessResponse,
  ComplianceCaseStatus,
  ComplianceStepStatus,
  SituationChangeType,
  TeleworkComplianceCaseRequest,
  TeleworkComplianceCaseResponse,
  TeleworkComplianceChecklistItem,
  TeleworkComplianceEmployeeRisk,
  TeleworkDeclarationResponse,
  WorkRhApiService,
  WorkRhVm
} from '../workrh-api.service';

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
  readonly currentPlan = computed(() => {
    const viewModel = this.vm();
    return viewModel?.plans.find((plan) => plan.code === viewModel.subscription.planCode) ?? null;
  });
  readonly companySummary = computed(() => this.vm()?.companySummary ?? null);
  readonly complianceDossier = computed(() => this.vm()?.complianceDossier ?? null);
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

  readonly employeeForm = this.formBuilder.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
    firstName: ['', [Validators.required]],
    lastName: ['', [Validators.required]],
    countryOfResidence: ['FR', [Validators.required, Validators.minLength(2)]],
    phoneNumber: [''],
    department: [''],
    jobTitle: [''],
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
