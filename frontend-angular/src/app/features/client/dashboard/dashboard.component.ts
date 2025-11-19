import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../core/services/auth.service';
import { ClientService } from '../../../core/services/client.service';
import { RewardService } from '../../../core/services/reward.service';
import { UserResponse } from '../../../core/models/user.model';
import { RewardProductResponse } from '../../../core/models/reward.model';
import { RouterLink } from '@angular/router';
import { FormatCurrencyPipe } from '../../../shared/pipes/format-currency.pipe';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, FormatCurrencyPipe],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit {
  userInfo = signal<UserResponse | null>(null);
  rewardProducts = signal<RewardProductResponse[]>([]);
  loading = signal<boolean>(false);
  loadingRewards = signal<boolean>(false);
  errorMessage = signal<string | null>(null);
  successMessage = signal<string | null>(null);

  constructor(
    public authService: AuthService,
    private clientService: ClientService,
    private rewardService: RewardService
  ) {}

  ngOnInit(): void {
    this.loadUserInfo();
    this.loadRewards();
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

  loadRewards(): void {
    this.loadingRewards.set(true);
    this.rewardService.getActiveRewardProducts().subscribe({
      next: (rewards) => {
        this.rewardProducts.set(rewards);
        this.loadingRewards.set(false);
      },
      error: (error) => {
        console.error('Error loading rewards:', error);
        this.loadingRewards.set(false);
      }
    });
  }

  canRedeem(reward: RewardProductResponse): boolean {
    const userPoints = this.userInfo()?.points || 0;
    return userPoints >= reward.pointsRequired && reward.stock > 0 && reward.isActive;
  }

  redeemReward(reward: RewardProductResponse): void {
    if (!this.canRedeem(reward)) {
      return;
    }

    if (!confirm(`¿Estás seguro de canjear "${reward.name}" por ${reward.pointsRequired} puntos?`)) {
      return;
    }

    this.errorMessage.set(null);
    this.successMessage.set(null);

    this.rewardService.redeemReward({ rewardProductId: reward.id }).subscribe({
      next: (response) => {
        this.successMessage.set(response.message || '¡Recompensa canjeada exitosamente!');
        this.loadUserInfo(); // Recargar información del usuario para actualizar puntos
        this.loadRewards(); // Recargar recompensas para actualizar stock
        setTimeout(() => {
          this.successMessage.set(null);
        }, 5000);
      },
      error: (error) => {
        console.error('Error redeeming reward:', error);
        this.errorMessage.set(error.error?.message || 'Error al canjear la recompensa');
        setTimeout(() => {
          this.errorMessage.set(null);
        }, 5000);
      }
    });
  }

  logout(): void {
    this.authService.logout();
  }
}
