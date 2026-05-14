import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from './auth.service';
import { DEFAULT_TENANT_ID } from './config';

const PUBLIC_GET_PATHS = new Set([
  '/api/subscriptions/plans',
  '/api/subscriptions/catalog/readiness'
]);

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const session = authService.session();
  const tenantId = session?.tenantId ?? DEFAULT_TENANT_ID;
  const publicGetRequest = req.method === 'GET' && PUBLIC_GET_PATHS.has(requestPath(req.url));

  const headers: Record<string, string> = {};
  if (tenantId && !req.headers.has('X-Tenant-Id')) {
    headers['X-Tenant-Id'] = tenantId;
  }
  if (!publicGetRequest && session?.accessToken && !req.headers.has('Authorization')) {
    headers.Authorization = `Bearer ${session.accessToken}`;
  }

  return next(Object.keys(headers).length ? req.clone({ setHeaders: headers }) : req);
};

function requestPath(url: string): string {
  try {
    return new URL(url, window.location.origin).pathname;
  } catch {
    return url.split('?')[0] ?? url;
  }
}
