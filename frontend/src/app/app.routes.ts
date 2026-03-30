import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { adminGuard } from './core/guards/admin.guard';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./features/newsfeed/newsfeed.component').then(m => m.NewsfeedComponent)
  },
  {
    path: 'article/:id',
    loadComponent: () =>
      import('./features/article-detail/article-detail.component').then(m => m.ArticleDetailComponent)
  },
  {
    path: 'search',
    loadComponent: () =>
      import('./features/search/search.component').then(m => m.SearchComponent)
  },
  {
    path: 'category/:id',
    loadComponent: () =>
      import('./features/newsfeed/newsfeed.component').then(m => m.NewsfeedComponent)
  },
  {
    path: 'auth/login',
    loadComponent: () =>
      import('./features/auth/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'auth/register',
    loadComponent: () =>
      import('./features/auth/register/register.component').then(m => m.RegisterComponent)
  },
  {
    path: 'profile',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/user-profile/user-profile.component').then(m => m.UserProfileComponent)
  },
  {
    path: 'admin',
    canActivate: [authGuard, adminGuard],
    loadChildren: () =>
      import('./features/admin/admin.routes').then(m => m.adminRoutes)
  },
  { path: '**', redirectTo: '' }
];
