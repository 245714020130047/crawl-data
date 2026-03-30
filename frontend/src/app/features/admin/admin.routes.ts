import { Routes } from '@angular/router';

export const adminRoutes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./admin-dashboard.component').then(m => m.AdminDashboardComponent)
  },
  {
    path: 'sources',
    loadComponent: () =>
      import('./sources/sources.component').then(m => m.SourcesComponent)
  }
];
