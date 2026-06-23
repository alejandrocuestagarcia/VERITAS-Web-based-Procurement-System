import { Injectable } from '@angular/core';
import { Router } from "@angular/router";
import { AuthModuleService, RefreshTokenDto, LoginRequestDto, AuthResponseDto, UserModuleService, UserDto } from "../api";
import { tap, Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  refreshTokenDto: RefreshTokenDto = {
    refreshToken: ''
  };

  private currentUserProfile: UserDto | null = null;

  constructor(
    private authApi: AuthModuleService,
    private userApi: UserModuleService,
    private router: Router
  ) {
  }

  getToken(): string | null {
    return localStorage.getItem('access_token');
  }

  getDecodedToken(): any | null {
    const token = this.getToken();
    if (!token) {
      return null;
    }

    try {
      const payload = token.split('.')[1];
      const decodedPayload = atob(payload);
      return JSON.parse(decodedPayload);
    } catch (e) {
      console.error('Error decoding token', e);
      return null;
    }
  }

  getRole(): string | null {
    const decodedToken = this.getDecodedToken();
    return decodedToken ? decodedToken.role : null;
  }

  getUserId(): number | null {
    const decodedToken = this.getDecodedToken();
    return decodedToken ? decodedToken.id : null;
  }

  isPasswordChangeRequired(): boolean {
    return localStorage.getItem('requires_password_change') === 'true';
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }

  hasRole(role: string): boolean {
    const userRole = this.getRole();
    return userRole === role;
  }

  hasAnyRole(roles: string[]): boolean {
    const userRole = this.getRole();
    if (!userRole) {
      return false;
    }
    return roles.includes(userRole);
  }

  getCurrentUser(): Observable<UserDto | null> {
    if (this.currentUserProfile) {
      return of(this.currentUserProfile);
    }
    return this.userApi.getCurrentUser().pipe(
      tap(profile => this.currentUserProfile = profile),
      catchError(() => {
        this.currentUserProfile = null;
        return of(null);
      })
    );
  }

  hasTeamSync(): boolean {
    const role = this.getRole();
    if (role !== 'REQUESTER') {
      return true;
    }
    return this.currentUserProfile?.teamId != null;
  }

  clearProfile(): void {
    this.currentUserProfile = null;
  }

  login(loginRequest: LoginRequestDto) {
    this.clearProfile();
    return this.authApi.login(loginRequest).pipe(
      tap((res: AuthResponseDto) => {
        if (res.accessToken && res.refreshToken) {
          localStorage.setItem('access_token', res.accessToken);
          localStorage.setItem('refresh_token', res.refreshToken);
          // Store the reset flag for the Auth Guard to check
          localStorage.setItem('requires_password_change', String(res.requiresPasswordChange));
        }
      })
    );
  }

  logout(): void {
    this.clearProfile();
    const refreshToken = localStorage.getItem('refresh_token');

    localStorage.removeItem('access_token');
    localStorage.removeItem('refresh_token');

    if (refreshToken) {
      this.refreshTokenDto.refreshToken = refreshToken;
      this.authApi.logout(this.refreshTokenDto).subscribe()
    }

    this.router.navigate(['/login']);
  }

  adminResetPassword(userId: number, tempPassword: string) {
    return this.authApi.adminResetPassword(userId, tempPassword);
  }

  completePasswordChange(newPassword: string) {
    return this.authApi.completePasswordChange(newPassword);
  }

  requestPasswordReset(email: string) {
    return this.authApi.requestPasswordReset({ email });
  }

  confirmPasswordReset(token: string, newPassword: string) {
    return this.authApi.confirmPasswordReset({ token, newPassword });
  }

}
