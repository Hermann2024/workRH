var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
let AlertComponent = class AlertComponent {
    constructor() {
        this.message = '';
        this.variant = 'alert-info';
        this.visible = true;
    }
};
__decorate([
    Input()
], AlertComponent.prototype, "message", void 0);
__decorate([
    Input()
], AlertComponent.prototype, "variant", void 0);
__decorate([
    Input()
], AlertComponent.prototype, "visible", void 0);
AlertComponent = __decorate([
    Component({
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
], AlertComponent);
export { AlertComponent };
