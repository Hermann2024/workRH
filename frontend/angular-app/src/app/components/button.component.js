var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
let ButtonComponent = class ButtonComponent {
    constructor() {
        this.label = '';
        this.variant = 'btn-primary';
        this.disabled = false;
        this.onClick = new EventEmitter();
    }
};
__decorate([
    Input()
], ButtonComponent.prototype, "label", void 0);
__decorate([
    Input()
], ButtonComponent.prototype, "variant", void 0);
__decorate([
    Input()
], ButtonComponent.prototype, "disabled", void 0);
__decorate([
    Output()
], ButtonComponent.prototype, "onClick", void 0);
ButtonComponent = __decorate([
    Component({
        selector: 'app-button',
        standalone: true,
        imports: [CommonModule],
        template: `
    <button 
      [ngClass]="['btn', variant, { 'btn-disabled': disabled }]"
      [disabled]="disabled"
      (click)="onClick.emit()">
      {{ label }}
    </button>
  `,
        styles: [`
    .btn {
      padding: 12px 18px;
      border: 0;
      border-radius: 16px;
      font: inherit;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.2s ease;
    }

    .btn-primary {
      background: linear-gradient(135deg, var(--blue), var(--accent));
      color: #ffffff;
      box-shadow: 0 4px 12px rgba(0, 35, 149, 0.2);
    }

    .btn-primary:hover:not(:disabled) {
      background: linear-gradient(135deg, var(--blue-dark), var(--accent-dark));
      transform: translateY(-2px);
      box-shadow: 0 8px 20px rgba(0, 35, 149, 0.3);
    }

    .btn-secondary {
      background: transparent;
      border: 1.5px solid var(--blue);
      color: var(--blue);
    }

    .btn-secondary:hover:not(:disabled) {
      background: linear-gradient(135deg, rgba(0, 35, 149, 0.08), rgba(77, 93, 184, 0.04));
      border-color: var(--blue-dark);
      box-shadow: 0 4px 12px rgba(0, 35, 149, 0.12);
    }

    .btn-danger {
      background: linear-gradient(135deg, var(--error), #ff6b35);
      color: #ffffff;
      box-shadow: 0 4px 12px rgba(218, 54, 51, 0.2);
    }

    .btn-danger:hover:not(:disabled) {
      background: linear-gradient(135deg, #c01d1a, #ff5221);
      box-shadow: 0 8px 20px rgba(218, 54, 51, 0.3);
    }

    .btn-disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }
  `]
    })
], ButtonComponent);
export { ButtonComponent };
