import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes, withComponentInputBinding()),
    // fetch() is the default backend since Angular 22, so withFetch() is no longer needed
    provideHttpClient(
      // functional interceptors, run in this order for every request (errorInterceptor comes in step 8)
      withInterceptors([]),
    ),
  ],
};
