var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
import { Injectable, signal } from '@angular/core';
let ToastService = class ToastService {
    constructor() {
        this.toasts = signal([]);
        this.toastList = this.toasts.asReadonly();
    }
    success(message, duration = 3000) {
        this.addToast(message, 'success', duration);
    }
    error(message, duration = 5000) {
        this.addToast(message, 'error', duration);
    }
    warning(message, duration = 4000) {
        this.addToast(message, 'warning', duration);
    }
    info(message, duration = 3000) {
        this.addToast(message, 'info', duration);
    }
    addToast(message, type, duration) {
        const id = Date.now().toString();
        const toast = { id, message, type, duration };
        this.toasts.update(toasts => [...toasts, toast]);
        if (duration && duration > 0) {
            setTimeout(() => this.removeToast(id), duration);
        }
    }
    removeToast(id) {
        this.toasts.update(toasts => toasts.filter(t => t.id !== id));
    }
    clear() {
        this.toasts.set([]);
    }
};
ToastService = __decorate([
    Injectable({ providedIn: 'root' })
], ToastService);
export { ToastService };
