import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import {
  EmployeeInvitationResponse,
  EmployeeProfileResponse,
  TenantWorkspaceResponse,
  WorkRhApiService,
  WorkRhVm
} from '../workrh-api.service';
import { ToastService } from '../services/toast.service';

type OnboardingStepStatus = 'DONE' | 'TODO' | 'BLOCKED';

interface OnboardingStep {
  id: string;
  title: string;
  detail: string;
  status: OnboardingStepStatus;
  actionLabel: string;
  route?: string;
}

@Component({
  selector: 'app-onboarding-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './onboarding-page.component.html',
  styleUrl: './page-styles.css'
})
export class OnboardingPageComponent {
  private readonly api = inject(WorkRhApiService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly toastService = inject(ToastService);
  private readonly router = inject(Router);

  readonly loading = signal(true);
  readonly savingWorkspace = signal(false);
  readonly loadError = signal<string | null>(null);
  readonly workspace = signal<TenantWorkspaceResponse | null>(null);
  readonly vm = signal<WorkRhVm | null>(null);
  readonly employees = signal<EmployeeProfileResponse[]>([]);
  readonly invitations = signal<EmployeeInvitationResponse[]>([]);

  readonly workspaceForm = this.formBuilder.nonNullable.group({
    companyName: ['', [Validators.required, Validators.minLength(2)]]
  });

  readonly steps = computed(() => this.buildSteps());
  readonly progress = computed(() => {
    const steps = this.steps();
    if (!steps.length) {
      return 0;
    }
    return Math.round((steps.filter((step) => step.status === 'DONE').length * 100) / steps.length);
  });
  readonly nextStep = computed(() =>
    this.steps().find((step) => step.status === 'BLOCKED')
    ?? this.steps().find((step) => step.status === 'TODO')
    ?? null
  );

  constructor() {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    forkJoin({
      workspace: this.api.getWorkspace(),
      vm: this.api.loadViewModel(new Date()),
      employees: this.api.getEmployees(),
      invitations: this.api.getEmployeeInvitations()
    }).subscribe({
      next: ({ workspace, vm, employees, invitations }) => {
        this.workspace.set(workspace);
        this.vm.set(vm);
        this.employees.set(employees);
        this.invitations.set(invitations);
        this.workspaceForm.reset({ companyName: workspace.companyName ?? '' });
        this.loadError.set(null);
        this.loading.set(false);
      },
      error: (error) => {
        this.loadError.set(this.readBackendMessage(error, 'Impossible de charger le parcours onboarding.'));
        this.loading.set(false);
      }
    });
  }

  saveWorkspace(): void {
    if (this.workspaceForm.invalid) {
      this.workspaceForm.markAllAsTouched();
      this.toastService.error('Renseignez le nom de l entreprise.');
      return;
    }

    this.savingWorkspace.set(true);
    this.api.updateWorkspace({ companyName: this.workspaceForm.controls.companyName.value.trim() }).subscribe({
      next: (workspace) => {
        this.workspace.set(workspace);
        this.savingWorkspace.set(false);
        this.toastService.success('Entreprise mise a jour.');
      },
      error: (error) => {
        this.savingWorkspace.set(false);
        this.toastService.error(this.readBackendMessage(error, 'Impossible de mettre a jour l entreprise.'));
      }
    });
  }

  runStep(step: OnboardingStep): void {
    if (step.route) {
      void this.router.navigateByUrl(step.route);
      return;
    }
    if (step.id === 'workspace') {
      this.saveWorkspace();
    }
  }

  statusLabel(status: OnboardingStepStatus): string {
    return status === 'DONE' ? 'Pret' : status === 'BLOCKED' ? 'Bloquant' : 'A faire';
  }

  statusClass(status: OnboardingStepStatus): string {
    return status === 'DONE' ? 'level-ok' : status === 'BLOCKED' ? 'level-critical' : 'level-warning';
  }

  private buildSteps(): OnboardingStep[] {
    const workspace = this.workspace();
    const vm = this.vm();
    const employees = this.employees();
    const invitations = this.invitations();
    const activeEmployees = employees.filter((employee) => employee.active).length;
    const completedProfiles = employees.filter((employee) =>
      Boolean(employee.birthDate)
      && Boolean(employee.gender)
      && Boolean(employee.contractType)
      && Boolean(employee.hireDate)
      && Boolean(employee.countryOfResidence)
    ).length;
    const entitlements = vm?.subscription.entitlements ?? [];
    const pendingInvitations = invitations.filter((invitation) => invitation.status === 'PENDING').length;

    return [
      {
        id: 'workspace',
        title: 'Identifier l entreprise',
        detail: workspace?.companyName ? `${workspace.companyName} est configuree.` : 'Le nom de l entreprise doit etre renseigne.',
        status: workspace?.companyName ? 'DONE' : 'BLOCKED',
        actionLabel: 'Enregistrer'
      },
      {
        id: 'subscription',
        title: 'Valider abonnement et sieges',
        detail: vm ? `${vm.subscription.planCode} actif, ${vm.subscription.seatsPurchased} siege(s).` : 'Abonnement non charge.',
        status: vm?.subscription.status === 'ACTIVE' ? 'DONE' : 'BLOCKED',
        actionLabel: 'Voir billing',
        route: '/billing'
      },
      {
        id: 'employees',
        title: 'Importer ou inviter les salaries',
        detail: employees.length ? `${activeEmployees}/${employees.length} compte(s) actif(s), ${pendingInvitations} invitation(s) en attente.` : 'Aucun salarie importe.',
        status: employees.length > 0 || pendingInvitations > 0 ? 'DONE' : 'BLOCKED',
        actionLabel: 'Importer CSV',
        route: '/employees'
      },
      {
        id: 'profiles',
        title: 'Completer les profils RH',
        detail: employees.length ? `${completedProfiles}/${employees.length} profil(s) complets.` : 'Les profils seront controles apres import.',
        status: employees.length > 0 && completedProfiles === employees.length ? 'DONE' : employees.length > 0 ? 'TODO' : 'BLOCKED',
        actionLabel: 'Completer',
        route: '/employees'
      },
      {
        id: 'compliance',
        title: 'Activer le cadre legal frontalier',
        detail: entitlements.includes('TELEWORK_COMPLIANCE_34') ? 'Controle 34 jours, dossier legal et politiques disponibles.' : 'Le plan courant ne donne pas acces a la conformite frontaliere.',
        status: entitlements.includes('TELEWORK_COMPLIANCE_34') ? 'DONE' : 'TODO',
        actionLabel: 'Voir politiques',
        route: '/policies'
      },
      {
        id: 'exports',
        title: 'Preparer exports et preuves audit',
        detail: entitlements.includes('EXPORTS') ? 'Exports dashboard et dossier legal disponibles.' : 'Les exports doivent etre inclus dans le plan ou option.',
        status: entitlements.includes('EXPORTS') ? 'DONE' : 'TODO',
        actionLabel: 'Ouvrir dashboard',
        route: '/dashboard'
      },
      {
        id: 'connectors',
        title: 'Cadrer les connecteurs SIRH / paie',
        detail: 'Workday, SAP, Lucca, Payfit et Factorial sont presentes. La synchronisation native reste a configurer provider par provider.',
        status: 'TODO',
        actionLabel: 'Voir services',
        route: '/services'
      }
    ];
  }

  private readBackendMessage(error: unknown, fallback: string): string {
    const backendMessage = (error as { error?: { message?: string } })?.error?.message;
    return typeof backendMessage === 'string' && backendMessage.trim()
      ? backendMessage
      : fallback;
  }
}
