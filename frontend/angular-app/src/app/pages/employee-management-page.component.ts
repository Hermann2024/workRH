import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import {
  EmployeeCreateRequest,
  EmployeeGender,
  EmploymentContractType,
  EmployeeInvitationRequest,
  EmployeeInvitationResponse,
  EmployeeProfileResponse,
  TenantWorkspaceResponse,
  EmployeeUpdateRequest,
  WorkRhApiService
} from '../workrh-api.service';
import { ToastService } from '../services/toast.service';

interface CsvEmployeeImportRow {
  lineNumber: number;
  email: string;
  firstName: string;
  lastName: string;
  countryOfResidence: string;
  phoneNumber: string;
  department: string;
  jobTitle: string;
  birthDate: string;
  gender: EmployeeGender;
  contractType: EmploymentContractType;
  hireDate: string;
  errors: string[];
}

interface CsvEmployeeImportSummary {
  imported: number;
  failed: number;
  errors: string[];
}

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
  readonly inviting = signal(false);
  readonly invitationLink = signal<string | null>(null);
  readonly importRows = signal<CsvEmployeeImportRow[]>([]);
  readonly importFileName = signal<string | null>(null);
  readonly importRunning = signal(false);
  readonly importSummary = signal<CsvEmployeeImportSummary | null>(null);
  readonly loadError = signal<string | null>(null);
  readonly employees = signal<EmployeeProfileResponse[]>([]);
  readonly invitations = signal<EmployeeInvitationResponse[]>([]);
  readonly workspace = signal<TenantWorkspaceResponse | null>(null);
  readonly activeTab = signal<'invite' | 'create' | 'import' | 'list'>('list');
  readonly contractTypes: EmploymentContractType[] = ['CDI', 'CDD', 'STAGE', 'ALTERNANCE', 'AUTRES'];
  readonly genderTypes: EmployeeGender[] = ['FEMININ', 'MASCULIN', 'AUTRES'];

  readonly employeeForm = this.formBuilder.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
    firstName: ['', [Validators.required]],
    lastName: ['', [Validators.required]],
    countryOfResidence: ['FR', [Validators.required, Validators.minLength(2), Validators.maxLength(7)]],
    phoneNumber: [''],
    department: [''],
    jobTitle: [''],
    birthDate: [''],
    gender: ['AUTRES' as EmployeeGender, [Validators.required]],
    contractType: ['CDI' as EmploymentContractType, [Validators.required]],
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
    birthDate: [''],
    gender: ['AUTRES' as EmployeeGender, [Validators.required]],
    contractType: ['CDI' as EmploymentContractType, [Validators.required]],
    hireDate: [this.toDateInput(new Date()), [Validators.required]],
    active: [true]
  });

  readonly invitationForm = this.formBuilder.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    firstName: ['', [Validators.required]],
    lastName: ['', [Validators.required]],
    countryOfResidence: ['FR', [Validators.required, Validators.minLength(2), Validators.maxLength(7)]],
    phoneNumber: [''],
    department: [''],
    jobTitle: [''],
    birthDate: [''],
    gender: ['AUTRES' as EmployeeGender, [Validators.required]],
    contractType: ['CDI' as EmploymentContractType, [Validators.required]],
    hireDate: [this.toDateInput(new Date()), [Validators.required]]
  });

  constructor() {
    this.refreshEmployees();
    this.refreshInvitations();
    this.refreshWorkspace();
  }

  createEmployee(): void {
    if (this.isSeatLimitReached()) {
      this.toastService.error('Limite de sieges atteinte. Augmentez les sieges ou desactivez un compte.');
      return;
    }
    if (this.employeeForm.invalid) {
      this.employeeForm.markAllAsTouched();
      this.toastService.error('Complétez les champs obligatoires du salarié.');
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
      birthDate: raw.birthDate || null,
      gender: raw.gender,
      contractType: raw.contractType,
      crossBorderWorker: countryOfResidence !== 'LU',
      hireDate: raw.hireDate,
      roles: ['EMPLOYEE']
    };

    this.saving.set(true);
    this.api.createEmployee(request).subscribe({
      next: (employee) => {
        this.saving.set(false);
        this.toastService.success(`Salarié cree : ${employee.firstName} ${employee.lastName}.`);
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
        this.refreshEmployees(false);
        this.activeTab.set('list');
      },
      error: (error) => {
        this.saving.set(false);
        this.toastService.error(this.readBackendMessage(error, 'Impossible de créer le salarié.'));
      }
    });
  }

  inviteEmployee(): void {
    if (this.isSeatLimitReached()) {
      this.toastService.error('Limite de sieges atteinte. Augmentez les sieges ou desactivez un compte.');
      return;
    }
    if (this.invitationForm.invalid) {
      this.invitationForm.markAllAsTouched();
      this.toastService.error('Complétez les champs obligatoires de l invitation.');
      return;
    }

    const raw = this.invitationForm.getRawValue();
    const countryOfResidence = raw.countryOfResidence.trim().toUpperCase();
    const request: EmployeeInvitationRequest = {
      email: raw.email.trim().toLowerCase(),
      firstName: raw.firstName.trim(),
      lastName: raw.lastName.trim(),
      countryOfResidence,
      phoneNumber: raw.phoneNumber.trim() || null,
      department: raw.department.trim() || null,
      jobTitle: raw.jobTitle.trim() || null,
      birthDate: raw.birthDate || null,
      gender: raw.gender,
      contractType: raw.contractType,
      hireDate: raw.hireDate
    };

    this.inviting.set(true);
    this.api.inviteEmployee(request).subscribe({
      next: (invitation) => {
        this.inviting.set(false);
        this.invitationLink.set(this.buildInvitationUrl(invitation.token));
        this.toastService.success(invitation.emailSent ? 'Invitation envoyee par email.' : 'Invitation creee. Copiez le lien pour l envoyer.');
        this.invitationForm.reset({
          email: '',
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
        this.refreshInvitations();
      },
      error: (error) => {
        this.inviting.set(false);
        this.toastService.error(this.readBackendMessage(error, 'Impossible de créer l invitation.'));
      }
    });
  }

  copyInvitationLink(link: string): void {
    if (!navigator.clipboard) {
      this.toastService.info(link);
      return;
    }
    navigator.clipboard.writeText(link).then(
      () => this.toastService.success('Lien copie.'),
      () => this.toastService.info(link)
    );
  }

  downloadEmployeeCsvTemplate(): void {
    const content = [
      'email,firstName,lastName,countryOfResidence,department,jobTitle,birthDate,gender,contractType,hireDate,phoneNumber',
      'marie.dupont@example.com,Marie,Dupont,FR,RH,HR Manager,1990-04-12,FEMININ,CDI,2024-01-15,+33600000000',
      'sam.muller@example.com,Sam,Muller,BE,Finance,Comptable,1988-09-30,AUTRES,CDD,2025-03-01,+32400000000'
    ].join('\n');
    this.saveBlob(new Blob([content], { type: 'text/csv;charset=utf-8' }), 'workrh-modele-import-salariés.csv');
  }

  async handleEmployeeCsvUpload(event: Event): Promise<void> {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    this.importSummary.set(null);
    if (!file) {
      return;
    }

    this.importFileName.set(file.name);
    const text = await file.text();
    const rows = this.parseEmployeeCsv(text);
    this.importRows.set(rows);
    const invalidRows = rows.filter((row) => row.errors.length > 0).length;
    if (rows.length === 0) {
      this.toastService.error('Aucune ligne salarié exploitable dans ce fichier.');
    } else if (invalidRows > 0) {
      this.toastService.error(`${invalidRows} ligne(s) a corriger avant import.`);
    } else {
      this.toastService.success(`${rows.length} salarié(s) pret(s) a inviter.`);
    }
    input.value = '';
  }

  async importCsvEmployees(): Promise<void> {
    const rows = this.importRows();
    const invalidRows = rows.filter((row) => row.errors.length > 0);
    if (rows.length === 0) {
      this.toastService.error('Chargez un fichier CSV avant de lancer l import.');
      return;
    }
    if (invalidRows.length > 0) {
      this.toastService.error('Corrigez les lignes invalides avant de lancer l import.');
      return;
    }

    const workspace = this.workspace();
    if (workspace?.seatsPurchased != null) {
      const remainingSeats = Math.max(workspace.seatsPurchased - workspace.seatsUsed, 0);
      if (rows.length > remainingSeats) {
        this.toastService.error(`Import bloque : ${remainingSeats} siege(s) disponible(s) pour ${rows.length} salarié(s).`);
        return;
      }
    }

    this.importRunning.set(true);
    const errors: string[] = [];
    let imported = 0;
    for (const row of rows) {
      try {
        await firstValueFrom(this.api.inviteEmployee(this.toInvitationRequest(row)));
        imported += 1;
      } catch (error) {
        errors.push(`Ligne ${row.lineNumber} (${row.email}) : ${this.readBackendMessage(error, 'import impossible')}`);
      }
    }

    this.importRunning.set(false);
    this.importSummary.set({ imported, failed: errors.length, errors });
    this.refreshInvitations();
    this.refreshWorkspace();
    if (errors.length === 0) {
      this.importRows.set([]);
      this.importFileName.set(null);
      this.toastService.success(`${imported} invitation(s) creee(s).`);
    } else {
      this.toastService.error(`${imported} invitation(s) creee(s), ${errors.length} erreur(s).`);
    }
  }

  invalidImportRowCount(): number {
    return this.importRows().filter((row) => row.errors.length > 0).length;
  }

  isSeatLimitReached(): boolean {
    const workspace = this.workspace();
    return !!workspace?.seatLimitExceeded || (
      workspace?.seatsPurchased != null && workspace.seatsUsed >= workspace.seatsPurchased
    );
  }

  toggleEmployeeActive(employee: EmployeeProfileResponse): void {
    this.toggleId.set(employee.id);
    const request$ = employee.active
      ? this.api.deactivateEmployee(employee.id)
      : this.api.activateEmployee(employee.id);

    request$.subscribe({
      next: () => {
        this.toggleId.set(null);
        this.toastService.info(employee.active ? 'Compte salarié désactivé.' : 'Compte salarié réactivé.');
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
      birthDate: employee.birthDate ?? '',
      gender: employee.gender ?? 'AUTRES',
      contractType: employee.contractType ?? 'CDI',
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
      this.toastService.error('Complétez les champs obligatoires avant de sauvegarder.');
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
      birthDate: raw.birthDate || null,
      gender: raw.gender,
      contractType: raw.contractType,
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
        this.toastService.success('Salarié modifie.');
        this.refreshEmployees(false);
      },
      error: (error) => {
        this.updating.set(false);
        this.toastService.error(this.readBackendMessage(error, 'Impossible de modifier le salarié.'));
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
        this.toastService.info('Salarié supprime.');
        this.refreshEmployees(false);
      },
      error: (error) => {
        this.deletingId.set(null);
        this.toastService.error(this.readBackendMessage(error, 'Impossible de supprimer le salarié.'));
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

  private refreshEmployees(showLoading = true): void {
    if (showLoading) {
      this.loading.set(true);
    }
    this.api.getEmployees().subscribe({
      next: (employees) => {
        this.employees.set(employees);
        this.loadError.set(null);
        this.loading.set(false);
        this.refreshWorkspace();
      },
      error: (error) => {
        this.loadError.set(this.readBackendMessage(error, 'Impossible de charger les salariés.'));
        this.loading.set(false);
      }
    });
  }

  private refreshInvitations(): void {
    this.api.getEmployeeInvitations().subscribe({
      next: (invitations) => this.invitations.set(invitations),
      error: () => this.invitations.set([])
    });
  }

  private refreshWorkspace(): void {
    this.api.getWorkspace().subscribe({
      next: (workspace) => this.workspace.set(workspace),
      error: () => this.workspace.set(null)
    });
  }

  private buildInvitationUrl(token: string | null): string | null {
    if (!token) {
      return null;
    }
    return `${window.location.origin}/invite?token=${encodeURIComponent(token)}`;
  }

  private parseEmployeeCsv(content: string): CsvEmployeeImportRow[] {
    const lines = content.split(/\r?\n/).filter((line) => line.trim().length > 0);
    if (lines.length <= 1) {
      return [];
    }

    const headers = this.splitCsvLine(lines[0]).map((header) => header.trim());
    const seenEmails = new Set(this.employees().map((employee) => employee.email.toLowerCase()));
    const importedEmails = new Set<string>();
    return lines.slice(1).map((line, index) => {
      const values = this.splitCsvLine(line);
      const raw: Record<string, string> = {};
      headers.forEach((header, valueIndex) => {
        raw[header] = values[valueIndex]?.trim() ?? '';
      });

      const email = (raw.email ?? '').toLowerCase();
      const countryOfResidence = (raw.countryOfResidence || 'FR').toUpperCase();
      const rawGender = (raw.gender || '').trim().toUpperCase();
      const rawContractType = (raw.contractType || '').trim().toUpperCase();
      const gender = this.normalizeGender(raw.gender);
      const contractType = this.normalizeContractType(raw.contractType);
      const row: CsvEmployeeImportRow = {
        lineNumber: index + 2,
        email,
        firstName: raw.firstName ?? '',
        lastName: raw.lastName ?? '',
        countryOfResidence,
        phoneNumber: raw.phoneNumber ?? '',
        department: raw.department ?? '',
        jobTitle: raw.jobTitle ?? '',
        birthDate: raw.birthDate ?? '',
        gender,
        contractType,
        hireDate: raw.hireDate || this.toDateInput(new Date()),
        errors: []
      };

      if (!this.isEmail(row.email)) {
        row.errors.push('email invalide');
      }
      if (!row.firstName) {
        row.errors.push('prenom manquant');
      }
      if (!row.lastName) {
        row.errors.push('nom manquant');
      }
      if (!row.countryOfResidence || row.countryOfResidence.length < 2) {
        row.errors.push('pays invalide');
      }
      if (rawGender && !this.genderTypes.includes(rawGender as EmployeeGender)) {
        row.errors.push('genre invalide');
      }
      if (rawContractType && !this.contractTypes.includes(rawContractType as EmploymentContractType)) {
        row.errors.push('contrat invalide');
      }
      if (row.birthDate && !this.isIsoDate(row.birthDate)) {
        row.errors.push('date de naissance invalide');
      }
      if (row.hireDate && !this.isIsoDate(row.hireDate)) {
        row.errors.push('date embauche invalide');
      }
      if (seenEmails.has(row.email) || importedEmails.has(row.email)) {
        row.errors.push('email deja present');
      }
      importedEmails.add(row.email);
      return row;
    });
  }

  private splitCsvLine(line: string): string[] {
    const values: string[] = [];
    let current = '';
    let quoted = false;
    for (let index = 0; index < line.length; index += 1) {
      const char = line[index];
      const next = line[index + 1];
      if (char === '"' && quoted && next === '"') {
        current += '"';
        index += 1;
      } else if (char === '"') {
        quoted = !quoted;
      } else if (char === ',' && !quoted) {
        values.push(current);
        current = '';
      } else {
        current += char;
      }
    }
    values.push(current);
    return values;
  }

  private normalizeGender(value: string | undefined): EmployeeGender {
    const normalized = (value || 'AUTRES').trim().toUpperCase();
    return this.genderTypes.includes(normalized as EmployeeGender) ? normalized as EmployeeGender : 'AUTRES';
  }

  private normalizeContractType(value: string | undefined): EmploymentContractType {
    const normalized = (value || 'CDI').trim().toUpperCase();
    return this.contractTypes.includes(normalized as EmploymentContractType) ? normalized as EmploymentContractType : 'CDI';
  }

  private toInvitationRequest(row: CsvEmployeeImportRow): EmployeeInvitationRequest {
    return {
      email: row.email,
      firstName: row.firstName,
      lastName: row.lastName,
      countryOfResidence: row.countryOfResidence,
      phoneNumber: row.phoneNumber || null,
      department: row.department || null,
      jobTitle: row.jobTitle || null,
      birthDate: row.birthDate || null,
      gender: row.gender,
      contractType: row.contractType,
      hireDate: row.hireDate || null
    };
  }

  private isEmail(value: string): boolean {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
  }

  private isIsoDate(value: string): boolean {
    return /^\d{4}-\d{2}-\d{2}$/.test(value) && !Number.isNaN(Date.parse(value));
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

  private saveBlob(blob: Blob, filename: string): void {
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    link.click();
    window.URL.revokeObjectURL(url);
  }
}
