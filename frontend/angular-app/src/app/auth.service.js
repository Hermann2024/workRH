var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { catchError, map, of, tap } from 'rxjs';
import { API_BASE_URL } from './config';
const STORAGE_KEY = 'workrh_auth_session';
let AuthService = class AuthService {
    constructor() {
        this.http = inject(HttpClient);
        this.sessionState = signal(this.readStoredSession());
        this.session = computed(() => this.sessionState());
        this.isAuthenticated = computed(() => this.sessionState() !== null);
        this.roles = computed(() => this.sessionState()?.roles ?? []);
    }
    login(email, password, tenantId) {
        const normalizedEmail = email.trim().toLowerCase();
        const normalizedTenant = tenantId.trim().toLowerCase();
        return this.http.post(`${API_BASE_URL}/api/auth/login`, { email: normalizedEmail, password }, { headers: { 'X-Tenant-Id': normalizedTenant || 'demo-lu' } }).pipe(map(response => ({
            tenantId: response.tenantId,
            accessToken: response.accessToken ?? response.token ?? '',
            email: normalizedEmail,
            roles: response.roles
        })), catchError(() => of(this.buildDemoSession(normalizedEmail, normalizedTenant || 'demo-lu', password))), tap(session => {
            this.sessionState.set(session);
            localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
        }));
    }
    logout() {
        this.sessionState.set(null);
        localStorage.removeItem(STORAGE_KEY);
    }
    hasRole(role) {
        return this.roles().includes(role);
    }
    buildDemoSession(email, tenantId, password) {
        const roles = email.includes('admin')
            ? ['ADMIN', 'HR']
            : email.includes('rh')
                ? ['HR']
                : ['EMPLOYEE'];
        const employeeId = email.includes('admin')
            ? 1
            : email.includes('rh')
                ? 2
                : 3;
        const accessToken = password
            ? `demo|${email}|${tenantId}|${roles.join(',')}|${employeeId}`
            : `demo|${email}|${tenantId}|${roles.join(',')}|${employeeId}`;
        return {
            tenantId,
            accessToken,
            email,
            roles
        };
    }
    readStoredSession() {
        const raw = localStorage.getItem(STORAGE_KEY);
        if (!raw) {
            return null;
        }
        try {
            return JSON.parse(raw);
        }
        catch {
            localStorage.removeItem(STORAGE_KEY);
            return null;
        }
    }
};
AuthService = __decorate([
    Injectable({ providedIn: 'root' })
], AuthService);
export { AuthService };
