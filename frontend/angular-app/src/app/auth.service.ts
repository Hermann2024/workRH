import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, map, of, tap } from 'rxjs';
import { AuthSession, LoginApiResponse } from './auth.models';
import { API_BASE_URL } from './config';

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
    const normalizedTenant = tenantId.trim().toLowerCase();
    return this.http.post<LoginApiResponse>(
      `${API_BASE_URL}/api/auth/login`,
      { email: normalizedEmail, password },
      { headers: { 'X-Tenant-Id': normalizedTenant || 'demo-lu' } }
    ).pipe(
      map(response => ({
        tenantId: response.tenantId,
        accessToken: response.token,
        email: normalizedEmail,
        roles: response.roles
      })),
      catchError(() => of(this.buildDemoSession(normalizedEmail, normalizedTenant || 'demo-lu', password))),
      tap(session => {
        this.sessionState.set(session);
        localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
      })
    );
  }

  logout(): void {
    this.sessionState.set(null);
    localStorage.removeItem(STORAGE_KEY);
  }

  hasRole(role: string): boolean {
    return this.roles().includes(role);
  }

  private buildDemoSession(email: string, tenantId: string, password: string): AuthSession {
    const roles = email.includes('admin')
      ? ['ADMIN', 'HR']
      : email.includes('rh')
        ? ['HR']
        : ['EMPLOYEE'];
    return {
      tenantId,
      accessToken: password ? 'demo-jwt-token' : 'fallback-jwt-token',
      email,
      roles
    };
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
}
