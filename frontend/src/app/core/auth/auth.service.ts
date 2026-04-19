import { Injectable } from '@angular/core';
import {AuthModuleService, RefreshTokenDto} from "../api";
import {Router} from "@angular/router";

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
  ) { }

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
