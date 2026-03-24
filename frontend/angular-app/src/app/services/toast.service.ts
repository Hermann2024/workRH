import { Injectable, signal } from '@angular/core';

export interface Toast {
  id: string;
  message: string;
  type: 'success' | 'error' | 'warning' | 'info';
  duration?: number;
}

@Injectable({ providedIn: 'root' })
export class ToastService {
  private toasts = signal<Toast[]>([]);
  readonly toastList = this.toasts.asReadonly();

  success(message: string, duration = 3000): void {
    this.addToast(message, 'success', duration);
  }

  error(message: string, duration = 5000): void {
    this.addToast(message, 'error', duration);
  }

  warning(message: string, duration = 4000): void {
    this.addToast(message, 'warning', duration);
  }

  info(message: string, duration = 3000): void {
    this.addToast(message, 'info', duration);
  }

  private addToast(message: string, type: Toast['type'], duration?: number): void {
    const id = Date.now().toString();
    const toast: Toast = { id, message, type, duration };
    
    this.toasts.update(toasts => [...toasts, toast]);

    if (duration && duration > 0) {
      setTimeout(() => this.removeToast(id), duration);
    }
  }

  removeToast(id: string): void {
    this.toasts.update(toasts => toasts.filter(t => t.id !== id));
  }

  clear(): void {
    this.toasts.set([]);
  }
}
