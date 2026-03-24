import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-pagination',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="pagination" *ngIf="totalPages > 1">
      <button [disabled]="currentPage === 1" 
              (click)="goToPage(currentPage - 1)"
              class="pagination-btn">
        Précédent
      </button>

      <div class="pagination-pages">
        <button *ngFor="let page of pages"
                [class.active]="page === currentPage"
                (click)="goToPage(page)"
                class="pagination-page">
          {{ page }}
        </button>
      </div>

      <button [disabled]="currentPage === totalPages" 
              (click)="goToPage(currentPage + 1)"
              class="pagination-btn">
        Suivant
      </button>
    </div>
  `,
  styles: [`
    .pagination {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
      margin-top: 24px;
    }

    .pagination-btn,
    .pagination-page {
      padding: 8px 12px;
      border: 1px solid var(--line);
      background: var(--card);
      border-radius: 8px;
      cursor: pointer;
      font: inherit;
      transition: all 0.2s ease;
      font-weight: 500;
    }

    .pagination-btn:hover:not(:disabled),
    .pagination-page:hover {
      border-color: var(--blue);
      color: var(--blue);
      background: rgba(0, 35, 149, 0.04);
    }

    .pagination-page.active {
      background: linear-gradient(135deg, var(--blue), var(--accent));
      color: white;
      border-color: transparent;
      font-weight: 700;
    }

    .pagination-btn:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }

    .pagination-pages {
      display: flex;
      gap: 4px;
    }
  `]
})
export class PaginationComponent {
  @Input() currentPage: number = 1;
  @Input() totalPages: number = 1;
  @Output() pageChange = new EventEmitter<number>();

  get pages(): number[] {
    const pages: number[] = [];
    const maxVisible = 5;
    let start = Math.max(1, this.currentPage - Math.floor(maxVisible / 2));
    let end = Math.min(this.totalPages, start + maxVisible - 1);

    if (end - start < maxVisible - 1) {
      start = Math.max(1, end - maxVisible + 1);
    }

    for (let i = start; i <= end; i++) {
      pages.push(i);
    }

    return pages;
  }

  goToPage(page: number): void {
    if (page >= 1 && page <= this.totalPages && page !== this.currentPage) {
      this.pageChange.emit(page);
    }
  }
}
