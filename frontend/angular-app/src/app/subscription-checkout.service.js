var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { API_BASE_URL } from './config';
let SubscriptionCheckoutService = class SubscriptionCheckoutService {
    constructor() {
        this.http = inject(HttpClient);
    }
    createCheckout(request) {
        return this.http.post(`${API_BASE_URL}/api/subscriptions/checkout/stripe`, request);
    }
};
SubscriptionCheckoutService = __decorate([
    Injectable({ providedIn: 'root' })
], SubscriptionCheckoutService);
export { SubscriptionCheckoutService };
