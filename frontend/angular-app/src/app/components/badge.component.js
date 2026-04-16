var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
let BadgeComponent = class BadgeComponent {
    constructor() {
        this.label = '';
        this.alert = false;
        this.success = true;
    }
};
__decorate([
    Input()
], BadgeComponent.prototype, "label", void 0);
__decorate([
    Input()
], BadgeComponent.prototype, "alert", void 0);
__decorate([
    Input()
], BadgeComponent.prototype, "success", void 0);
BadgeComponent = __decorate([
    Component({
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
], BadgeComponent);
export { BadgeComponent };
