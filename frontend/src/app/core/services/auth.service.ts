import {Injectable} from '@angular/core';
import {Router} from "@angular/router";
import {AuthModuleService, RefreshTokenDto, LoginRequestDto, AuthResponseDto} from "../api";
import {tap} from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  refreshTokenDto: RefreshTokenDto = {
    refreshToken: ''
  };

  constructor(
    private authApi: AuthModuleService,
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

  login(loginRequest: LoginRequestDto) {
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

}
