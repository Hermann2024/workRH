import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-section-header',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="section-head">
      <div>
        <p class="section-tag">{{ tag }}</p>
        <h2>{{ title }}</h2>
      </div>
      <p class="hint" *ngIf="hint">{{ hint }}</p>
    </div>
  `,
  styles: [`
    .section-head {
      display: flex;
      justify-content: space-between;
      gap: 20px;
      align-items: end;
      margin-bottom: 20px;
    }

    .section-tag {
      display: inline-flex;
      align-items: center;
      padding: 6px 10px;
      border-radius: 999px;
      font-size: 12px;
      text-transform: uppercase;
      letter-spacing: 0.14em;
      background: linear-gradient(135deg, rgba(0, 35, 149, 0.12), rgba(77, 93, 184, 0.08));
      color: var(--blue-dark);
      font-weight: 700;
      border: 1px solid rgba(0, 35, 149, 0.2);
      margin: 0;
    }

    h2 {
      margin: 8px 0 0;
      font-size: 1.8rem;
    }

    .hint {
      color: rgba(13, 17, 23, 0.72);
      margin: 0;
    }

    @media (max-width: 900px) {
      .section-head {
        display: block;
      }
    }
  `]
})
export class SectionHeaderComponent {
  @Input() tag: string = '';
  @Input() title: string = '';
  @Input() hint?: string;
}
