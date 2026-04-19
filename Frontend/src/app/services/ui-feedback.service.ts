import { Injectable, signal } from '@angular/core';

export type ToastType = 'success' | 'info' | 'warning' | 'danger';

export interface ToastMessage {
  id: string;
  type: ToastType;
  title: string;
  message: string;
  timestamp: number;
}

@Injectable({
  providedIn: 'root'
})
export class UiFeedbackService {
  private readonly _online = signal<boolean>(typeof navigator !== 'undefined' ? navigator.onLine : true);
  private readonly _toasts = signal<ToastMessage[]>([]);

  readonly online = this._online.asReadonly();
  readonly toasts = this._toasts.asReadonly();

  constructor() {
    if (typeof window !== 'undefined') {
      window.addEventListener('online', () => this._online.set(true));
      window.addEventListener('offline', () => this._online.set(false));
    }
  }

  notify(type: ToastType, title: string, message: string, durationMs = 4500): void {
    const toast: ToastMessage = {
      id: this.generateId(),
      type,
      title,
      message,
      timestamp: Date.now()
    };

    this._toasts.update(items => [...items, toast]);

    if (durationMs > 0) {
      setTimeout(() => this.dismiss(toast.id), durationMs);
    }
  }

  success(title: string, message: string, durationMs?: number): void {
    this.notify('success', title, message, durationMs);
  }

  info(title: string, message: string, durationMs?: number): void {
    this.notify('info', title, message, durationMs);
  }

  warning(title: string, message: string, durationMs?: number): void {
    this.notify('warning', title, message, durationMs);
  }

  error(title: string, message: string, durationMs?: number): void {
    this.notify('danger', title, message, durationMs);
  }

  dismiss(id: string): void {
    this._toasts.update(items => items.filter(item => item.id !== id));
  }

  private generateId(): string {
    return Math.random().toString(36).slice(2);
  }
}
