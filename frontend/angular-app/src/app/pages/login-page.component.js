var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../auth.service';
let LoginPageComponent = class LoginPageComponent {
    constructor() {
        this.formBuilder = inject(FormBuilder);
        this.authService = inject(AuthService);
        this.route = inject(ActivatedRoute);
        this.router = inject(Router);
        this.submitting = signal(false);
        this.errorMessage = signal(null);
        this.loginForm = this.formBuilder.nonNullable.group({
            tenantId: ['demo-lu', [Validators.required]],
            email: ['rh@company.com', [Validators.required, Validators.email]],
            password: ['secret', [Validators.required]]
        });
    }
    submit() {
        if (this.loginForm.invalid) {
            this.loginForm.markAllAsTouched();
            return;
        }
        this.errorMessage.set(null);
        this.submitting.set(true);
        const { email, password, tenantId } = this.loginForm.getRawValue();
        this.authService.login(email, password, tenantId).subscribe({
            next: () => {
                this.submitting.set(false);
                const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
                if (returnUrl?.startsWith('/')) {
                    this.router.navigateByUrl(returnUrl);
                    return;
                }
                const roles = this.authService.roles();
                const employeeOnly = roles.includes('EMPLOYEE') && !roles.includes('HR') && !roles.includes('ADMIN');
                this.router.navigateByUrl(employeeOnly ? '/employee' : '/dashboard');
            },
            error: (error) => {
                this.submitting.set(false);
                this.errorMessage.set(error?.error?.message || 'Une erreur est survenue lors de la connexion. Veuillez reessayer.');
            }
        });
    }
};
LoginPageComponent = __decorate([
    Component({
        selector: 'app-login-page',
        standalone: true,
        imports: [CommonModule, ReactiveFormsModule],
        templateUrl: './login-page.component.html',
        styleUrl: './page-styles.css'
    })
], LoginPageComponent);
export { LoginPageComponent };
