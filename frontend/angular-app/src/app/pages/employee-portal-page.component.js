var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
import { CommonModule, DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { WorkRhApiService } from '../workrh-api.service';
import { ToastService } from '../services/toast.service';
let EmployeePortalPageComponent = class EmployeePortalPageComponent {
    constructor() {
        this.api = inject(WorkRhApiService);
        this.formBuilder = inject(FormBuilder);
        this.toastService = inject(ToastService);
        this.loading = signal(true);
        this.submittingTelework = signal(false);
        this.submittingLeave = signal(false);
        this.submittingSickness = signal(false);
        this.workspace = signal(null);
        this.leaveTypeOptions = [
            { value: 'PAID', label: 'Congé payé', helper: 'Demande standard de congé annuel.' },
            { value: 'UNPAID', label: 'Congé sans solde', helper: 'Absence non rémunérée soumise à validation RH.' },
            { value: 'PATERNITY', label: 'Paternité', helper: 'Congé extraordinaire à instruire selon justificatif.' },
            { value: 'MOVING', label: 'Déménagement', helper: 'Congé extraordinaire lié à un changement de domicile.' },
            { value: 'MARRIAGE', label: 'Mariage / PACS', helper: 'Congé extraordinaire événement familial.' },
            { value: 'BIRTH_OR_ADOPTION', label: 'Naissance / adoption', helper: 'Demande exceptionnelle liée à l’arrivée d’un enfant.' },
            { value: 'FAMILY_CARE', label: 'Assistance familiale', helper: 'Absence liée à une situation familiale spécifique.' }
        ];
        this.teleworkForm = this.formBuilder.nonNullable.group({
            workDate: [this.toDateInput(new Date()), [Validators.required]],
            countryCode: ['FR', [Validators.required, Validators.minLength(2), Validators.maxLength(7)]],
            totalWorkMinutes: [480, [Validators.required, Validators.min(1)]],
            residenceTeleworkMinutes: [480, [Validators.required, Validators.min(0)]],
            residenceNonTeleworkMinutes: [0, [Validators.required, Validators.min(0)]],
            otherForeignWorkMinutes: [0, [Validators.required, Validators.min(0)]],
            otherForeignCountryCode: [''],
            connectedToEmployerInfrastructure: [true]
        });
        this.leaveForm = this.formBuilder.nonNullable.group({
            type: ['PAID', [Validators.required]],
            startDate: [this.toDateInput(new Date()), [Validators.required]],
            endDate: [this.toDateInput(new Date()), [Validators.required]],
            comment: ['']
        });
        this.sicknessForm = this.formBuilder.nonNullable.group({
            startDate: [this.toDateInput(new Date()), [Validators.required]],
            endDate: [this.toDateInput(new Date()), [Validators.required]],
            comment: ['']
        });
        this.currentMonthTeleworkCount = computed(() => {
            const currentMonthPrefix = this.toDateInput(new Date()).slice(0, 7);
            return (this.workspace()?.teleworkHistory ?? [])
                .filter((item) => item.workDate.startsWith(currentMonthPrefix) && item.residenceTeleworkMinutes > 0)
                .length;
        });
        this.requestedLeavesCount = computed(() => (this.workspace()?.leaves ?? []).filter((leave) => leave.status === 'REQUESTED').length);
        this.currentYearTeleworkCount = computed(() => {
            const currentYear = String(new Date().getFullYear());
            return (this.workspace()?.teleworkHistory ?? [])
                .filter((item) => item.workDate.startsWith(currentYear) && item.countsTowardFiscalThreshold)
                .length;
        });
        this.latestTeleworkEntries = computed(() => this.sortByDateDesc(this.workspace()?.teleworkHistory ?? [], (item) => item.workDate).slice(0, 8));
        this.latestLeaveEntries = computed(() => this.sortByDateDesc(this.workspace()?.leaves ?? [], (item) => item.startDate).slice(0, 8));
        this.latestSicknessEntries = computed(() => this.sortByDateDesc(this.workspace()?.sickness ?? [], (item) => item.startDate).slice(0, 8));
        this.loadWorkspace();
    }
    submitTelework() {
        const profile = this.workspace()?.profile;
        if (!profile || this.teleworkForm.invalid) {
            this.teleworkForm.markAllAsTouched();
            return;
        }
        this.submittingTelework.set(true);
        const formValue = this.teleworkForm.getRawValue();
        const outsideMinutes = Number(formValue.residenceTeleworkMinutes)
            + Number(formValue.residenceNonTeleworkMinutes)
            + Number(formValue.otherForeignWorkMinutes);
        if (outsideMinutes > Number(formValue.totalWorkMinutes)) {
            this.submittingTelework.set(false);
            this.toastService.error('Les minutes hors Luxembourg ne peuvent pas depasser le total travaille.');
            return;
        }
        if (Number(formValue.otherForeignWorkMinutes) > 0 && !formValue.otherForeignCountryCode.trim()) {
            this.submittingTelework.set(false);
            this.toastService.error('Indique un code pays si tu declares du temps dans un autre pays.');
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
                this.toastService.success('Jour de télétravail enregistré.');
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
                this.toastService.error(error?.error?.message || 'Impossible d’enregistrer le télétravail.');
            }
        });
    }
    submitLeave() {
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
                this.toastService.success('Demande de congé transmise.');
                this.leaveForm.patchValue({
                    startDate: this.toDateInput(new Date()),
                    endDate: this.toDateInput(new Date()),
                    comment: ''
                });
                this.loadWorkspace();
            },
            error: (error) => {
                this.submittingLeave.set(false);
                this.toastService.error(error?.error?.message || 'Impossible d’enregistrer la demande de congé.');
            }
        });
    }
    submitSickness() {
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
                this.toastService.success('Arrêt maladie déclaré.');
                this.sicknessForm.patchValue({
                    startDate: this.toDateInput(new Date()),
                    endDate: this.toDateInput(new Date()),
                    comment: ''
                });
                this.loadWorkspace();
            },
            error: (error) => {
                this.submittingSickness.set(false);
                this.toastService.error(error?.error?.message || 'Impossible d’enregistrer l’arrêt maladie.');
            }
        });
    }
    cancelLeave(leave) {
        this.api.cancelLeave(leave.id, 'Annulation demandée par le salarié').subscribe({
            next: () => {
                this.toastService.info('Demande de congé annulée.');
                this.loadWorkspace();
            },
            error: (error) => {
                this.toastService.error(error?.error?.message || 'Impossible d’annuler cette demande.');
            }
        });
    }
    leaveTypeLabel(type) {
        return this.leaveTypeOptions.find((option) => option.value === type)?.label ?? type;
    }
    selectedLeaveHelper() {
        const selectedType = this.leaveForm.controls.type.value;
        return this.leaveTypeOptions.find((option) => option.value === selectedType)?.helper
            ?? 'Ajoutez un commentaire si vous souhaitez aider le traitement RH.';
    }
    canCancelLeave(leave) {
        return leave.status === 'REQUESTED';
    }
    loadWorkspace() {
        this.loading.set(true);
        this.api.loadEmployeeWorkspace().subscribe({
            next: (workspace) => {
                this.workspace.set(workspace);
                this.teleworkForm.patchValue({
                    countryCode: (workspace.profile.countryOfResidence ?? 'FR').toUpperCase(),
                    totalWorkMinutes: workspace.teleworkSummary?.policy.standardDailyWorkMinutes ?? 480,
                    residenceTeleworkMinutes: workspace.teleworkSummary?.policy.standardDailyWorkMinutes ?? 480
                });
                this.loading.set(false);
            },
            error: () => {
                this.loading.set(false);
                this.toastService.error('Impossible de charger l’espace employé.');
            }
        });
    }
    sortByDateDesc(items, getValue) {
        return [...items].sort((left, right) => getValue(right).localeCompare(getValue(left)));
    }
    toDateInput(date) {
        return date.toISOString().slice(0, 10);
    }
};
EmployeePortalPageComponent = __decorate([
    Component({
        selector: 'app-employee-portal-page',
        standalone: true,
        imports: [CommonModule, ReactiveFormsModule, DatePipe],
        templateUrl: './employee-portal-page.component.html',
        styleUrl: './page-styles.css'
    })
], EmployeePortalPageComponent);
export { EmployeePortalPageComponent };
