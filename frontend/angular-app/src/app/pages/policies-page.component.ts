import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { WorkRhApiService, WorkRhVm } from '../workrh-api.service';

@Component({
  selector: 'app-policies-page',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './policies-page.component.html',
  styleUrl: './page-styles.css'
})
export class PoliciesPageComponent {
  private readonly api = inject(WorkRhApiService);

  readonly loading = signal(true);
  readonly vm = signal<WorkRhVm | null>(null);
  readonly policyStats = computed(() => {
    const summary = this.vm()?.companySummary;
    if (!summary) {
      return [];
    }

    return [
      { label: 'Employes suivis', value: summary.totalEmployeesTracked },
      { label: 'Jours annuels utilises', value: summary.totalAnnualUsedDays },
      { label: 'Alerte fiscale', value: summary.totalEmployeesOverFiscalLimit },
      { label: 'Alerte hebdo', value: summary.totalEmployeesOverWeeklyPolicy }
    ];
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
}
