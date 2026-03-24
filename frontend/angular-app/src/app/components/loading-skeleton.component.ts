import { Component } from '@angular/core';

@Component({
  selector: 'app-loading-skeleton',
  standalone: true,
  template: `
    <section class="panel loading-panel">
      <div class="skeleton-content">
        <div class="skeleton skeleton-title"></div>
        <div class="skeleton skeleton-text"></div>
        <div class="skeleton skeleton-text short"></div>
      </div>
    </section>
  `,
  styles: [`
    .loading-panel {
      text-align: center;
      min-height: 400px;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .skeleton-content {
      width: 100%;
      padding: 40px;
    }

    .skeleton {
      background: linear-gradient(90deg, var(--line) 25%, rgba(0, 0, 0, 0.04) 50%, var(--line) 75%);
      background-size: 200% 100%;
      animation: loading 1.5s infinite;
      border-radius: 8px;
      margin: 16px 0;
    }

    .skeleton-title {
      height: 32px;
      width: 60%;
      margin: 0 auto 20px;
    }

    .skeleton-text {
      height: 16px;
      width: 80%;
      margin: 12px auto;
    }

    .skeleton-text.short {
      width: 50%;
    }

    @keyframes loading {
      0% {
        background-position: 200% 0;
      }
      100% {
        background-position: -200% 0;
      }
    }
  `]
})
export class LoadingSkeletonComponent {}
