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
    companyName?: string | null;
    tenantId: string;
    firstName: string;
    lastName: string;
    email: string;
    password: string;
    seatsPurchased?: number | null;
    accountType?: 'HR' | 'EMPLOYEE';
    planCode?: 'STARTER' | 'PRO' | 'PREMIUM' | 'ENTERPRISE' | null;
  }): Observable<AuthSession> {
    const normalizedTenant = this.normalizeTenantId(request.tenantId);
    const normalizedEmail = request.email.trim().toLowerCase();
    return this.http.post<LoginApiResponse>(
      `${API_BASE_URL}/api/auth/signup`,
      {
        companyName: request.companyName?.trim() || null,
        tenantId: normalizedTenant,
        firstName: request.firstName.trim(),
        lastName: request.lastName.trim(),
        email: normalizedEmail,
        password: request.password,
        seatsPurchased: request.seatsPurchased ?? null,
        accountType: request.accountType ?? 'HR',
        planCode: request.planCode ?? null
      },
      { headers: { 'X-Tenant-Id': normalizedTenant } }
    ).pipe(
      map(response => this.toSession(response, normalizedEmail)),
      tap(session => this.persistSession(session))
    );
  }

  previewInvitation(token: string): Observable<{
    tenantId: string;
    companyName: string;
    email: string;
    firstName: string;
    lastName: string;
    expiresAt: string | null;
  }> {
    return this.http.get<{
      tenantId: string;
      companyName: string;
      email: string;
      firstName: string;
      lastName: string;
      expiresAt: string | null;
    }>(`${API_BASE_URL}/api/auth/invitations?token=${encodeURIComponent(token)}`);
  }

  acceptInvitation(token: string, password: string, email: string): Observable<AuthSession> {
    return this.http.post<LoginApiResponse>(
      `${API_BASE_URL}/api/auth/invitations/accept`,
      { token, password }
    ).pipe(
      map(response => this.toSession(response, email.trim().toLowerCase())),
      tap(session => this.persistSession(session))
    );
  }

  requestPasswordReset(email: string, tenantId: string): Observable<{ accepted: boolean; emailSent: boolean }> {
    const normalizedTenant = this.normalizeTenantId(tenantId);
    return this.http.post<{ accepted: boolean; emailSent: boolean }>(
      `${API_BASE_URL}/api/auth/password-reset/request`,
      { email: email.trim().toLowerCase() },
      normalizedTenant ? { headers: { 'X-Tenant-Id': normalizedTenant } } : {}
    );
  }

  confirmPasswordReset(token: string, password: string): Observable<AuthSession> {
    return this.http.post<LoginApiResponse>(
      `${API_BASE_URL}/api/auth/password-reset/confirm`,
      { token, password }
    ).pipe(
      map(response => this.toSession(response, '')),
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
      const session = JSON.parse(raw) as AuthSession;
      if (!this.isUsableAccessToken(session.accessToken)) {
        localStorage.removeItem(STORAGE_KEY);
        return null;
      }
      return session;
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

  private isUsableAccessToken(accessToken: string): boolean {
    if (!accessToken) {
      return false;
    }
    if (accessToken.startsWith('demo|')) {
      return true;
    }

    const [, payload] = accessToken.split('.');
    if (!payload) {
      return false;
    }

    try {
      const normalizedPayload = payload.replace(/-/g, '+').replace(/_/g, '/');
      const paddedPayload = normalizedPayload.padEnd(
        normalizedPayload.length + (4 - normalizedPayload.length % 4) % 4,
        '='
      );
      const claims = JSON.parse(atob(paddedPayload)) as { exp?: number };
      return typeof claims.exp === 'number' && claims.exp * 1000 > Date.now();
    } catch {
      return false;
    }
  }

  private normalizeTenantId(tenantId: string): string {
    return tenantId.trim().toLowerCase()
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/^-+|-+$/g, '')
      .replace(/-{2,}/g, '-');
  }
}
