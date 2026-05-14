import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../auth.service';
import { TranslatePipe } from '../i18n/translate.pipe';
import { DEFAULT_TENANT_ID } from '../config';

@Component({
  selector: 'app-forgot-password-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, TranslatePipe],
  templateUrl: './forgot-password-page.component.html',
  styleUrl: './page-styles.css'
})
export class ForgotPasswordPageComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly authService = inject(AuthService);

  readonly submitting = signal(false);
  readonly submitted = signal(false);
  readonly emailSent = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly resetForm = this.formBuilder.nonNullable.group({
    tenantId: [DEFAULT_TENANT_ID, [Validators.required]],
    email: ['', [Validators.required, Validators.email]]
  });

  submit(): void {
    if (this.resetForm.invalid) {
      this.resetForm.markAllAsTouched();
      return;
    }

    const raw = this.resetForm.getRawValue();
    this.submitting.set(true);
    this.errorMessage.set(null);
    this.authService.requestPasswordReset(raw.email, raw.tenantId).subscribe({
      next: (response) => {
        this.submitting.set(false);
        this.submitted.set(true);
        this.emailSent.set(response.emailSent);
      },
      error: (error) => {
        this.submitting.set(false);
        this.errorMessage.set(error?.error?.message || 'Impossible de traiter la demande.');
      }
    });
  }
}
