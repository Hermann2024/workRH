import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-checkout-loading',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="checkout-loading-overlay" *ngIf="isVisible">
      <div class="checkout-loading-modal">
        <div class="loading-spinner">
          <div class="spinner-ring"></div>
          <div class="spinner-ring"></div>
          <div class="spinner-ring"></div>
        </div>

        <div class="loading-content">
          <h3>{{ title }}</h3>
          <p>{{ message }}</p>

          <div class="loading-steps" *ngIf="showSteps">
            <div class="step" [class.active]="currentStep >= 1">
              <div class="step-icon">🔐</div>
              <span>Sécurisation</span>
            </div>
            <div class="step" [class.active]="currentStep >= 2">
              <div class="step-icon">💳</div>
              <span>Paiement</span>
            </div>
            <div class="step" [class.active]="currentStep >= 3">
              <div class="step-icon">✅</div>
              <span>Activation</span>
            </div>
          </div>

          <div class="loading-progress">
            <div class="progress-bar">
              <div class="progress-fill" [style.width.%]="progress"></div>
            </div>
            <span class="progress-text">{{ progress }}%</span>
          </div>
        </div>

        <button class="cancel-btn" (click)="onCancel.emit()" *ngIf="showCancel">
          Annuler
        </button>
      </div>
    </div>
  `,
  styles: [`
    .checkout-loading-overlay {
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: rgba(13, 17, 23, 0.8);
      backdrop-filter: blur(4px);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 9999;
      animation: fadeIn 0.3s ease;
    }

    .checkout-loading-modal {
      background: var(--card);
      border-radius: 24px;
      padding: var(--space-2xl);
      box-shadow: 0 20px 60px rgba(13, 17, 23, 0.3);
      max-width: 400px;
      width: 90%;
      text-align: center;
      animation: slideUp 0.4s ease;
    }

    .loading-spinner {
      display: flex;
      justify-content: center;
      margin-bottom: var(--space-lg);
    }

    .spinner-ring {
      width: 40px;
      height: 40px;
      border: 3px solid rgba(239, 51, 64, 0.1);
      border-top: 3px solid var(--accent);
      border-radius: 50%;
      animation: spin 1s linear infinite;
      margin: 0 4px;
    }

    .spinner-ring:nth-child(2) { animation-delay: 0.2s; }
    .spinner-ring:nth-child(3) { animation-delay: 0.4s; }

    @keyframes spin {
      0% { transform: rotate(0deg); }
      100% { transform: rotate(360deg); }
    }

    .loading-content h3 {
      margin: 0 0 var(--space-md);
      color: var(--ink);
      font-size: 1.4rem;
      font-weight: 600;
    }

    .loading-content p {
      margin: 0 0 var(--space-xl);
      color: var(--ink-secondary);
      line-height: 1.6;
    }

    .loading-steps {
      display: flex;
      justify-content: center;
      gap: var(--space-lg);
      margin-bottom: var(--space-xl);
    }

    .step {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: var(--space-xs);
      opacity: 0.4;
      transition: opacity 0.3s ease;
    }

    .step.active {
      opacity: 1;
    }

    .step-icon {
      font-size: 1.5rem;
      margin-bottom: var(--space-xs);
    }

    .step span {
      font-size: 0.8rem;
      font-weight: 500;
      color: var(--ink-secondary);
    }

    .loading-progress {
      margin-top: var(--space-lg);
    }

    .progress-bar {
      width: 100%;
      height: 4px;
      background: rgba(13, 17, 23, 0.1);
      border-radius: 2px;
      overflow: hidden;
      margin-bottom: var(--space-sm);
    }

    .progress-fill {
      height: 100%;
      background: linear-gradient(90deg, var(--blue), var(--accent));
      border-radius: 2px;
      transition: width 0.3s ease;
    }

    .progress-text {
      font-size: 0.8rem;
      color: var(--ink-secondary);
      font-weight: 500;
    }

    .cancel-btn {
      margin-top: var(--space-lg);
      padding: var(--space-sm) var(--space-lg);
      border: 1px solid var(--line);
      background: transparent;
      border-radius: 12px;
      color: var(--ink-secondary);
      cursor: pointer;
      font: inherit;
      font-size: 0.9rem;
      transition: all 0.2s ease;
    }

    .cancel-btn:hover {
      background: rgba(218, 54, 51, 0.05);
      border-color: var(--error);
      color: var(--error);
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
export class CheckoutLoadingComponent {
  @Input() isVisible: boolean = false;
  @Input() title: string = 'Configuration du paiement';
  @Input() message: string = 'Création de votre session de paiement sécurisée...';
  @Input() showSteps: boolean = true;
  @Input() currentStep: number = 1;
  @Input() progress: number = 0;
  @Input() showCancel: boolean = true;

  @Output() onCancel = new EventEmitter<void>();
}
