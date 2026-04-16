import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './config';
import type { SubscriptionResponse } from './workrh-api.service';

export interface StripeCheckoutRequest {
  planCode: 'STARTER' | 'PRO' | 'PREMIUM' | 'ENTERPRISE';
  seatsPurchased: number;
  paymentMethodTypes: string[];
  smsOptionEnabled: boolean;
  advancedAuditOptionEnabled: boolean;
  advancedExportOptionEnabled: boolean;
  customerEmail: string;
  successUrl: string;
  cancelUrl: string;
}

export interface StripeCheckoutResponse {
  sessionId: string;
  checkoutUrl: string;
}

@Injectable({ providedIn: 'root' })
export class SubscriptionCheckoutService {
  private readonly http = inject(HttpClient);

  createCheckout(request: StripeCheckoutRequest): Observable<StripeCheckoutResponse> {
    return this.http.post<StripeCheckoutResponse>(`${API_BASE_URL}/api/subscriptions/checkout/stripe`, request);
  }

  confirmCheckout(sessionId: string): Observable<SubscriptionResponse> {
    return this.http.post<SubscriptionResponse>(
      `${API_BASE_URL}/api/subscriptions/checkout/stripe/confirm?sessionId=${encodeURIComponent(sessionId)}`,
      {}
    );
  }
}
