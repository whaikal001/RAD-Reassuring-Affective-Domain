import { Routes } from '@angular/router';
import { authGuard } from './guards/auth.guard';
import { registeredGuard } from './guards/registered.guard';
import { adminGuard } from './guards/admin.guard';
import { noAdminGuard } from './guards/no-admin.guard';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/landing/landing.component').then(m => m.LandingComponent),
    data: { animation: 'LandingPage' }
  },
  {
    path: 'login',
    loadComponent: () => import('./pages/login/login.component').then(m => m.LoginComponent),
    data: { animation: 'LoginPage' }
  },
  {
    path: 'register',
    loadComponent: () => import('./pages/register/register.component').then(m => m.RegisterComponent),
    data: { animation: 'RegisterPage' }
  },
  {
    path: 'chat',
    loadComponent: () => import('./pages/chat/chat.component').then(m => m.ChatComponent),
    canActivate: [authGuard, noAdminGuard],
    data: { animation: 'ChatPage' }
  },
  {
    path: 'dashboard',
    loadComponent: () => import('./pages/dashboard/dashboard.component').then(m => m.DashboardComponent),
    canActivate: [registeredGuard],
    data: { animation: 'DashboardPage' }
  },
  {
    path: 'admin',
    loadComponent: () => import('./pages/admin/admin.component').then(m => m.AdminComponent),
    canActivate: [adminGuard],
    data: { animation: 'AdminPage' }
  },
  {
    path: 'history',
    loadComponent: () => import('./pages/history/history.component').then(m => m.HistoryComponent),
    canActivate: [registeredGuard],
    data: { animation: 'HistoryPage' }
  },
  {
    path: 'session/:id',
    loadComponent: () => import('./pages/session-detail/session-detail.component').then(m => m.SessionDetailComponent),
    canActivate: [registeredGuard],
    data: { animation: 'SessionDetailPage' }
  },
  {
    path: 'reports',
    loadComponent: () => import('./pages/reports/reports.component').then(m => m.ReportsComponent),
    canActivate: [registeredGuard],
    data: { animation: 'ReportsPage' }
  },
  {
    path: 'profile',
    loadComponent: () => import('./pages/profile/profile.component').then(m => m.ProfileComponent),
    canActivate: [registeredGuard],
    data: { animation: 'ProfilePage' }
  },
  {
    path: '**',
    redirectTo: 'chat'
  }
];
