import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LoadingSkeletonComponent } from '../components/loading-skeleton.component';
import { ServiceModuleResponse, WorkRhApiService, WorkRhVm } from '../workrh-api.service';

@Component({
  selector: 'app-services-page',
  standalone: true,
  imports: [CommonModule, RouterLink, LoadingSkeletonComponent],
  templateUrl: './services-page.component.html',
  styleUrl: './page-styles.css'
})
export class ServicesPageComponent {
  private readonly api = inject(WorkRhApiService);

  readonly loading = signal(true);
  readonly loadError = signal<string | null>(null);
  readonly vm = signal<WorkRhVm | null>(null);
  readonly modules = signal<ServiceModuleResponse[]>([]);
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
        this.loadError.set(this.readBackendMessage(error, 'Impossible de charger les services du plan.'));
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
        this.loadError.set(this.readBackendMessage(error, 'Catalogue des services indisponible.'));
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
}
