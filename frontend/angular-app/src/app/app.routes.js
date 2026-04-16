import { authGuard, featureGuard, hrGuard, landingGuard } from './feature.guard';
import { DashboardPageComponent } from './pages/dashboard-page.component';
import { BillingPageComponent } from './pages/billing-page.component';
import { EmployeePortalPageComponent } from './pages/employee-portal-page.component';
import { LoginPageComponent } from './pages/login-page.component';
import { PoliciesPageComponent } from './pages/policies-page.component';
import { PricingPageComponent } from './pages/pricing-page.component';
export const appRoutes = [
    { path: '', pathMatch: 'full', component: PricingPageComponent, canActivate: [landingGuard] },
    { path: 'offres', pathMatch: 'full', redirectTo: 'pricing' },
    { path: 'login', component: LoginPageComponent },
    { path: 'dashboard', component: DashboardPageComponent, canActivate: [authGuard, hrGuard] },
    { path: 'employee', component: EmployeePortalPageComponent, canActivate: [authGuard] },
    { path: 'pricing', component: PricingPageComponent },
    { path: 'billing', component: BillingPageComponent, canActivate: [authGuard, hrGuard] },
    {
        path: 'policies',
        component: PoliciesPageComponent,
        canActivate: [authGuard, hrGuard, featureGuard('TELEWORK_COMPLIANCE_34')]
    },
    { path: '**', redirectTo: '' }
];
