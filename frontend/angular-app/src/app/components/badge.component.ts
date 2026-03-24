import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-badge',
  standalone: true,
  imports: [CommonModule],
  template: `
    <span [ngClass]="['badge', { 'badge-alert': alert, 'badge-success': success }]">
      {{ label }}
    </span>
  `,
  styles: [`
    .badge {
      padding: 6px 10px;
      border-radius: 999px;
      background: rgba(31, 136, 61, 0.15);
      color: var(--success);
      font-weight: 600;
      font-size: 0.85rem;
      display: inline-block;
    }

    .badge-alert {
      background: rgba(218, 54, 51, 0.15);
      color: var(--error);
    }

    .badge-success {
      background: rgba(31, 136, 61, 0.15);
      color: var(--success);
    }
  `]
})
export class BadgeComponent {
  @Input() label: string = '';
  @Input() alert: boolean = false;
  @Input() success: boolean = true;
}
