import {Injectable} from '@angular/core';
import {Router} from "@angular/router";
import {AuthModuleService, RefreshTokenDto} from "../api";

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
}
