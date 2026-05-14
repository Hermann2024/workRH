import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LoadingSkeletonComponent } from '../components/loading-skeleton.component';
import { I18nService } from '../i18n/i18n.service';
import { TranslatePipe } from '../i18n/translate.pipe';
import { ServiceModuleResponse, WorkRhApiService, WorkRhVm } from '../workrh-api.service';

interface ConnectorProviderVm {
  name: string;
  category: string;
  status: 'READY_EXPORT' | 'SCOPING' | 'API_REQUIRED';
  mode: string;
  fields: string[];
  rollout: string[];
}

@Component({
  selector: 'app-services-page',
  standalone: true,
  imports: [CommonModule, RouterLink, LoadingSkeletonComponent, TranslatePipe],
  templateUrl: './services-page.component.html',
  styleUrl: './page-styles.css'
})
export class ServicesPageComponent {
  private readonly api = inject(WorkRhApiService);
  private readonly i18n = inject(I18nService);

  readonly loading = signal(true);
  readonly loadError = signal<string | null>(null);
  readonly vm = signal<WorkRhVm | null>(null);
  readonly modules = signal<ServiceModuleResponse[]>([]);
  readonly connectorProviders: ConnectorProviderVm[] = [
    {
      name: 'Workday',
      category: 'SIRH',
      status: 'API_REQUIRED',
      mode: 'API REST / OAuth 2.0',
      fields: ['Identite', 'contrat', 'manager', 'residence', 'departement'],
      rollout: ['Valider scopes OAuth', 'mapper champs employes', 'tester synchro delta']
    },
    {
      name: 'SAP SuccessFactors',
      category: 'SIRH',
      status: 'API_REQUIRED',
      mode: 'OData API / OAuth',
      fields: ['User', 'Employment', 'JobInfo', 'adresse', 'statut actif'],
      rollout: ['Compte technique', 'mapping Employee Central', 'journal imports']
    },
    {
      name: 'Lucca',
      category: 'SIRH',
      status: 'SCOPING',
      mode: 'API / export CSV',
      fields: ['Collaborateur', 'contrat', 'absence', 'departement'],
      rollout: ['Valider modules Lucca', 'import initial CSV', 'automatiser API']
    },
    {
      name: 'Payfit',
      category: 'Paie',
      status: 'SCOPING',
      mode: 'Export paie / API selon contrat',
      fields: ['Identite', 'absence', 'justificatifs', 'variables paie'],
      rollout: ['Cadrer format paie', 'export mensuel', 'controle avant envoi']
    },
    {
      name: 'Factorial',
      category: 'SIRH',
      status: 'SCOPING',
      mode: 'API / CSV',
      fields: ['Employes', 'contrats', 'absences', 'equipes'],
      rollout: ['Jeton API', 'mapping pays residence', 'sync employes']
    },
    {
      name: 'BambooHR',
      category: 'SIRH',
      status: 'SCOPING',
      mode: 'API / report export',
      fields: ['Employee', 'jobTitle', 'department', 'hireDate'],
      rollout: ['Cle API', 'rapport employes', 'controle doublons']
    }
  ];
  readonly activeModules = computed(() => this.modules().filter((module) => module.enabled));
  readonly inactiveModules = computed(() => this.modules().filter((module) => !module.enabled));
  readonly categories = computed(() => {
    const categoryMap = new Map<string, ServiceModuleResponse[]>();
    for (const module of this.modules()) {
      const items = categoryMap.get(module.category) ?? [];
      items.push(module);
      categoryMap.set(module.category, items);
    }
    return Array.from(categoryMap.entries()).map(([name, items]) => ({ name, items }));
  });
  readonly currentPlan = computed(() => {
    const viewModel = this.vm();
    return viewModel?.plans.find((plan) => plan.code === viewModel.subscription.planCode) ?? null;
  });

  constructor() {
    this.api.loadViewModel().subscribe({
      next: (viewModel) => {
        this.vm.set(viewModel);
        this.loadServices();
      },
      error: (error) => {
        this.loadError.set(this.readBackendMessage(error, 'services.err_load_vm'));
        this.loading.set(false);
      }
    });
  }

  private loadServices(): void {
    this.api.getServiceModules().subscribe({
      next: (modules) => {
        this.modules.set(modules);
        this.loadError.set(null);
        this.loading.set(false);
      },
      error: (error) => {
        this.loadError.set(this.readBackendMessage(error, 'services.err_load_catalog'));
        this.loading.set(false);
      }
    });
  }

  categoryLabel(apiCategory: string): string {
    this.i18n.locale();
    const key = `svc.cat.${apiCategory}`;
    const translated = this.i18n.translate(key);
    return translated !== key ? translated : apiCategory;
  }

  moduleDisplayName(module: ServiceModuleResponse): string {
    return this.featureField(module.feature, 'name', module.name);
  }

  moduleDescription(module: ServiceModuleResponse): string {
    return this.featureField(module.feature, 'desc', module.description);
  }

  moduleActionLabel(module: ServiceModuleResponse): string {
    return this.featureField(module.feature, 'action', module.actionLabel);
  }

  connectorStatusLabel(status: ConnectorProviderVm['status']): string {
    return {
      READY_EXPORT: 'Pret export',
      SCOPING: 'Cadrage',
      API_REQUIRED: 'API requise'
    }[status];
  }

  connectorStatusClass(status: ConnectorProviderVm['status']): string {
    return status === 'READY_EXPORT' ? 'level-ok' : status === 'API_REQUIRED' ? 'level-critical' : 'level-warning';
  }

  private featureField(featureCode: string, field: 'name' | 'desc' | 'action', fallback: string): string {
    this.i18n.locale();
    const key = `feature.${featureCode}.${field}`;
    const translated = this.i18n.translate(key);
    return translated !== key ? translated : fallback;
  }

  private readBackendMessage(error: unknown, fallbackKey: string): string {
    this.i18n.locale();
    const backendMessage = (error as { error?: { message?: string } })?.error?.message;
    if (typeof backendMessage === 'string' && backendMessage.trim()) {
      return backendMessage;
    }
    return this.i18n.translate(fallbackKey);
  }
}
