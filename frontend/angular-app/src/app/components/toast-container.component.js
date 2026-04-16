var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ToastService } from '../services/toast.service';
let ToastContainerComponent = class ToastContainerComponent {
    constructor() {
        this.toastService = inject(ToastService);
    }
};
ToastContainerComponent = __decorate([
    Component({
        selector: 'app-toast-container',
        standalone: true,
        imports: [CommonModule],
        template: `
    <div class="toast-container">
      <div *ngFor="let toast of toastService.toastList()"
           [ngClass]="['toast', 'toast-' + toast.type]"
           [@slideIn]>
        {{ toast.message }}
        <button (click)="toastService.removeToast(toast.id)" class="toast-close">×</button>
      </div>
    </div>
  `,
        styles: [`
    .toast-container {
      position: fixed;
      top: 20px;
      right: 20px;
      z-index: 9999;
      display: flex;
      flex-direction: column;
      gap: 12px;
    }

    .toast {
      padding: 14px 18px;
      border-radius: 12px;
      box-shadow: 0 8px 24px rgba(13, 17, 23, 0.15);
      display: flex;
      align-items: center;
      justify-content: space-between;
      min-width: 300px;
      animation: slideInRight 0.3s ease;
      font-weight: 500;
    }

    .toast-success {
      background: rgba(31, 136, 61, 0.1);
      color: var(--success);
      border-left: 4px solid var(--success);
    }

    .toast-error {
      background: rgba(218, 54, 51, 0.1);
      color: var(--error);
      border-left: 4px solid var(--error);
    }

    .toast-warning {
      background: rgba(191, 135, 0, 0.1);
      color: var(--warning);
      border-left: 4px solid var(--warning);
    }

    .toast-info {
      background: rgba(0, 35, 149, 0.1);
      color: var(--blue);
      border-left: 4px solid var(--blue);
    }

    .toast-close {
      background: none;
      border: none;
      font-size: 20px;
      cursor: pointer;
      padding: 0 8px;
      margin-left: 8px;
      opacity: 0.7;
      transition: opacity 0.2s;
    }

    .toast-close:hover {
      opacity: 1;
    }

    @keyframes slideInRight {
      from {
        opacity: 0;
        transform: translateX(100%);
      }
      to {
        opacity: 1;
        transform: translateX(0);
      }
    }

    @media (max-width: 640px) {
      .toast-container {
        left: 12px;
        right: 12px;
      }

      .toast {
        min-width: auto;
        width: 100%;
      }
    }
  `]
    })
], ToastContainerComponent);
export { ToastContainerComponent };
