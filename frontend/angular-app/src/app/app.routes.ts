import { Routes } from '@angular/router';
import { authGuard, featureGuard, hrGuard } from './feature.guard';
import { DashboardPageComponent } from './pages/dashboard-page.component';
import { BillingPageComponent } from './pages/billing-page.component';
import { LoginPageComponent } from './pages/login-page.component';
import { PoliciesPageComponent } from './pages/policies-page.component';
import { PricingPageComponent } from './pages/pricing-page.component';

export const appRoutes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
  { path: 'login', component: LoginPageComponent },
  { path: 'dashboard', component: DashboardPageComponent, canActivate: [authGuard] },
  { path: 'pricing', component: PricingPageComponent },
  { path: 'billing', component: BillingPageComponent, canActivate: [authGuard, hrGuard] },
  {
    path: 'policies',
    component: PoliciesPageComponent,
    canActivate: [authGuard, hrGuard, featureGuard('TELEWORK_COMPLIANCE_34')]
  },
  { path: '**', redirectTo: 'dashboard' }
];
