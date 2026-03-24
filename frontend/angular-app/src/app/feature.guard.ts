import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from './auth.service';
import { WorkRhApiService, WorkRhVm } from './workrh-api.service';
import { map } from 'rxjs';

export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAuthenticated()) {
    return true;
  }
  return router.parseUrl('/login');
};

export const hrGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.hasRole('HR') || authService.hasRole('ADMIN')) {
    return true;
  }
  return router.parseUrl('/dashboard');
};

export const featureGuard = (feature: string): CanActivateFn => {
  return () => {
    const router = inject(Router);
    const api = inject(WorkRhApiService);

    return api.loadViewModel().pipe(
      map((vm: WorkRhVm) => {
        const allowed = vm.subscription.entitlements.includes(feature);
        return allowed ? true : router.parseUrl('/pricing');
      })
    );
  };
};
