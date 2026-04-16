import { inject } from '@angular/core';
import { AuthService } from './auth.service';
export const authInterceptor = (req, next) => {
    const authService = inject(AuthService);
    const session = authService.session();
    const headers = {};
    if (!req.headers.has('X-Tenant-Id')) {
        headers['X-Tenant-Id'] = session?.tenantId ?? 'demo-lu';
    }
    if (session?.accessToken && !req.headers.has('Authorization')) {
        headers.Authorization = `Bearer ${session.accessToken}`;
    }
    return next(Object.keys(headers).length ? req.clone({ setHeaders: headers }) : req);
};
