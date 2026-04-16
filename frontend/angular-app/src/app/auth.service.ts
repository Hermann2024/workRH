import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map, tap } from 'rxjs';
import { AuthSession, LoginApiResponse } from './auth.models';
import { API_BASE_URL, DEFAULT_TENANT_ID } from './config';

const STORAGE_KEY = 'workrh_auth_session';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly sessionState = signal<AuthSession | null>(this.readStoredSession());

  readonly session = computed(() => this.sessionState());
  readonly isAuthenticated = computed(() => this.sessionState() !== null);
  readonly roles = computed(() => this.sessionState()?.roles ?? []);

  login(email: string, password: string, tenantId: string): Observable<AuthSession> {
    const normalizedEmail = email.trim().toLowerCase();
    const normalizedTenant = this.normalizeTenantId(tenantId);
    const tenantHeader = normalizedTenant || DEFAULT_TENANT_ID;
    return this.http.post<LoginApiResponse>(
      `${API_BASE_URL}/api/auth/login`,
      { email: normalizedEmail, password },
      tenantHeader
        ? { headers: { 'X-Tenant-Id': tenantHeader } }
        : {}
    ).pipe(
      map(response => this.toSession(response, normalizedEmail)),
      tap(session => this.persistSession(session))
    );
  }

  signup(request: {
    tenantId: string;
    firstName: string;
    lastName: string;
    email: string;
    password: string;
    seatsPurchased: number;
    planCode?: 'STARTER' | 'PRO' | 'PREMIUM' | 'ENTERPRISE' | null;
  }): Observable<AuthSession> {
    const normalizedTenant = this.normalizeTenantId(request.tenantId);
    const normalizedEmail = request.email.trim().toLowerCase();
    return this.http.post<LoginApiResponse>(
      `${API_BASE_URL}/api/auth/signup`,
      {
        firstName: request.firstName.trim(),
        lastName: request.lastName.trim(),
        email: normalizedEmail,
        password: request.password,
        seatsPurchased: request.seatsPurchased,
        planCode: request.planCode ?? null
      },
      { headers: { 'X-Tenant-Id': normalizedTenant } }
    ).pipe(
      map(response => this.toSession(response, normalizedEmail)),
      tap(session => this.persistSession(session))
    );
  }

  logout(): void {
    this.sessionState.set(null);
    localStorage.removeItem(STORAGE_KEY);
  }

  hasRole(role: string): boolean {
    return this.roles().includes(role);
  }

  private readStoredSession(): AuthSession | null {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) {
      return null;
    }
    try {
      return JSON.parse(raw) as AuthSession;
    } catch {
      localStorage.removeItem(STORAGE_KEY);
      return null;
    }
  }

  private toSession(response: LoginApiResponse, email: string): AuthSession {
    return {
      tenantId: response.tenantId,
      accessToken: response.accessToken ?? response.token ?? '',
      email,
      roles: response.roles
    };
  }

  private persistSession(session: AuthSession): void {
    this.sessionState.set(session);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
  }

  private normalizeTenantId(tenantId: string): string {
    return tenantId.trim().toLowerCase()
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/^-+|-+$/g, '')
      .replace(/-{2,}/g, '-');
  }
}
