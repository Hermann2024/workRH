import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SubscriptionResponse } from './workrh-api.service';
import { API_BASE_URL } from './config';

export interface SubscriptionChangeRequest {
  targetPlanCode: 'STARTER' | 'PRO' | 'PREMIUM' | 'ENTERPRISE';
  seatsPurchased: number;
  smsOptionEnabled: boolean;
  advancedAuditOptionEnabled: boolean;
  advancedExportOptionEnabled: boolean;
}

export interface SubscriptionCancelRequest {
  reason: string;
}

@Injectable({ providedIn: 'root' })
export class SubscriptionLifecycleService {
  private readonly http = inject(HttpClient);

  upgrade(request: SubscriptionChangeRequest): Observable<SubscriptionResponse> {
    return this.http.patch<SubscriptionResponse>(`${API_BASE_URL}/api/subscriptions/current/upgrade`, request);
  }

  downgrade(request: SubscriptionChangeRequest): Observable<SubscriptionResponse> {
    return this.http.patch<SubscriptionResponse>(`${API_BASE_URL}/api/subscriptions/current/downgrade`, request);
  }

  cancel(request: SubscriptionCancelRequest): Observable<SubscriptionResponse> {
    return this.http.patch<SubscriptionResponse>(`${API_BASE_URL}/api/subscriptions/current/cancel`, request);
  }

  reactivate(): Observable<SubscriptionResponse> {
    return this.http.patch<SubscriptionResponse>(`${API_BASE_URL}/api/subscriptions/current/reactivate`, {});
  }
}
