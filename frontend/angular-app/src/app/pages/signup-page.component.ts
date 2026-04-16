import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../auth.service';

type SignupPlanCode = 'STARTER' | 'PRO' | 'PREMIUM' | 'ENTERPRISE';
type TrialPlanCode = 'STARTER' | 'PRO' | 'PREMIUM';
type SignupFieldName = 'tenantId' | 'firstName' | 'lastName' | 'email' | 'password' | 'seatsPurchased';

const PLAN_SEAT_RULES: Record<TrialPlanCode, { min: number; max: number | null; label: string }> = {
  STARTER: { min: 1, max: 10, label: 'Starter' },
  PRO: { min: 10, max: 50, label: 'Pro' },
  PREMIUM: { min: 50, max: null, label: 'Premium' }
};

@Component({
  selector: 'app-signup-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './signup-page.component.html',
  styleUrl: './page-styles.css'
})
export class SignupPageComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly requestedPlan = computed(() => this.parsePlanCode(this.route.snapshot.queryParamMap.get('plan')));
  readonly trialPlan = computed<TrialPlanCode>(() => this.resolveTrialPlan(this.requestedPlan()));
  readonly seatRules = computed(() => PLAN_SEAT_RULES[this.trialPlan()]);
  readonly signupForm = this.formBuilder.nonNullable.group({
    tenantId: ['', [Validators.required, Validators.minLength(3)]],
    firstName: ['', [Validators.required, Validators.minLength(2)]],
    lastName: ['', [Validators.required, Validators.minLength(2)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
    seatsPurchased: [5, [Validators.required, Validators.min(1), Validators.max(500)]]
  });

  constructor() {
    this.applySeatRules();
  }

  submit(): void {
    if (this.submitting()) {
      return;
    }

    if (this.signupForm.invalid) {
      this.signupForm.markAllAsTouched();
      this.errorMessage.set('Completez les champs obligatoires avant de creer le compte.');
      return;
    }

    this.errorMessage.set(null);
    this.submitting.set(true);
    this.authService.signup({
      ...this.signupForm.getRawValue(),
      planCode: this.requestedPlan()
    }).subscribe({
      next: () => {
        this.submitting.set(false);
        const requestedPlan = this.requestedPlan();
        if (requestedPlan) {
          this.router.navigate(['/billing'], {
            queryParams: {
              plan: requestedPlan
            }
          });
          return;
        }
        this.router.navigateByUrl('/billing');
      },
      error: (error: HttpErrorResponse) => {
        this.submitting.set(false);
        this.errorMessage.set(this.readSignupError(error));
      }
    });
  }

  hasFieldError(fieldName: SignupFieldName): boolean {
    const field = this.signupForm.controls[fieldName];
    return field.invalid && (field.touched || field.dirty);
  }

  fieldError(fieldName: SignupFieldName): string | null {
    const field = this.signupForm.controls[fieldName];
    if (!field.errors || (!field.touched && !field.dirty)) {
      return null;
    }

    if (field.errors['required']) {
      return 'Ce champ est obligatoire.';
    }
    if (field.errors['email']) {
      return 'Entrez une adresse email valide.';
    }
    if (field.errors['minlength']) {
      const requiredLength = field.errors['minlength'].requiredLength;
      return `Minimum ${requiredLength} caracteres.`;
    }
    if (field.errors['min']) {
      return this.formatSeatRangeError('minimum');
    }
    if (field.errors['max']) {
      return this.formatSeatRangeError('maximum');
    }

    return 'Valeur invalide.';
  }

  seatHelpText(): string {
    const rules = this.seatRules();
    if (rules.max === null) {
      return `Minimum ${rules.min} employes pour l'essai ${rules.label}.`;
    }
    return `Entre ${rules.min} et ${rules.max} employes pour l'essai ${rules.label}.`;
  }

  private applySeatRules(): void {
    const rules = this.seatRules();
    const validators = [Validators.required, Validators.min(rules.min)];
    if (rules.max !== null) {
      validators.push(Validators.max(rules.max));
    }

    const seatsControl = this.signupForm.controls.seatsPurchased;
    seatsControl.setValidators(validators);

    const nextValue = rules.max === null
      ? Math.max(seatsControl.value, rules.min)
      : Math.min(Math.max(seatsControl.value, rules.min), rules.max);
    seatsControl.setValue(nextValue);
    seatsControl.updateValueAndValidity({ emitEvent: false });
  }

  private parsePlanCode(value: string | null): SignupPlanCode | null {
    if (value === 'STARTER' || value === 'PRO' || value === 'PREMIUM' || value === 'ENTERPRISE') {
      return value;
    }
    return null;
  }

  private resolveTrialPlan(planCode: SignupPlanCode | null): TrialPlanCode {
    if (planCode === 'PRO' || planCode === 'PREMIUM') {
      return planCode;
    }
    return 'STARTER';
  }

  private formatSeatRangeError(boundary: 'minimum' | 'maximum'): string {
    const rules = this.seatRules();
    if (boundary === 'minimum') {
      return rules.max === null
        ? `Le plan ${rules.label} demande au moins ${rules.min} employes pour demarrer l'essai.`
        : `Le plan ${rules.label} demande entre ${rules.min} et ${rules.max} employes pour demarrer l'essai.`;
    }

    return `Le plan ${rules.label} accepte au maximum ${rules.max} employes pour demarrer l'essai.`;
  }

  private readSignupError(error: HttpErrorResponse): string {
    const backendMessage = error?.error?.message;
    if (typeof backendMessage !== 'string' || !backendMessage.trim()) {
      return 'Une erreur est survenue lors de la creation du compte.';
    }

    switch (backendMessage) {
      case 'This workspace already exists':
        return 'Ce tenant existe deja. Choisissez un autre identifiant ou connectez-vous avec le compte existant.';
      case 'This email is already used in the selected workspace':
        return 'Cette adresse email est deja utilisee dans cet espace.';
      case 'Tenant identifier is required':
        return 'Le tenant est obligatoire.';
      case 'Tenant identifier must contain at least 3 characters':
        return 'Le tenant doit contenir au moins 3 caracteres.';
      case 'Seats below minimum plan size':
        return this.formatSeatRangeError('minimum');
      case 'Seats above maximum plan size':
        return this.formatSeatRangeError('maximum');
      case 'Unable to initialize the starter trial for this workspace.':
      case 'Unable to initialize the trial for this workspace.':
        return "L'essai n'a pas pu etre initialise. Verifiez le plan et le nombre d'employes, puis reessayez.";
      default:
        return backendMessage;
    }
  }
}
