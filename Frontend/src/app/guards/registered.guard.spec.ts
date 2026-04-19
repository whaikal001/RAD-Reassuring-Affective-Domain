import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { registeredGuard } from './registered.guard';
import { AuthService } from '../services/auth.service';

describe('registeredGuard', () => {
  it('blocks unauthenticated users and routes to login', () => {
    const navigateSpy = jasmine.createSpy('navigate');

    TestBed.configureTestingModule({
      providers: [
        { provide: Router, useValue: { navigate: navigateSpy } },
        {
          provide: AuthService,
          useValue: {
            isAuthenticated: () => false,
            isAnonymous: () => false
          }
        }
      ]
    });

    const result = TestBed.runInInjectionContext(() => registeredGuard({} as any, { url: '/reports' } as any));
    expect(result).toBeFalse();
    expect(navigateSpy).toHaveBeenCalledWith(['/login']);
  });

  it('blocks anonymous users and routes to chat', () => {
    const navigateSpy = jasmine.createSpy('navigate');

    TestBed.configureTestingModule({
      providers: [
        { provide: Router, useValue: { navigate: navigateSpy } },
        {
          provide: AuthService,
          useValue: {
            isAuthenticated: () => true,
            isAnonymous: () => true
          }
        }
      ]
    });

    const result = TestBed.runInInjectionContext(() => registeredGuard({} as any, { url: '/history' } as any));
    expect(result).toBeFalse();
    expect(navigateSpy).toHaveBeenCalledWith(['/chat']);
  });

  it('allows authenticated registered users', () => {
    const navigateSpy = jasmine.createSpy('navigate');

    TestBed.configureTestingModule({
      providers: [
        { provide: Router, useValue: { navigate: navigateSpy } },
        {
          provide: AuthService,
          useValue: {
            isAuthenticated: () => true,
            isAnonymous: () => false
          }
        }
      ]
    });

    const result = TestBed.runInInjectionContext(() => registeredGuard({} as any, { url: '/dashboard' } as any));
    expect(result).toBeTrue();
    expect(navigateSpy).not.toHaveBeenCalled();
  });
});
