var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
let CardComponent = class CardComponent {
    constructor() {
        this.variant = '';
    }
};
__decorate([
    Input()
], CardComponent.prototype, "variant", void 0);
CardComponent = __decorate([
    Component({
        selector: 'app-card',
        standalone: true,
        imports: [CommonModule],
        template: `
    <article [ngClass]="['card', variant]">
      <ng-content></ng-content>
    </article>
  `,
        styles: [`
    .card {
      background: var(--card);
      border: 1px solid var(--line);
      border-radius: 24px;
      backdrop-filter: blur(8px);
      box-shadow: 0 8px 24px rgba(13, 17, 23, 0.04);
      transition: all 0.3s ease;
      padding: 20px;
    }

    .card:hover {
      box-shadow: 0 12px 32px rgba(13, 17, 23, 0.08);
    }

    .card-subscription {
      padding: 24px;
      background: linear-gradient(135deg, rgba(0, 35, 149, 0.06) 0%, rgba(239, 51, 64, 0.06) 100%);
      border: 2px solid rgba(0, 35, 149, 0.2);
      box-shadow: inset 0 1px 2px rgba(0, 35, 149, 0.08);
    }

    .card-metric {
      padding: 20px;
      background: linear-gradient(135deg, rgba(255, 255, 255, 0.98), rgba(248, 249, 250, 1));
    }

    .card-pricing {
      padding: 18px;
      background: linear-gradient(135deg, rgba(255, 255, 255, 0.99), rgba(248, 249, 250, 0.98));
      border-top: 3px solid var(--line);
    }

    .card-pricing.featured {
      border-color: rgba(239, 51, 64, 0.4);
      background: linear-gradient(135deg, rgba(239, 51, 64, 0.08), rgba(0, 35, 149, 0.04));
      transform: translateY(-4px);
      box-shadow: 0 16px 32px rgba(239, 51, 64, 0.16);
      border-top: 3px solid var(--accent);
    }
  `]
    })
], CardComponent);
export { CardComponent };
