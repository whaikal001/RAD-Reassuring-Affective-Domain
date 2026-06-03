import { Injectable, signal } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class SidebarService {
  /** Mobile drawer open/closed (hamburger). */
  isSidebarOpen = signal(false);

  /** Desktop collapse state — hides the sidebar so the page reclaims full width. Persisted. */
  collapsed = signal<boolean>(this.readCollapsed());

  toggle() {
    this.isSidebarOpen.set(!this.isSidebarOpen());
  }

  open() {
    this.isSidebarOpen.set(true);
  }

  close() {
    this.isSidebarOpen.set(false);
  }

  toggleCollapsed() {
    const next = !this.collapsed();
    this.collapsed.set(next);
    try { localStorage.setItem('radai_sidebar_collapsed', String(next)); } catch (e) {}
  }

  private readCollapsed(): boolean {
    try { return localStorage.getItem('radai_sidebar_collapsed') === 'true'; } catch (e) { return false; }
  }
}
