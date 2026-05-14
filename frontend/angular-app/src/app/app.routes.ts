import { Routes } from '@angular/router';
import { authGuard, employeeRecordGuard, featureGuard, hrGuard, landingGuard, platformGuard } from './feature.guard';
import { DashboardPageComponent } from './pages/dashboard-page.component';
import { BillingPageComponent } from './pages/billing-page.component';
import { EmployeePortalPageComponent } from './pages/employee-portal-page.component';
import { EmployeeManagementPageComponent } from './pages/employee-management-page.component';
import { AcceptInvitationPageComponent } from './pages/accept-invitation-page.component';
import { LoginPageComponent } from './pages/login-page.component';
import { ForgotPasswordPageComponent } from './pages/forgot-password-page.component';
import { ResetPasswordPageComponent } from './pages/reset-password-page.component';
import { PoliciesPageComponent } from './pages/policies-page.component';
import { PricingPageComponent } from './pages/pricing-page.component';
import { ServicesPageComponent } from './pages/services-page.component';
import { SignupPageComponent } from './pages/signup-page.component';
import { SupportPageComponent } from './pages/support-page.component';
import { PlatformAdminPageComponent } from './pages/platform-admin-page.component';
import { OnboardingPageComponent } from './pages/onboarding-page.component';
import { TrustPageComponent } from './pages/trust-page.component';

export const appRoutes: Routes = [
  { path: '', pathMatch: 'full', component: PricingPageComponent, canActivate: [landingGuard] },
  { path: 'offres', pathMatch: 'full', redirectTo: 'pricing' },
  { path: 'login', component: LoginPageComponent },
  { path: 'forgot-password', component: ForgotPasswordPageComponent },
  { path: 'reset-password', component: ResetPasswordPageComponent },
  { path: 'invite', component: AcceptInvitationPageComponent },
  { path: 'signup', component: SignupPageComponent, canActivate: [landingGuard] },
  { path: 'dashboard', component: DashboardPageComponent, canActivate: [authGuard, hrGuard] },
  { path: 'onboarding', component: OnboardingPageComponent, canActivate: [authGuard, hrGuard] },
  { path: 'trust', component: TrustPageComponent, canActivate: [authGuard, hrGuard] },
  { path: 'services', component: ServicesPageComponent, canActivate: [authGuard, hrGuard] },
  { path: 'employees', component: EmployeeManagementPageComponent, canActivate: [authGuard, hrGuard, featureGuard('EMPLOYEE_MANAGEMENT')] },
  { path: 'employee/:employeeId', component: EmployeePortalPageComponent, canActivate: [authGuard, employeeRecordGuard, featureGuard('EMPLOYEE_MANAGEMENT')] },
  { path: 'employee', component: EmployeePortalPageComponent, canActivate: [authGuard] },
  { path: 'pricing', component: PricingPageComponent },
  { path: 'support', component: SupportPageComponent, canActivate: [authGuard] },
  { path: 'platform', component: PlatformAdminPageComponent, canActivate: [authGuard, platformGuard] },
  { path: 'billing', component: BillingPageComponent, canActivate: [authGuard, hrGuard] },
  {
    path: 'policies',
    component: PoliciesPageComponent,
    canActivate: [authGuard, hrGuard, featureGuard('TELEWORK_COMPLIANCE_34')]
  },
  { path: '**', redirectTo: '' }
];
