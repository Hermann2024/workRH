import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-alert',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div [ngClass]="['alert', variant]" *ngIf="visible">
      <span>{{ message }}</span>
    </div>
  `,
  styles: [`
    .alert {
      margin-bottom: 18px;
      padding: 14px 16px;
      border-radius: 16px;
      border-left: 4px solid;
      font-weight: 500;
      animation: slideDown 0.3s ease;
    }

    .alert-success {
      background: rgba(31, 136, 61, 0.08);
      color: var(--success);
      border-left-color: var(--success);
    }

    .alert-error {
      background: rgba(218, 54, 51, 0.08);
      color: var(--error);
      border-left-color: var(--error);
    }

    .alert-warning {
      background: rgba(191, 135, 0, 0.08);
      color: var(--warning);
      border-left-color: var(--warning);
    }

    .alert-info {
      background: rgba(0, 35, 149, 0.08);
      color: var(--blue);
      border-left-color: var(--blue);
    }

    @keyframes slideDown {
      from {
        opacity: 0;
        transform: translateY(-10px);
      }
      to {
        opacity: 1;
        transform: translateY(0);
      }
    }
  `]
})
export class AlertComponent {
  @Input() message: string = '';
  @Input() variant: 'alert-success' | 'alert-error' | 'alert-warning' | 'alert-info' = 'alert-info';
  @Input() visible: boolean = true;
}
