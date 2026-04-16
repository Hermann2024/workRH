import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../auth.service';
import {
  DEFAULT_LOGIN_EMAIL,
  DEFAULT_LOGIN_PASSWORD,
  DEFAULT_TENANT_ID,
  SHOW_DEMO_HINTS
} from '../config';

@Component({
  selector: 'app-login-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './login-page.component.html',
  styleUrl: './page-styles.css'
})
export class LoginPageComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly showDemoHints = SHOW_DEMO_HINTS;
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly loginForm = this.formBuilder.nonNullable.group({
    tenantId: [DEFAULT_TENANT_ID, [Validators.required]],
    email: [DEFAULT_LOGIN_EMAIL, [Validators.required, Validators.email]],
    password: [DEFAULT_LOGIN_PASSWORD, [Validators.required]]
  });

  fillCredentials(email: string): void {
    this.loginForm.patchValue({
      tenantId: DEFAULT_TENANT_ID || 'demo-lu',
      email,
      password: DEFAULT_LOGIN_PASSWORD || 'secret'
    });
  }

  submit(): void {
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
        this.errorMessage.set(
          error?.error?.message || 'Une erreur est survenue lors de la connexion. Veuillez réessayer.'
        );
      }
    });
  }
}
