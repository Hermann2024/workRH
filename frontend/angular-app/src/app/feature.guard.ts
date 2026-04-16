import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from './auth.service';
import { WorkRhApiService } from './workrh-api.service';
import { catchError, map, of } from 'rxjs';

export const landingGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (!authService.isAuthenticated()) {
    return true;
  }

  const employeeOnly = authService.hasRole('EMPLOYEE')
    && !authService.hasRole('HR')
    && !authService.hasRole('ADMIN');

  return router.parseUrl(employeeOnly ? '/employee' : '/dashboard');
};

export const authGuard: CanActivateFn = (_route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAuthenticated()) {
    return true;
  }
  return router.createUrlTree(['/login'], {
    queryParams: {
      returnUrl: state.url
    }
  });
};

export const hrGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.hasRole('HR') || authService.hasRole('ADMIN')) {
    return true;
  }
  return router.parseUrl(authService.hasRole('EMPLOYEE') ? '/employee' : '/pricing');
};

export const featureGuard = (feature: string): CanActivateFn => {
  return () => {
    const router = inject(Router);
    const api = inject(WorkRhApiService);

    return api.checkFeature(feature).pipe(
      map((response) => response.allowed ? true : router.parseUrl('/pricing')),
      catchError(() => of(router.parseUrl('/pricing')))
    );
  };
};
