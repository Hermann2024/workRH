import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../auth.service';
import { TranslatePipe } from '../i18n/translate.pipe';
import { ToastService } from '../services/toast.service';

@Component({
  selector: 'app-reset-password-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, TranslatePipe],
  templateUrl: './reset-password-page.component.html',
  styleUrl: './page-styles.css'
})
export class ResetPasswordPageComponent {
  private readonly authService = inject(AuthService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly toastService = inject(ToastService);

  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly passwordForm = this.formBuilder.nonNullable.group({
    password: ['', [Validators.required, Validators.minLength(8)]],
    confirmPassword: ['', [Validators.required]]
  });

  private readonly token = this.route.snapshot.queryParamMap.get('token') ?? '';

  submit(): void {
    if (!this.token) {
      this.errorMessage.set('Lien de reinitialisation manquant.');
      return;
    }
    if (this.passwordForm.invalid) {
      this.passwordForm.markAllAsTouched();
      return;
    }

    const raw = this.passwordForm.getRawValue();
    if (raw.password !== raw.confirmPassword) {
      this.errorMessage.set('Les mots de passe ne correspondent pas.');
      return;
    }

    this.submitting.set(true);
    this.errorMessage.set(null);
    this.authService.confirmPasswordReset(this.token, raw.password).subscribe({
      next: () => {
        this.submitting.set(false);
        this.toastService.success('Mot de passe mis a jour.');
        const roles = this.authService.roles();
        const employeeOnly = roles.includes('EMPLOYEE') && !roles.includes('HR') && !roles.includes('ADMIN');
        void this.router.navigateByUrl(employeeOnly ? '/employee' : '/dashboard');
      },
      error: (error) => {
        this.submitting.set(false);
        this.errorMessage.set(error?.error?.message || 'Impossible de reinitialiser ce mot de passe.');
      }
    });
  }
}
