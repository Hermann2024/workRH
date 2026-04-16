import 'zone.js';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { bootstrapApplication } from '@angular/platform-browser';
import { provideRouter } from '@angular/router';
import { AppComponent } from './app/app.component';
import { authInterceptor } from './app/auth.interceptor';
import { appRoutes } from './app/app.routes';
bootstrapApplication(AppComponent, {
    providers: [provideHttpClient(withInterceptors([authInterceptor])), provideRouter(appRoutes)]
}).catch(err => console.error(err));
