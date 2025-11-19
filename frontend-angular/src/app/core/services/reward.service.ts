import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { RewardProductRequest, RewardProductResponse, RedeemRewardRequest } from '../models/reward.model';

@Injectable({
  providedIn: 'root'
})
export class RewardService {
  private apiUrl = `${environment.apiUrl}/admin/rewards`;
  private publicApiUrl = `${environment.apiUrl}/public/rewards`;
  private clientApiUrl = `${environment.apiUrl}/client/rewards`;

  constructor(private http: HttpClient) {}

  // Métodos para administradores
  getAllRewardProducts(): Observable<RewardProductResponse[]> {
    return this.http.get<RewardProductResponse[]>(this.apiUrl);
  }

  getRewardProductById(id: number): Observable<RewardProductResponse> {
    return this.http.get<RewardProductResponse>(`${this.apiUrl}/${id}`);
  }

  createRewardProduct(reward: RewardProductRequest): Observable<RewardProductResponse> {
    return this.http.post<RewardProductResponse>(this.apiUrl, reward);
  }

  updateRewardProduct(id: number, reward: RewardProductRequest): Observable<RewardProductResponse> {
    return this.http.put<RewardProductResponse>(`${this.apiUrl}/${id}`, reward);
  }

  deleteRewardProduct(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  // Métodos públicos (para clientes - solo productos activos)
  getActiveRewardProducts(): Observable<RewardProductResponse[]> {
    return this.http.get<RewardProductResponse[]>(this.publicApiUrl);
  }

  getPublicRewardProductById(id: number): Observable<RewardProductResponse> {
    return this.http.get<RewardProductResponse>(`${this.publicApiUrl}/${id}`);
  }

  // Métodos para clientes (canje)
  redeemReward(redeemRequest: RedeemRewardRequest): Observable<{ message: string; reward: RewardProductResponse }> {
    return this.http.post<{ message: string; reward: RewardProductResponse }>(`${this.clientApiUrl}/redeem`, redeemRequest);
  }
}

