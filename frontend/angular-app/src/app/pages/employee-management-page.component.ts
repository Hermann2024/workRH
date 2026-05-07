import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import {
  EmployeeCreateRequest,
  EmployeeProfileResponse,
  EmployeeUpdateRequest,
  WorkRhApiService
} from '../workrh-api.service';
import { ToastService } from '../services/toast.service';

@Component({
  selector: 'app-employee-management-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './employee-management-page.component.html',
  styleUrl: './page-styles.css'
})
export class EmployeeManagementPageComponent {
  private readonly api = inject(WorkRhApiService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly toastService = inject(ToastService);
  private readonly router = inject(Router);

  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly updating = signal(false);
  readonly deletingId = signal<number | null>(null);
  readonly toggleId = signal<number | null>(null);
  readonly editingId = signal<number | null>(null);
  readonly loadError = signal<string | null>(null);
  readonly employees = signal<EmployeeProfileResponse[]>([]);
  readonly activeTab = signal<'create' | 'list'>('list');

  readonly employeeForm = this.formBuilder.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
    firstName: ['', [Validators.required]],
    lastName: ['', [Validators.required]],
    countryOfResidence: ['FR', [Validators.required, Validators.minLength(2), Validators.maxLength(7)]],
    phoneNumber: [''],
    department: [''],
    jobTitle: [''],
    hireDate: [this.toDateInput(new Date()), [Validators.required]]
  });

  readonly editForm = this.formBuilder.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    firstName: ['', [Validators.required]],
    lastName: ['', [Validators.required]],
    countryOfResidence: ['FR', [Validators.required, Validators.minLength(2), Validators.maxLength(7)]],
    phoneNumber: [''],
    department: [''],
    jobTitle: [''],
    hireDate: [this.toDateInput(new Date()), [Validators.required]],
    active: [true]
  });

  constructor() {
    this.refreshEmployees();
  }

  createEmployee(): void {
    if (this.employeeForm.invalid) {
      this.employeeForm.markAllAsTouched();
      this.toastService.error('Completez les champs obligatoires du salarie.');
      return;
    }

    const raw = this.employeeForm.getRawValue();
    const countryOfResidence = raw.countryOfResidence.trim().toUpperCase();
    const request: EmployeeCreateRequest = {
      email: raw.email.trim().toLowerCase(),
      password: raw.password,
      firstName: raw.firstName.trim(),
      lastName: raw.lastName.trim(),
      countryOfResidence,
      phoneNumber: raw.phoneNumber.trim() || null,
      department: raw.department.trim() || null,
      jobTitle: raw.jobTitle.trim() || null,
      crossBorderWorker: countryOfResidence !== 'LU',
      hireDate: raw.hireDate,
      roles: ['EMPLOYEE']
    };

    this.saving.set(true);
    this.api.createEmployee(request).subscribe({
      next: (employee) => {
        this.saving.set(false);
        this.toastService.success(`Salarie cree : ${employee.firstName} ${employee.lastName}.`);
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
        this.refreshEmployees(false);
        this.activeTab.set('list');
      },
      error: (error) => {
        this.saving.set(false);
        this.toastService.error(this.readBackendMessage(error, 'Impossible de creer le salarie.'));
      }
    });
  }

  toggleEmployeeActive(employee: EmployeeProfileResponse): void {
    this.toggleId.set(employee.id);
    const request$ = employee.active
      ? this.api.deactivateEmployee(employee.id)
      : this.api.activateEmployee(employee.id);

    request$.subscribe({
      next: () => {
        this.toggleId.set(null);
        this.toastService.info(employee.active ? 'Compte salarie desactive.' : 'Compte salarie reactive.');
        this.refreshEmployees(false);
      },
      error: (error) => {
        this.toggleId.set(null);
        this.toastService.error(this.readBackendMessage(error, 'Impossible de modifier ce compte.'));
      }
    });
  }

  startEdit(employee: EmployeeProfileResponse): void {
    this.editingId.set(employee.id);
    this.editForm.reset({
      email: employee.email,
      firstName: employee.firstName,
      lastName: employee.lastName,
      countryOfResidence: employee.countryOfResidence ?? 'FR',
      phoneNumber: employee.phoneNumber ?? '',
      department: employee.department ?? '',
      jobTitle: employee.jobTitle ?? '',
      hireDate: employee.hireDate ?? this.toDateInput(new Date()),
      active: employee.active
    });
  }

  cancelEdit(): void {
    this.editingId.set(null);
  }

  openEmployeeAbsences(employee: EmployeeProfileResponse): void {
    void this.router.navigate(['/employee', employee.id], {
      queryParams: { tab: 'leave' }
    });
  }

  updateEmployee(): void {
    const employeeId = this.editingId();
    if (employeeId == null) {
      return;
    }
    if (this.editForm.invalid) {
      this.editForm.markAllAsTouched();
      this.toastService.error('Completez les champs obligatoires avant de sauvegarder.');
      return;
    }

    const raw = this.editForm.getRawValue();
    const countryOfResidence = raw.countryOfResidence.trim().toUpperCase();
    const request: EmployeeUpdateRequest = {
      email: raw.email.trim().toLowerCase(),
      firstName: raw.firstName.trim(),
      lastName: raw.lastName.trim(),
      countryOfResidence,
      phoneNumber: raw.phoneNumber.trim() || null,
      department: raw.department.trim() || null,
      jobTitle: raw.jobTitle.trim() || null,
      crossBorderWorker: countryOfResidence !== 'LU',
      hireDate: raw.hireDate,
      roles: ['EMPLOYEE'],
      active: raw.active
    };

    this.updating.set(true);
    this.api.updateEmployee(employeeId, request).subscribe({
      next: () => {
        this.updating.set(false);
        this.editingId.set(null);
        this.toastService.success('Salarie modifie.');
        this.refreshEmployees(false);
      },
      error: (error) => {
        this.updating.set(false);
        this.toastService.error(this.readBackendMessage(error, 'Impossible de modifier le salarie.'));
      }
    });
  }

  deleteEmployee(employee: EmployeeProfileResponse): void {
    const confirmed = window.confirm(`Supprimer le compte de ${employee.firstName} ${employee.lastName} ?`);
    if (!confirmed) {
      return;
    }

    this.deletingId.set(employee.id);
    this.api.deleteEmployee(employee.id).subscribe({
      next: () => {
        this.deletingId.set(null);
        if (this.editingId() === employee.id) {
          this.editingId.set(null);
        }
        this.toastService.info('Salarie supprime.');
        this.refreshEmployees(false);
      },
      error: (error) => {
        this.deletingId.set(null);
        this.toastService.error(this.readBackendMessage(error, 'Impossible de supprimer le salarie.'));
      }
    });
  }

  private refreshEmployees(showLoading = true): void {
    if (showLoading) {
      this.loading.set(true);
    }
    this.api.getEmployees().subscribe({
      next: (employees) => {
        this.employees.set(employees);
        this.loadError.set(null);
        this.loading.set(false);
      },
      error: (error) => {
        this.loadError.set(this.readBackendMessage(error, 'Impossible de charger les salariés.'));
        this.loading.set(false);
      }
    });
  }

  private readBackendMessage(error: unknown, fallback: string): string {
    const backendMessage = (error as { error?: { message?: string } })?.error?.message;
    return typeof backendMessage === 'string' && backendMessage.trim()
      ? backendMessage
      : fallback;
  }

  private toDateInput(date: Date): string {
    return date.toISOString().slice(0, 10);
  }
}
