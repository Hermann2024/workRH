import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { WorkRhApiService, WorkRhVm } from '../workrh-api.service';
import { LoadingSkeletonComponent } from '../components/loading-skeleton.component';

@Component({
  selector: 'app-dashboard-page',
  standalone: true,
  imports: [CommonModule, LoadingSkeletonComponent],
  templateUrl: './dashboard-page.component.html',
  styleUrl: './page-styles.css'
})
export class DashboardPageComponent {
  private readonly api = inject(WorkRhApiService);

  readonly loading = signal(true);
  readonly vm = signal<WorkRhVm | null>(null);
  readonly currentPlan = computed(() => {
    const viewModel = this.vm();
    return viewModel?.plans.find((plan) => plan.code === viewModel.subscription.planCode) ?? null;
  });

  constructor() {
    this.api.loadViewModel().subscribe({
      next: (viewModel: WorkRhVm) => {
        this.vm.set(viewModel);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  isEntitled(feature: string): boolean {
    return this.vm()?.subscription.entitlements.includes(feature) ?? false;
  }
}
