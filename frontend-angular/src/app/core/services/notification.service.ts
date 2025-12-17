import { Injectable, signal, computed } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

export interface ToastNotification {
  id: string;
  message: string;
  type: 'success' | 'error' | 'info' | 'warning';
  duration?: number;
}

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private notifications = signal<ToastNotification[]>([]);
  public notifications$ = computed(() => this.notifications());

  private notificationId = 0;

  constructor() {}

  show(message: string, type: 'success' | 'error' | 'info' | 'warning' = 'info', duration: number = 3000): void {
    const id = `toast-${++this.notificationId}`;
    const notification: ToastNotification = {
      id,
      message,
      type,
      duration
    };

    this.notifications.update(notifications => [...notifications, notification]);

    if (duration > 0) {
      setTimeout(() => {
        this.remove(id);
      }, duration);
    }
  }

  success(message: string, duration: number = 3000): void {
    this.show(message, 'success', duration);
  }

  error(message: string, duration: number = 5000): void {
    this.show(message, 'error', duration);
  }

  info(message: string, duration: number = 3000): void {
    this.show(message, 'info', duration);
  }

  warning(message: string, duration: number = 4000): void {
    this.show(message, 'warning', duration);
  }

  remove(id: string): void {
    this.notifications.update(notifications => 
      notifications.filter(n => n.id !== id)
    );
  }

  clear(): void {
    this.notifications.set([]);
  }
}

