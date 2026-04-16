var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
let ModalComponent = class ModalComponent {
    constructor() {
        this.isOpen = false;
        this.title = '';
        this.showFooter = true;
        this.confirmLabel = 'Confirmer';
        this.cancelLabel = 'Annuler';
        this.closeOnOverlay = true;
        this.onClose = new EventEmitter();
        this.onConfirmClick = new EventEmitter();
    }
    closeModal() {
        this.onClose.emit();
    }
    onOverlayClick() {
        if (this.closeOnOverlay) {
            this.closeModal();
        }
    }
    onConfirm() {
        this.onConfirmClick.emit();
    }
};
__decorate([
    Input()
], ModalComponent.prototype, "isOpen", void 0);
__decorate([
    Input()
], ModalComponent.prototype, "title", void 0);
__decorate([
    Input()
], ModalComponent.prototype, "showFooter", void 0);
__decorate([
    Input()
], ModalComponent.prototype, "confirmLabel", void 0);
__decorate([
    Input()
], ModalComponent.prototype, "cancelLabel", void 0);
__decorate([
    Input()
], ModalComponent.prototype, "closeOnOverlay", void 0);
__decorate([
    Output()
], ModalComponent.prototype, "onClose", void 0);
__decorate([
    Output()
], ModalComponent.prototype, "onConfirmClick", void 0);
ModalComponent = __decorate([
    Component({
        selector: 'app-modal',
        standalone: true,
        imports: [CommonModule],
        template: `
    <div class="modal-overlay" *ngIf="isOpen" (click)="onOverlayClick()">
      <div class="modal-dialog" (click)="$event.stopPropagation()">
        <div class="modal-header">
          <h2>{{ title }}</h2>
          <button class="modal-close" (click)="closeModal()">&times;</button>
        </div>
        <div class="modal-body">
          <ng-content></ng-content>
        </div>
        <div class="modal-footer" *ngIf="showFooter">
          <button class="btn btn-secondary" (click)="closeModal()">
            {{ cancelLabel }}
          </button>
          <button class="btn btn-primary" (click)="onConfirm()">
            {{ confirmLabel }}
          </button>
        </div>
      </div>
    </div>
  `,
        styles: [`
    .modal-overlay {
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: rgba(13, 17, 23, 0.5);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 1000;
      animation: fadeIn 0.3s ease;
    }

    .modal-dialog {
      background: var(--card);
      border-radius: 24px;
      box-shadow: 0 20px 60px rgba(13, 17, 23, 0.3);
      max-width: 500px;
      width: 90%;
      max-height: 90vh;
      overflow-y: auto;
      animation: slideUp 0.3s ease;
    }

    .modal-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 24px;
      border-bottom: 1px solid var(--line);
    }

    .modal-header h2 {
      margin: 0;
      font-size: 1.5rem;
    }

    .modal-close {
      background: none;
      border: none;
      font-size: 28px;
      cursor: pointer;
      color: var(--ink);
      opacity: 0.6;
      transition: opacity 0.2s;
    }

    .modal-close:hover {
      opacity: 1;
    }

    .modal-body {
      padding: 24px;
    }

    .modal-footer {
      display: flex;
      gap: 12px;
      padding: 24px;
      border-top: 1px solid var(--line);
      justify-content: flex-end;
    }

    .btn {
      padding: 10px 16px;
      border: 0;
      border-radius: 12px;
      font: inherit;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.2s ease;
    }

    .btn-primary {
      background: linear-gradient(135deg, var(--blue), var(--accent));
      color: #ffffff;
    }

    .btn-primary:hover {
      background: linear-gradient(135deg, var(--blue-dark), var(--accent-dark));
    }

    .btn-secondary {
      background: transparent;
      border: 1.5px solid var(--line);
      color: var(--ink);
    }

    .btn-secondary:hover {
      background: rgba(0, 35, 149, 0.04);
      border-color: var(--blue);
    }

    @keyframes fadeIn {
      from { opacity: 0; }
      to { opacity: 1; }
    }

    @keyframes slideUp {
      from {
        opacity: 0;
        transform: translateY(20px);
      }
      to {
        opacity: 1;
        transform: translateY(0);
      }
    }
  `]
    })
], ModalComponent);
export { ModalComponent };
