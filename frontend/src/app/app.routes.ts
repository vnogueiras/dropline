import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'events',
    pathMatch: 'full',
  },
  {
    path: 'events',
    loadComponent: () => import('./features/events/pages/events/events').then((m) => m.Events),
  },
  {
    path: 'events/:id',
    loadComponent: () =>
      import('./features/events/pages/event-details/event-details').then((m) => m.EventDetails),
  },
  {
    path: '**',
    loadComponent: () => import('./core/pages/not-found/not-found').then((m) => m.NotFound),
  },
];
