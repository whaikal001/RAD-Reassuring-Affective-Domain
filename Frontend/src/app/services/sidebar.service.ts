import { Injectable, signal } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class SidebarService {
  isSidebarOpen = signal(false);

  toggle() {
    this.isSidebarOpen.set(!this.isSidebarOpen());
  }

  open() {
    this.isSidebarOpen.set(true);
  }

  close() {
    this.isSidebarOpen.set(false);
  }
}
