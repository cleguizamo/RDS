import { Component, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NotificationService, ToastNotification } from '../../core/services/notification.service';

@Component({
  selector: 'app-toast',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './toast.component.html',
  styleUrl: './toast.component.css'
})
export class ToastComponent {
  notifications = computed(() => this.notificationService.notifications$());

  constructor(public notificationService: NotificationService) {}

  removeNotification(id: string): void {
    this.notificationService.remove(id);
  }
}

