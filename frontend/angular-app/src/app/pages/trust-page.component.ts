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
  evidence: string;
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

  downloadTrustPack(): void {
    const sections = [
      this.buildRoleMatrix(),
      this.buildGdprRegister(),
      this.buildRetentionPolicy(),
      this.buildIncidentPlan(),
      this.buildSlaPolicy()
    ];
    this.saveBlob(
      new Blob([sections.join('\n\n---\n\n')], { type: 'text/markdown;charset=utf-8' }),
      'workrh-trust-pack.md'
    );
  }

  downloadDocument(type: 'roles' | 'gdpr' | 'retention' | 'incident' | 'sla'): void {
    const documents = {
      roles: { name: 'workrh-matrice-roles.md', content: this.buildRoleMatrix() },
      gdpr: { name: 'workrh-registre-rgpd.md', content: this.buildGdprRegister() },
      retention: { name: 'workrh-politique-conservation.md', content: this.buildRetentionPolicy() },
      incident: { name: 'workrh-plan-incident.md', content: this.buildIncidentPlan() },
      sla: { name: 'workrh-sla-support.md', content: this.buildSlaPolicy() }
    };
    const document = documents[type];
    this.saveBlob(new Blob([document.content], { type: 'text/markdown;charset=utf-8' }), document.name);
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
        detail: workspace?.tenantId ? `Tenant actif ${workspace.tenantId}. Les endpoints critiques utilisent le tenant du JWT/TenantContext et les lectures par ID filtrent par tenant.` : 'Tenant non chargé.',
        status: workspace?.tenantId ? 'READY' : 'TODO',
        owner: 'Backend',
        evidence: 'TenantContext obligatoire, controle X-Tenant-Id vs JWT, repositories findByIdAndTenantId.'
      },
      {
        title: 'Roles et accès',
        detail: 'Navigation et endpoints separes pour RH, admin plateforme et employé. La matrice roles est exportable.',
        status: 'READY',
        owner: 'Produit',
        evidence: 'Matrice roles incluse dans le Trust Pack.'
      },
      {
        title: 'Données sensibles RH',
        detail: `${employeesWithSensitiveProfile}/${employees.length} profil(s) contiennent des données personnelles sensibles a proteger.`,
        status: 'READY',
        owner: 'DPO',
        evidence: 'Registre RGPD et politique de conservation exportables.'
      },
      {
        title: 'Exports contrôles',
        detail: entitlements.includes('EXPORTS') ? 'Exports limites par plan et droit fonctionnel.' : 'Exports desactives tant que le plan ne donne pas le droit.',
        status: 'READY',
        owner: 'Produit',
        evidence: 'Controle par entitlement EXPORTS et routes authentifiees.'
      },
      {
        title: 'Audit conformité',
        detail: entitlements.includes('DECLARATION_AUDIT') ? 'Historique déclaratif et traces conformité disponibles.' : 'Audit avancé désactivé tant que le plan ne donne pas le droit.',
        status: 'READY',
        owner: 'Legal',
        evidence: 'Audit disponible selon plan, documente dans le Trust Pack.'
      },
      {
        title: 'RGPD et conservation',
        detail: 'Registre de traitement, retention et procedure droits utilisateur disponibles en export.',
        status: 'READY',
        owner: 'DPO',
        evidence: 'Registre RGPD + politique conservation telechargeables.'
      },
      {
        title: 'Fichiers justificatifs',
        detail: 'Justificatifs limites a PDF/JPG/PNG, 10 MB, noms normalises et empreinte SHA-256 stockee pour integrite.',
        status: 'READY',
        owner: 'Security',
        evidence: 'Validation MIME/taille + hash SHA-256 cote leave/sickness.'
      },
      {
        title: 'Incident et support',
        detail: 'Support applicatif, SLA indicatif, categories incident et plan de reponse exportable.',
        status: 'READY',
        owner: 'Ops',
        evidence: 'Plan incident + SLA support telechargeables.'
      }
    ];
  }

  private buildRoleMatrix(): string {
    return `# WorkRH - Matrice roles et accès

| Role | Accès principal | Limites tenant |
| --- | --- | --- |
| EMPLOYEE | Espace employé, absences et justificatifs personnels | Accès limite a employeeId du JWT |
| HR | Dashboard, employés, absences, conformité, exports selon plan | Données filtrees par tenant du JWT |
| ADMIN | Administration tenant, support et billing | Données filtrees par tenant du JWT |
| PLATFORM_ADMIN | Supervision plateforme | Accès transverse reserve aux opérations internes |

Regle: un header X-Tenant-Id ne peut pas contredire le tenant porte par le JWT.`;
  }

  private buildGdprRegister(): string {
    const tenantId = this.workspace()?.tenantId ?? 'tenant courant';
    return `# WorkRH - Registre RGPD

Tenant: ${tenantId}

| Traitement | Données | Finalite | Base | Accès |
| --- | --- | --- | --- | --- |
| Gestion salariés | Identite, email, contrat, residence | Administration RH | Contrat / interet legitime | HR, ADMIN |
| Absences | Dates, type, justificatifs | Gestion congés et maladie | Obligation legale / contrat | HR, employé concerne |
| Télétravail frontalier | Jours, pays, seuils | Controle fiscal/social | Obligation legale / interet legitime | HR, ADMIN |
| Support | Messages, email, telephone | Assistance client | Contrat | Support, ADMIN |

Droits utilisateur: accès, rectification, export et suppression a traiter via support sécurité/RGPD.`;
  }

  private buildRetentionPolicy(): string {
    return `# WorkRH - Politique de conservation

| Donnee | Conservation cible | Action |
| --- | --- | --- |
| Comptes salariés actifs | Duree du contrat client | Suppression/anonymisation a la fin |
| Invitations | 14 jours | Expiration automatique |
| Reset mot de passe | 30 minutes | Usage unique |
| Justificatifs absence/maladie | Selon obligation RH locale | Revue periodique DPO |
| Logs support | Duree contrat + besoin preuve | Purge planifiee |
| Exports telecharges | Responsabilite client apres telechargement | Mention dans DPA |

Les durees doivent etre ajustees au contrat client et au pays applicable.`;
  }

  private buildIncidentPlan(): string {
    return `# WorkRH - Plan incident

1. Qualification: sécurité, disponibilite, données personnelles, paiement.
2. Containment: suspendre endpoint, compte ou intégration concernee.
3. Analyse: logs applicatifs, tenant impacte, données exposees.
4. Notification: client et DPO selon severite, puis autorite si requis.
5. Correction: patch, rotation secret, restauration ou purge.
6. Post-mortem: cause, impact, actions preventives.

Severites: critique < 4h, bloquant < 1 jour ouvrable, standard < 3 jours ouvrables.`;
  }

  private buildSlaPolicy(): string {
    return `# WorkRH - SLA support

| Priorite | Exemple | Premiere reponse cible |
| --- | --- | --- |
| Critique | Indisponibilité, risque données | 4h ouvre |
| Bloquant | Paie/export impossible | 1 jour ouvre |
| Standard | Question, amelioration | 3 jours ouvres |

Canaux: support applicatif, ticket sécurité, suivi tenant. Le SLA contractuel final doit etre repris dans les CGV/DPA.`;
  }

  private saveBlob(blob: Blob, filename: string): void {
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    link.click();
    window.URL.revokeObjectURL(url);
  }

  private readBackendMessage(error: unknown, fallback: string): string {
    const backendMessage = (error as { error?: { message?: string } })?.error?.message;
    return typeof backendMessage === 'string' && backendMessage.trim()
      ? backendMessage
      : fallback;
  }
}
