import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../core/services/auth.service';
import { ClientService } from '../../../core/services/client.service';
import { UserResponse } from '../../../core/models/user.model';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit {
  userInfo = signal<UserResponse | null>(null);
  loading = signal<boolean>(false);
  errorMessage = signal<string | null>(null);

  constructor(
    public authService: AuthService,
    private clientService: ClientService
  ) {}

  ngOnInit(): void {
    this.loadUserInfo();
  }

  loadUserInfo(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    this.clientService.getProfile().subscribe({
      next: (user) => {
        this.userInfo.set(user);
        this.loading.set(false);
      },
      error: (error) => {
        console.error('Error loading user profile:', error);
        this.errorMessage.set(error.error?.message || 'Error al cargar información del usuario');
        this.loading.set(false);
      }
    });
  }

  logout(): void {
    this.authService.logout();
  }
}
