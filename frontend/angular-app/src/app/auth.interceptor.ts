import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from './auth.service';
import { DEFAULT_TENANT_ID } from './config';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const session = authService.session();
  const tenantId = session?.tenantId ?? DEFAULT_TENANT_ID;

  const headers: Record<string, string> = {};
  if (tenantId && !req.headers.has('X-Tenant-Id')) {
    headers['X-Tenant-Id'] = tenantId;
  }
  if (session?.accessToken && !req.headers.has('Authorization')) {
    headers.Authorization = `Bearer ${session.accessToken}`;
  }

  return next(Object.keys(headers).length ? req.clone({ setHeaders: headers }) : req);
};
