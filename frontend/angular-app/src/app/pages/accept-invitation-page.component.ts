import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../auth.service';
import { TranslatePipe } from '../i18n/translate.pipe';
import { ToastService } from '../services/toast.service';

@Component({
  selector: 'app-accept-invitation-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, TranslatePipe],
  templateUrl: './accept-invitation-page.component.html',
  styleUrl: './page-styles.css'
})
export class AcceptInvitationPageComponent {
  private readonly authService = inject(AuthService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly toastService = inject(ToastService);

  readonly loading = signal(true);
  readonly accepting = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly invitation = signal<{
    tenantId: string;
    companyName: string;
    email: string;
    firstName: string;
    lastName: string;
    expiresAt: string | null;
  } | null>(null);

  readonly passwordForm = this.formBuilder.nonNullable.group({
    password: ['', [Validators.required, Validators.minLength(8)]],
    confirmPassword: ['', [Validators.required]]
  });

  private readonly token = this.route.snapshot.queryParamMap.get('token') ?? '';

  constructor() {
    this.loadInvitation();
  }

  acceptInvitation(): void {
    const preview = this.invitation();
    if (!preview) {
      return;
    }
    if (this.passwordForm.invalid) {
      this.passwordForm.markAllAsTouched();
      this.toastService.error('Choisissez un mot de passe de 8 caracteres minimum.');
      return;
    }

    const raw = this.passwordForm.getRawValue();
    if (raw.password !== raw.confirmPassword) {
      this.toastService.error('Les mots de passe ne correspondent pas.');
      return;
    }

    this.accepting.set(true);
    this.authService.acceptInvitation(this.token, raw.password, preview.email).subscribe({
      next: () => {
        this.accepting.set(false);
        this.toastService.success('Compte active.');
        void this.router.navigate(['/employee']);
      },
      error: (error) => {
        this.accepting.set(false);
        this.toastService.error(this.readBackendMessage(error, 'Impossible d activer cette invitation.'));
      }
    });
  }

  private loadInvitation(): void {
    if (!this.token) {
      this.errorMessage.set('Lien d invitation manquant.');
      this.loading.set(false);
      return;
    }

    this.authService.previewInvitation(this.token).subscribe({
      next: (invitation) => {
        this.invitation.set(invitation);
        this.errorMessage.set(null);
        this.loading.set(false);
      },
      error: (error) => {
        this.errorMessage.set(this.readBackendMessage(error, 'Cette invitation est invalide ou expiree.'));
        this.loading.set(false);
      }
    });
  }

  private readBackendMessage(error: unknown, fallback: string): string {
    const backendMessage = (error as { error?: { message?: string } })?.error?.message;
    return typeof backendMessage === 'string' && backendMessage.trim()
      ? backendMessage
      : fallback;
  }
}
