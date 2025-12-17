import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface ForgotPasswordRequest {
  email: string;
}

export interface CheckEmailRequest {
  email: string;
}

export interface CheckEmailResponse {
  exists: boolean;
  message: string;
}

export interface VerifyResetCodeRequest {
  email: string;
  code: string;
}

export interface ResetPasswordRequest {
  email: string;
  code: string;
  newPassword: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

@Injectable({
  providedIn: 'root'
})
export class PasswordService {
  private apiUrl = `${environment.apiUrl}/auth`;

  constructor(private http: HttpClient) {}

  checkEmail(request: CheckEmailRequest): Observable<CheckEmailResponse> {
    return this.http.post<CheckEmailResponse>(`${this.apiUrl}/check-email`, request);
  }

  forgotPassword(request: ForgotPasswordRequest): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.apiUrl}/forgot-password`, request);
  }

  verifyResetCode(request: VerifyResetCodeRequest): Observable<{ valid: boolean; message: string }> {
    return this.http.post<{ valid: boolean; message: string }>(`${this.apiUrl}/verify-reset-code`, request);
  }

  resetPassword(request: ResetPasswordRequest): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.apiUrl}/reset-password`, request);
  }

  changePassword(request: ChangePasswordRequest): Observable<{ message: string }> {
    return this.http.put<{ message: string }>(`${environment.apiUrl}/client/change-password`, request);
  }
}

