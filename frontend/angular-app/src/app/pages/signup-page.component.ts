import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../auth.service';
import { TranslatePipe } from '../i18n/translate.pipe';
import { I18nService } from '../i18n/i18n.service';
import { PLAN_COMMERCIAL_CONTENT } from '../plan-commercial-content';

type SignupPlanCode = 'STARTER' | 'PRO' | 'PREMIUM' | 'ENTERPRISE';
type TrialPlanCode = 'STARTER' | 'PRO' | 'PREMIUM';
type SignupAccountType = 'HR' | 'EMPLOYEE';
type SignupFieldName = 'companyName' | 'tenantId' | 'firstName' | 'lastName' | 'email' | 'password' | 'seatsPurchased';

const PLAN_SEAT_RULES: Record<TrialPlanCode, { min: number; max: number | null; label: string }> = {
  STARTER: { min: 1, max: 10, label: 'Starter' },
  PRO: { min: 10, max: 50, label: 'Pro' },
  PREMIUM: { min: 50, max: null, label: 'Premium' }
};

@Component({
  selector: 'app-signup-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, TranslatePipe],
  templateUrl: './signup-page.component.html',
  styleUrl: './page-styles.css'
})
export class SignupPageComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly i18n = inject(I18nService);

  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly accountType = signal<SignupAccountType>('HR');
  readonly isHrSignup = computed(() => this.accountType() === 'HR');
  readonly requestedPlan = computed(() => this.parsePlanCode(this.route.snapshot.queryParamMap.get('plan')));
  readonly trialPlan = computed<TrialPlanCode>(() => this.resolveTrialPlan(this.requestedPlan()));
  readonly seatRules = computed(() => PLAN_SEAT_RULES[this.trialPlan()]);
  readonly trialPlanSummary = computed(() => PLAN_COMMERCIAL_CONTENT[this.trialPlan()]);
  readonly trialPlanSummaryDisplay = computed(() => {
    this.i18n.locale();
    const plan = this.trialPlan();
    const headlineKey = `pricing.plan.${plan}.headline`;
    const idealKey = `pricing.plan.${plan}.idealFor`;
    const base = PLAN_COMMERCIAL_CONTENT[plan];
    const headline = this.i18n.translate(headlineKey);
    const idealFor = this.i18n.translate(idealKey);
    return {
      headline: headline !== headlineKey ? headline : base.headline,
      idealFor: idealFor !== idealKey ? idealFor : base.idealFor
    };
  });
  readonly signupForm = this.formBuilder.nonNullable.group({
    companyName: ['', [Validators.required, Validators.minLength(2)]],
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

  selectAccountType(accountType: SignupAccountType): void {
    this.accountType.set(accountType);
    this.errorMessage.set(null);
    const companyNameControl = this.signupForm.controls.companyName;
    if (accountType === 'HR') {
      companyNameControl.setValidators([Validators.required, Validators.minLength(2)]);
      companyNameControl.updateValueAndValidity({ emitEvent: false });
      this.applySeatRules();
      return;
    }

    companyNameControl.clearValidators();
    companyNameControl.updateValueAndValidity({ emitEvent: false });
    const seatsControl = this.signupForm.controls.seatsPurchased;
    seatsControl.clearValidators();
    seatsControl.updateValueAndValidity({ emitEvent: false });
  }

  submit(): void {
    if (this.submitting()) {
      return;
    }

    if (this.signupForm.invalid) {
      this.signupForm.markAllAsTouched();
      this.errorMessage.set(this.i18n.translate('signup.error_incomplete'));
      return;
    }

    this.errorMessage.set(null);
    this.submitting.set(true);
    const raw = this.signupForm.getRawValue();
    this.authService.signup({
      companyName: this.isHrSignup() ? raw.companyName : null,
      tenantId: raw.tenantId,
      firstName: raw.firstName,
      lastName: raw.lastName,
      email: raw.email,
      password: raw.password,
      seatsPurchased: this.isHrSignup() ? raw.seatsPurchased : null,
      accountType: this.accountType(),
      planCode: this.requestedPlan()
    }).subscribe({
      next: () => {
        this.submitting.set(false);
        if (!this.isHrSignup()) {
          this.router.navigateByUrl('/employee');
          return;
        }

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
      return this.i18n.translate('signup.validation.required');
    }
    if (field.errors['email']) {
      return this.i18n.translate('signup.validation.email');
    }
    if (field.errors['minlength']) {
      const requiredLength = field.errors['minlength'].requiredLength;
      return this.i18n.translate('signup.validation.minlength', { n: requiredLength });
    }
    if (field.errors['min']) {
      return this.formatSeatRangeError('minimum');
    }
    if (field.errors['max']) {
      return this.formatSeatRangeError('maximum');
    }

    return this.i18n.translate('signup.validation.invalid');
  }

  seatHelpText(): string {
    this.i18n.locale();
    const rules = this.seatRules();
    if (rules.max === null) {
      return this.i18n.translate('signup.seat_help_min', { min: rules.min, plan: rules.label });
    }
    return this.i18n.translate('signup.seat_help_range', {
      min: rules.min,
      max: rules.max,
      plan: rules.label
    });
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
    this.i18n.locale();
    const rules = this.seatRules();
    if (boundary === 'minimum') {
      return rules.max === null
        ? this.i18n.translate('signup.seat_error_min', { plan: rules.label, min: rules.min })
        : this.i18n.translate('signup.seat_error_between', {
            plan: rules.label,
            min: rules.min,
            max: rules.max
          });
    }

    return this.i18n.translate('signup.seat_error_max', { plan: rules.label, max: rules.max ?? '' });
  }

  private readSignupError(error: HttpErrorResponse): string {
    const backendMessage = error?.error?.message;
    if (typeof backendMessage !== 'string' || !backendMessage.trim()) {
      return this.i18n.translate('signup.error_generic');
    }

    switch (backendMessage) {
      case 'This workspace already exists':
        return this.i18n.translate('signup.err.workspace_exists');
      case 'This email is already used in the selected workspace':
        return this.i18n.translate('signup.err.email_used');
      case 'Employee signup requires an existing workspace':
        return this.i18n.translate('signup.err.tenant_missing');
      case 'Tenant identifier is required':
        return this.i18n.translate('signup.err.tenant_required');
      case 'Tenant identifier must contain at least 3 characters':
        return this.i18n.translate('signup.err.tenant_short');
      case 'Seats purchased is required for HR signup':
        return this.i18n.translate('signup.err.seats_required');
      case 'Seats below minimum plan size':
        return this.formatSeatRangeError('minimum');
      case 'Seats above maximum plan size':
        return this.formatSeatRangeError('maximum');
      case 'Unable to initialize the starter trial for this workspace.':
      case 'Unable to initialize the trial for this workspace.':
        return this.i18n.translate('signup.err.trial_init');
      default:
        return backendMessage;
    }
  }
}
