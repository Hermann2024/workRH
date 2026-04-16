import { CommonModule, DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
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

  readonly loading = signal(true);
  readonly loadError = signal<string | null>(null);
  readonly submittingTelework = signal(false);
  readonly submittingLeave = signal(false);
  readonly submittingSickness = signal(false);
  readonly workspace = signal<EmployeeWorkspaceVm | null>(null);

  readonly leaveTypeOptions: Array<{ value: LeaveType; label: string; helper: string }> = [
    { value: 'PAID', label: 'Conge paye', helper: 'Demande standard de conge annuel.' },
    { value: 'UNPAID', label: 'Conge sans solde', helper: 'Absence non remuneree soumise a validation RH.' },
    { value: 'PATERNITY', label: 'Paternite', helper: 'Conge extraordinaire a instruire selon justificatif.' },
    { value: 'MOVING', label: 'Demenagement', helper: 'Conge extraordinaire lie a un changement de domicile.' },
    { value: 'MARRIAGE', label: 'Mariage / PACS', helper: 'Conge extraordinaire evenement familial.' },
    { value: 'BIRTH_OR_ADOPTION', label: 'Naissance / adoption', helper: "Demande exceptionnelle liee a l'arrivee d'un enfant." },
    { value: 'FAMILY_CARE', label: 'Assistance familiale', helper: 'Absence liee a une situation familiale specifique.' }
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

  constructor() {
    this.loadWorkspace();
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
        this.toastService.success('Demande de conge transmise.');
        this.leaveForm.patchValue({
          startDate: this.toDateInput(new Date()),
          endDate: this.toDateInput(new Date()),
          comment: ''
        });
        this.loadWorkspace();
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
    this.api.loadEmployeeWorkspace().subscribe({
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
}
