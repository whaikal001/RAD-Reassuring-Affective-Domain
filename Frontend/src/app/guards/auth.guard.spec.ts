import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { authGuard } from './auth.guard';
import { AuthService } from '../services/auth.service';

describe('authGuard', () => {
  it('allows navigation when authenticated', () => {
    const navigateSpy = jasmine.createSpy('navigate');

    TestBed.configureTestingModule({
      providers: [
        { provide: Router, useValue: { navigate: navigateSpy } },
        { provide: AuthService, useValue: { isAuthenticated: () => true } }
      ]
    });

    const result = TestBed.runInInjectionContext(() => authGuard({} as any, { url: '/dashboard' } as any));
    expect(result).toBeTrue();
    expect(navigateSpy).not.toHaveBeenCalled();
  });

  it('redirects unauthenticated users to login', () => {
    const navigateSpy = jasmine.createSpy('navigate');

    TestBed.configureTestingModule({
      providers: [
        { provide: Router, useValue: { navigate: navigateSpy } },
        { provide: AuthService, useValue: { isAuthenticated: () => false } }
      ]
    });

    const result = TestBed.runInInjectionContext(() => authGuard({} as any, { url: '/reports' } as any));
    expect(result).toBeFalse();
    expect(navigateSpy).toHaveBeenCalledWith(['/login'], { queryParams: { returnUrl: '/reports' } });
  });
});
