import { CommonModule, DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import {
  EmployeeWorkspaceVm,
  LeaveResponse,
  LeaveType,
  WorkRhApiService
} from '../workrh-api.service';
import { ToastService } from '../services/toast.service';

@Component({
  selector: 'app-employee-portal-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, DatePipe],
  templateUrl: './employee-portal-page.component.html',
  styleUrl: './page-styles.css'
})
export class EmployeePortalPageComponent {
  private readonly api = inject(WorkRhApiService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly toastService = inject(ToastService);
  private readonly route = inject(ActivatedRoute);
  private readonly managedEmployeeId = signal<number | null>(null);

  readonly loading = signal(true);
  readonly loadError = signal<string | null>(null);
  readonly submittingTelework = signal(false);
  readonly submittingLeave = signal(false);
  readonly submittingSickness = signal(false);
  readonly workspace = signal<EmployeeWorkspaceVm | null>(null);
  readonly activeTab = signal<'telework' | 'leave' | 'sickness' | 'history'>('telework');

  readonly leaveTypeOptions: Array<{ value: LeaveType; label: string; helper: string }> = [
    { value: 'PAID', label: 'Conge paye', helper: 'Demande standard de conge annuel.' },
    { value: 'UNPAID', label: 'Conge sans solde', helper: 'Absence non remuneree soumise a validation RH.' },
    { value: 'PATERNITY', label: 'Paternite', helper: 'Conge extraordinaire a instruire selon justificatif.' },
    { value: 'MOVING', label: 'Demenagement', helper: 'Conge extraordinaire lie a un changement de domicile.' },
    { value: 'MARRIAGE', label: 'Mariage / PACS', helper: 'Conge extraordinaire evenement familial.' },
    { value: 'BIRTH_OR_ADOPTION', label: 'Naissance / adoption', helper: "Demande exceptionnelle liee a l'arrivee d'un enfant." },
    { value: 'FAMILY_CARE', label: 'Assistance familiale', helper: 'Absence liee a une situation familiale specifique.' },
    { value: 'BEREAVEMENT', label: 'Deces / deuil', helper: 'Absence exceptionnelle pour evenement familial.' },
    { value: 'MEDICAL_APPOINTMENT', label: 'Rendez-vous medical', helper: 'Absence ponctuelle liee a un rendez-vous medical.' },
    { value: 'TRAINING', label: 'Formation', helper: 'Absence planifiee pour formation ou parcours professionnel.' },
    { value: 'ADMINISTRATIVE', label: 'Demarche administrative', helper: 'Absence pour convocation ou demarche administrative.' },
    { value: 'OTHER', label: 'Autre absence', helper: 'Precisez le motif dans le commentaire pour traitement RH.' }
  ];

  readonly teleworkForm = this.formBuilder.nonNullable.group({
    workDate: [this.toDateInput(new Date()), [Validators.required]],
    countryCode: ['FR', [Validators.required, Validators.minLength(2), Validators.maxLength(7)]],
    totalWorkMinutes: [480, [Validators.required, Validators.min(1)]],
    residenceTeleworkMinutes: [480, [Validators.required, Validators.min(0)]],
    residenceNonTeleworkMinutes: [0, [Validators.required, Validators.min(0)]],
    otherForeignWorkMinutes: [0, [Validators.required, Validators.min(0)]],
    otherForeignCountryCode: [''],
    connectedToEmployerInfrastructure: [true]
  });

  readonly leaveForm = this.formBuilder.nonNullable.group({
    type: ['PAID' as LeaveType, [Validators.required]],
    startDate: [this.toDateInput(new Date()), [Validators.required]],
    endDate: [this.toDateInput(new Date()), [Validators.required]],
    comment: ['']
  });

  readonly sicknessForm = this.formBuilder.nonNullable.group({
    startDate: [this.toDateInput(new Date()), [Validators.required]],
    endDate: [this.toDateInput(new Date()), [Validators.required]],
    comment: ['']
  });

  readonly teleworkModeOptions = [
    {
      label: 'Teletravail residence',
      helper: 'Jour travaille depuis le pays de residence.',
      apply: () => this.applyTeleworkMode(480, 480, 0, 0, '')
    },
    {
      label: 'Activite residence hors teletravail',
      helper: 'Travail dans le pays de residence sans connexion teletravail classique.',
      apply: () => this.applyTeleworkMode(480, 0, 480, 0, '')
    },
    {
      label: 'Autre pays hors Luxembourg',
      helper: 'Activite realisee dans un pays different de la residence.',
      apply: () => this.applyTeleworkMode(480, 0, 0, 480, 'BE')
    },
    {
      label: 'Jour mixte',
      helper: 'Repartition manuelle entre teletravail, autre activite residence et autre pays.',
      apply: () => this.applyTeleworkMode(480, 240, 120, 120, 'BE')
    }
  ];

  readonly currentMonthTeleworkCount = computed(() => {
    const currentMonthPrefix = this.toDateInput(new Date()).slice(0, 7);
    return (this.workspace()?.teleworkHistory ?? [])
      .filter((item) => item.workDate.startsWith(currentMonthPrefix) && item.residenceTeleworkMinutes > 0)
      .length;
  });

  readonly requestedLeavesCount = computed(
    () => (this.workspace()?.leaves ?? []).filter((leave) => leave.status === 'REQUESTED').length
  );

  readonly currentYearTeleworkCount = computed(() => {
    const currentYear = String(new Date().getFullYear());
    return (this.workspace()?.teleworkHistory ?? [])
      .filter((item) => item.workDate.startsWith(currentYear) && item.countsTowardFiscalThreshold)
      .length;
  });

  readonly latestTeleworkEntries = computed(() => this.sortByDateDesc(this.workspace()?.teleworkHistory ?? [], (item) => item.workDate).slice(0, 8));
  readonly latestLeaveEntries = computed(() => this.sortByDateDesc(this.workspace()?.leaves ?? [], (item) => item.startDate).slice(0, 8));
  readonly latestSicknessEntries = computed(() => this.sortByDateDesc(this.workspace()?.sickness ?? [], (item) => item.startDate).slice(0, 8));
  readonly latestLeaveDecision = computed(() => this.sortByDateDesc(
    (this.workspace()?.leaves ?? []).filter((leave) => leave.status === 'APPROVED' || leave.status === 'REJECTED'),
    (leave) => leave.updatedAt || leave.createdAt
  )[0] ?? null);

  constructor() {
    this.route.paramMap.subscribe((params) => {
      const employeeId = this.readManagedEmployeeId(params.get('employeeId'));
      this.managedEmployeeId.set(employeeId);
      this.activeTab.set(this.readRequestedTab() ?? (employeeId == null ? 'telework' : 'leave'));
      this.loadWorkspace();
    });
  }

  submitTelework(): void {
    const profile = this.workspace()?.profile;
    if (!profile || this.teleworkForm.invalid) {
      this.teleworkForm.markAllAsTouched();
      return;
    }

    this.submittingTelework.set(true);
    const formValue = this.teleworkForm.getRawValue();
    const outsideMinutes =
      Number(formValue.residenceTeleworkMinutes)
      + Number(formValue.residenceNonTeleworkMinutes)
      + Number(formValue.otherForeignWorkMinutes);
    if (outsideMinutes > Number(formValue.totalWorkMinutes)) {
      this.submittingTelework.set(false);
      this.toastService.error('Les minutes hors Luxembourg ne peuvent pas depasser le total travaille.');
      return;
    }
    if (Number(formValue.otherForeignWorkMinutes) > 0 && !formValue.otherForeignCountryCode.trim()) {
      this.submittingTelework.set(false);
      this.toastService.error('Indiquez un code pays si vous declarez du temps dans un autre pays.');
      return;
    }
    const request = {
      employeeId: profile.id,
      workDate: formValue.workDate,
      countryCode: formValue.countryCode.trim().toUpperCase(),
      totalWorkMinutes: Number(formValue.totalWorkMinutes),
      residenceTeleworkMinutes: Number(formValue.residenceTeleworkMinutes),
      residenceNonTeleworkMinutes: Number(formValue.residenceNonTeleworkMinutes),
      otherForeignWorkMinutes: Number(formValue.otherForeignWorkMinutes),
      otherForeignCountryCode: formValue.otherForeignCountryCode.trim().toUpperCase() || null,
      connectedToEmployerInfrastructure: formValue.connectedToEmployerInfrastructure
    };

    this.api.declareTelework(request).subscribe({
      next: () => {
        this.submittingTelework.set(false);
        this.toastService.success('Jour de teletravail enregistre.');
        this.teleworkForm.patchValue({
          workDate: this.toDateInput(new Date()),
          totalWorkMinutes: 480,
          residenceTeleworkMinutes: 480,
          residenceNonTeleworkMinutes: 0,
          otherForeignWorkMinutes: 0,
          otherForeignCountryCode: '',
          connectedToEmployerInfrastructure: true
        });
        this.loadWorkspace();
        this.activeTab.set('history');
      },
      error: (error) => {
        this.submittingTelework.set(false);
        this.toastService.error(this.readBackendMessage(error, "Impossible d'enregistrer le teletravail."));
      }
    });
  }

  submitLeave(): void {
    const profile = this.workspace()?.profile;
    if (!profile || this.leaveForm.invalid) {
      this.leaveForm.markAllAsTouched();
      return;
    }

    this.submittingLeave.set(true);
    const formValue = this.leaveForm.getRawValue();
    this.api.createLeaveRequest({
      employeeId: profile.id,
      type: formValue.type,
      startDate: formValue.startDate,
      endDate: formValue.endDate,
      comment: formValue.comment.trim()
    }).subscribe({
      next: () => {
        this.submittingLeave.set(false);
        this.toastService.success('Demande d’absence transmise.');
        this.leaveForm.patchValue({
          startDate: this.toDateInput(new Date()),
          endDate: this.toDateInput(new Date()),
          comment: ''
        });
        this.loadWorkspace();
        this.activeTab.set('history');
      },
      error: (error) => {
        this.submittingLeave.set(false);
        this.toastService.error(this.readBackendMessage(error, "Impossible d'enregistrer la demande de conge."));
      }
    });
  }

  submitSickness(): void {
    const profile = this.workspace()?.profile;
    if (!profile || this.sicknessForm.invalid) {
      this.sicknessForm.markAllAsTouched();
      return;
    }

    this.submittingSickness.set(true);
    const formValue = this.sicknessForm.getRawValue();
    this.api.declareSickness({
      employeeId: profile.id,
      startDate: formValue.startDate,
      endDate: formValue.endDate,
      comment: formValue.comment.trim()
    }).subscribe({
      next: () => {
        this.submittingSickness.set(false);
        this.toastService.success('Arret maladie declare.');
        this.sicknessForm.patchValue({
          startDate: this.toDateInput(new Date()),
          endDate: this.toDateInput(new Date()),
          comment: ''
        });
        this.loadWorkspace();
        this.activeTab.set('history');
      },
      error: (error) => {
        this.submittingSickness.set(false);
        this.toastService.error(this.readBackendMessage(error, "Impossible d'enregistrer l'arret maladie."));
      }
    });
  }

  cancelLeave(leave: LeaveResponse): void {
    this.api.cancelLeave(leave.id, 'Annulation demandee par le salarie').subscribe({
      next: () => {
        this.toastService.info('Demande de conge annulee.');
        this.loadWorkspace();
      },
      error: (error) => {
        this.toastService.error(this.readBackendMessage(error, "Impossible d'annuler cette demande."));
      }
    });
  }

  leaveTypeLabel(type: LeaveType): string {
    return this.leaveTypeOptions.find((option) => option.value === type)?.label ?? type;
  }

  leaveStatusLabel(status: string): string {
    const labels: Record<string, string> = {
      REQUESTED: 'En attente',
      APPROVED: 'Approuvee',
      REJECTED: 'Rejetee',
      CANCELLED: 'Annulee'
    };
    return labels[status] ?? status;
  }

  leaveDecisionMessage(leave: LeaveResponse): string {
    const period = `${this.formatDisplayDate(leave.startDate)} - ${this.formatDisplayDate(leave.endDate)}`;
    if (leave.status === 'APPROVED') {
      return `Votre demande ${this.leaveTypeLabel(leave.type)} du ${period} a ete approuvee.`;
    }
    if (leave.status === 'REJECTED') {
      return `Votre demande ${this.leaveTypeLabel(leave.type)} du ${period} a ete rejetee.`;
    }
    return `Votre demande ${this.leaveTypeLabel(leave.type)} du ${period} a ete mise a jour.`;
  }

  isManagedEmployeeWorkspace(): boolean {
    return this.managedEmployeeId() != null;
  }

  selectedLeaveHelper(): string {
    const selectedType = this.leaveForm.controls.type.value;
    return this.leaveTypeOptions.find((option) => option.value === selectedType)?.helper
      ?? 'Ajoutez un commentaire si vous souhaitez aider le traitement RH.';
  }

  canCancelLeave(leave: LeaveResponse): boolean {
    return leave.status === 'REQUESTED';
  }

  private loadWorkspace(): void {
    const hadWorkspace = this.workspace() !== null;
    this.loading.set(true);
    this.api.loadEmployeeWorkspace(new Date(), this.managedEmployeeId()).subscribe({
      next: (workspace) => {
        this.workspace.set(workspace);
        this.loadError.set(null);
        this.teleworkForm.patchValue({
          countryCode: (workspace.profile.countryOfResidence ?? 'FR').toUpperCase(),
          totalWorkMinutes: workspace.teleworkSummary?.policy.standardDailyWorkMinutes ?? 480,
          residenceTeleworkMinutes: workspace.teleworkSummary?.policy.standardDailyWorkMinutes ?? 480
        });
        this.loading.set(false);
      },
      error: (error) => {
        this.loading.set(false);
        const message = this.readBackendMessage(error, "Impossible de charger l'espace employe.");
        if (!hadWorkspace) {
          this.loadError.set(message);
        }
        this.toastService.error(message);
      }
    });
  }

  private applyTeleworkMode(
    totalWorkMinutes: number,
    residenceTeleworkMinutes: number,
    residenceNonTeleworkMinutes: number,
    otherForeignWorkMinutes: number,
    otherForeignCountryCode: string
  ): void {
    this.teleworkForm.patchValue({
      totalWorkMinutes,
      residenceTeleworkMinutes,
      residenceNonTeleworkMinutes,
      otherForeignWorkMinutes,
      otherForeignCountryCode
    });
  }

  private readBackendMessage(error: unknown, fallback: string): string {
    const backendMessage = (error as { error?: { message?: string } })?.error?.message;
    return typeof backendMessage === 'string' && backendMessage.trim()
      ? backendMessage
      : fallback;
  }

  private sortByDateDesc<T>(items: T[], getValue: (item: T) => string): T[] {
    return [...items].sort((left, right) => getValue(right).localeCompare(getValue(left)));
  }

  private toDateInput(date: Date): string {
    return date.toISOString().slice(0, 10);
  }

  private formatDisplayDate(value: string): string {
    const [year, month, day] = value.split('-');
    return day && month && year ? `${day}/${month}/${year}` : value;
  }

  private readManagedEmployeeId(rawId: string | null): number | null {
    if (rawId == null) {
      return null;
    }

    const employeeId = Number(rawId);
    return Number.isInteger(employeeId) && employeeId > 0 ? employeeId : null;
  }

  private readRequestedTab(): 'telework' | 'leave' | 'sickness' | 'history' | null {
    const rawTab = this.route.snapshot.queryParamMap.get('tab');
    return rawTab === 'telework' || rawTab === 'leave' || rawTab === 'sickness' || rawTab === 'history'
      ? rawTab
      : null;
  }
}
