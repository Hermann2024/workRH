var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
import { CommonModule } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './auth.service';
import { ToastContainerComponent } from './components/toast-container.component';
let AppComponent = class AppComponent {
    constructor() {
        this.authService = inject(AuthService);
        this.router = inject(Router);
        this.session = this.authService.session;
        this.isAuthenticated = this.authService.isAuthenticated;
        this.shellSummary = computed(() => {
            if (this.isAuthenticated()) {
                const currentSession = this.session();
                const primaryRole = currentSession?.roles[0] ?? 'EMPLOYEE';
                return {
                    badge: 'Workspace',
                    title: 'Pilotage RH frontalier',
                    description: `Tenant ${currentSession?.tenantId} connecte avec le role ${primaryRole}.`,
                    highlights: ['Dashboard unifie', 'Conformite Luxembourg', 'Billing SaaS'],
                    statusLabel: currentSession?.email ?? 'Session active'
                };
            }
            return {
                badge: 'WorkRH Platform',
                title: 'RH, teletravail et abonnement dans le meme cockpit',
                description: 'Une interface unique pour les operations RH, la conformite frontaliere et la monetisation SaaS.',
                highlights: ['Login demo', 'Pricing clair', 'Modules securises'],
                statusLabel: 'Session non connectee'
            };
        });
        this.navItems = computed(() => {
            const items = [
                { label: 'Offres', route: '/pricing' }
            ];
            if (this.isAuthenticated()) {
                if (this.authService.hasRole('EMPLOYEE')) {
                    items.unshift({ label: 'Mon espace', route: '/employee' });
                }
                if (this.authService.hasRole('HR') || this.authService.hasRole('ADMIN')) {
                    items.unshift({ label: 'Dashboard', route: '/dashboard' });
                    items.push({ label: 'Billing', route: '/billing' });
                    items.push({ label: 'Policies', route: '/policies' });
                }
            }
            else {
                items.unshift({ label: 'Login', route: '/login' });
            }
            return items;
        });
    }
    logout() {
        this.authService.logout();
        this.router.navigateByUrl('/login');
    }
};
AppComponent = __decorate([
    Component({
        selector: 'app-root',
        standalone: true,
        imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive, ToastContainerComponent],
        templateUrl: './app.component.html',
        styleUrl: './app.component.css'
    })
], AppComponent);
export { AppComponent };
