import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { LoadingSkeletonComponent } from '../components/loading-skeleton.component';
import {
  EmployeeCreateRequest,
  EmployeeProfileResponse,
  LeaveResponse,
  MonthlyStatsResponse,
  NotificationResponse,
  SicknessResponse,
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
  readonly auditHistory = signal<TeleworkDeclarationResponse[]>([]);
  readonly selectedEmployeeId = signal<number | null>(null);
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
  readonly latestNotifications = computed(() => this.notifications().slice(0, 6));
  readonly pendingLeaveRequests = computed(
    () => this.leaveRequests().filter((leave) => leave.status === 'REQUESTED')
  );
  readonly latestSicknessRecords = computed(() => this.sicknessRecords().slice(0, 8));
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
          'Impossible de charger le dashboard RH sans données de secours.'
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
      return 'Employé non sélectionné';
    }
    return `${employee.firstName} ${employee.lastName}`.trim();
  }

  employeeName(employeeId: number): string {
    const employee = this.employees().find((item) => item.id === employeeId) ?? null;
    return employee ? this.employeeLabel(employee) : `#${employeeId}`;
  }

  leaveTypeLabel(type: LeaveResponse['type']): string {
    const labels: Record<LeaveResponse['type'], string> = {
      PAID: 'Conge paye',
      UNPAID: 'Conge sans solde',
      PATERNITY: 'Paternite',
      MOVING: 'Demenagement',
      MARRIAGE: 'Mariage / PACS',
      BIRTH_OR_ADOPTION: 'Naissance / adoption',
      FAMILY_CARE: 'Assistance familiale'
    };
    return labels[type] ?? type;
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
      this.opsError.set('Complétez les informations obligatoires pour créer le salarié.');
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
        this.opsMessage.set('Salarié créé avec succès.');
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
        this.opsError.set(this.readBackendMessage(error, 'Impossible de créer le salarié.'));
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
        this.opsMessage.set(employee.active ? 'Compte salarié désactivé.' : 'Compte salarié réactivé.');
        this.refreshEmployees(false);
      },
      error: (error) => {
        this.employeeToggleId.set(null);
        this.opsError.set(this.readBackendMessage(error, 'Impossible de modifier le statut du salarié.'));
      }
    });
  }

  selectEmployee(employeeId: number): void {
    this.selectedEmployeeId.set(employeeId);
    if (this.isEntitled('DECLARATION_AUDIT')) {
      this.refreshAuditHistory();
    }
  }

  approveLeave(leave: LeaveResponse): void {
    this.leaveActionId.set(`approve-${leave.id}`);
    this.opsError.set(null);
    this.opsMessage.set(null);
    this.api.approveLeave(leave.id, 'Validé depuis le dashboard RH').subscribe({
      next: () => {
        this.leaveActionId.set(null);
        this.opsMessage.set('Demande de congé approuvée.');
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
    this.api.rejectLeave(leave.id, 'Refusé depuis le dashboard RH').subscribe({
      next: () => {
        this.leaveActionId.set(null);
        this.opsMessage.set('Demande de congé rejetée.');
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

    if (!viewModel.subscription.entitlements.includes('DECLARATION_AUDIT')) {
      this.auditHistory.set([]);
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
        this.opsError.set(this.readBackendMessage(error, 'Gestion des salariés indisponible.'));
      }
    });
  }

  private refreshLeaves(): void {
    this.api.getLeaves().subscribe({
      next: (leaves) => this.leaveRequests.set(leaves),
      error: (error) => {
        this.opsError.set(this.readBackendMessage(error, 'Gestion des congés indisponible.'));
      }
    });
  }

  private refreshSickness(): void {
    this.api.getSickness().subscribe({
      next: (records) => this.sicknessRecords.set(records),
      error: (error) => {
        this.opsError.set(this.readBackendMessage(error, 'Suivi des arrêts maladie indisponible.'));
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
