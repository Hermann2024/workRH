import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import {
  EmployeeProfileResponse,
  TenantWorkspaceResponse,
  WorkRhApiService,
  WorkRhVm
} from '../workrh-api.service';

type TrustStatus = 'READY' | 'PARTIAL' | 'TODO';

interface TrustControl {
  title: string;
  detail: string;
  status: TrustStatus;
  owner: string;
}

@Component({
  selector: 'app-trust-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './trust-page.component.html',
  styleUrl: './page-styles.css'
})
export class TrustPageComponent {
  private readonly api = inject(WorkRhApiService);

  readonly loading = signal(true);
  readonly loadError = signal<string | null>(null);
  readonly workspace = signal<TenantWorkspaceResponse | null>(null);
  readonly vm = signal<WorkRhVm | null>(null);
  readonly employees = signal<EmployeeProfileResponse[]>([]);

  readonly controls = computed(() => this.buildControls());
  readonly readyControls = computed(() => this.controls().filter((control) => control.status === 'READY').length);
  readonly partialControls = computed(() => this.controls().filter((control) => control.status === 'PARTIAL').length);
  readonly todoControls = computed(() => this.controls().filter((control) => control.status === 'TODO').length);
  readonly trustScore = computed(() => {
    const controls = this.controls();
    if (!controls.length) {
      return 0;
    }
    const score = controls.reduce((sum, control) => {
      if (control.status === 'READY') {
        return sum + 1;
      }
      if (control.status === 'PARTIAL') {
        return sum + 0.55;
      }
      return sum;
    }, 0);
    return Math.round((score * 100) / controls.length);
  });

  constructor() {
    forkJoin({
      workspace: this.api.getWorkspace(),
      vm: this.api.loadViewModel(new Date()),
      employees: this.api.getEmployees()
    }).subscribe({
      next: ({ workspace, vm, employees }) => {
        this.workspace.set(workspace);
        this.vm.set(vm);
        this.employees.set(employees);
        this.loading.set(false);
        this.loadError.set(null);
      },
      error: (error) => {
        this.loadError.set(this.readBackendMessage(error, 'Impossible de charger le centre de confiance.'));
        this.loading.set(false);
      }
    });
  }

  statusLabel(status: TrustStatus): string {
    return {
      READY: 'Pret',
      PARTIAL: 'Partiel',
      TODO: 'A finaliser'
    }[status];
  }

  statusClass(status: TrustStatus): string {
    return status === 'READY' ? 'level-ok' : status === 'PARTIAL' ? 'level-warning' : 'level-critical';
  }

  private buildControls(): TrustControl[] {
    const workspace = this.workspace();
    const vm = this.vm();
    const employees = this.employees();
    const entitlements = vm?.subscription.entitlements ?? [];
    const employeesWithSensitiveProfile = employees.filter((employee) =>
      Boolean(employee.birthDate)
      || Boolean(employee.countryOfResidence)
      || Boolean(employee.phoneNumber)
    ).length;

    return [
      {
        title: 'Isolation tenant',
        detail: workspace?.tenantId ? `Tenant actif ${workspace.tenantId}. Les donnees doivent rester filtrees par tenant sur chaque API.` : 'Tenant non charge.',
        status: workspace?.tenantId ? 'PARTIAL' : 'TODO',
        owner: 'Backend'
      },
      {
        title: 'Roles et acces',
        detail: 'Navigation separee RH, admin plateforme et employe. Une matrice fine des permissions reste a documenter.',
        status: 'PARTIAL',
        owner: 'Produit'
      },
      {
        title: 'Donnees sensibles RH',
        detail: `${employeesWithSensitiveProfile}/${employees.length} profil(s) contiennent des donnees personnelles sensibles a proteger.`,
        status: employees.length ? 'PARTIAL' : 'TODO',
        owner: 'DPO'
      },
      {
        title: 'Exports controles',
        detail: entitlements.includes('EXPORTS') ? 'Exports limites par plan et droit fonctionnel.' : 'Exports non actifs sur le plan courant.',
        status: entitlements.includes('EXPORTS') ? 'READY' : 'PARTIAL',
        owner: 'Produit'
      },
      {
        title: 'Audit conformite',
        detail: entitlements.includes('DECLARATION_AUDIT') ? 'Historique declaratif et traces conformite disponibles.' : 'Audit avance non active.',
        status: entitlements.includes('DECLARATION_AUDIT') ? 'READY' : 'TODO',
        owner: 'Legal'
      },
      {
        title: 'RGPD et conservation',
        detail: 'Politique de retention, registre de traitement et procedure suppression/export utilisateur a formaliser avant production commerciale.',
        status: 'TODO',
        owner: 'DPO'
      },
      {
        title: 'Fichiers justificatifs',
        detail: 'Les justificatifs sont geres dans les workflows absence/maladie. Chiffrement stockage, antivirus et durees de conservation doivent etre verrouilles.',
        status: 'PARTIAL',
        owner: 'Security'
      },
      {
        title: 'Incident et support',
        detail: 'Support applicatif present. Il manque SLA, procedure incident, contact securite et page statut production.',
        status: 'TODO',
        owner: 'Ops'
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
