import { Injectable } from '@angular/core';
import {HttpInterceptor, HttpRequest, HttpHandler, HttpEvent, HttpErrorResponse} from '@angular/common/http';
import {catchError, Observable, switchMap, throwError} from 'rxjs';
import {AuthModuleService, RefreshTokenDto} from "../api";
import {AuthService} from "../services/auth.service";

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  refreshTokenDto: RefreshTokenDto = {
    refreshToken: ''
  };

  constructor(
    private authApi: AuthModuleService,
    private authService: AuthService,
  ) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const token = localStorage.getItem('access_token');

    if (req.url.includes('/auth/refresh')) {
      return next.handle(req);
    }

    return next.handle(this.addToken(req, token)).pipe(
      catchError(error => {
        if (error instanceof HttpErrorResponse && error.status === 401) {
          return this.handleError(req, next);
        }
        return throwError(() => error);
      })
    );
  }

  private addToken(req: HttpRequest<any>, token: string | null): HttpRequest<any> {
    if (!token) return req;
    return req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
  }

  private handleError(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const refreshToken = localStorage.getItem('refresh_token');

    if (!refreshToken) {
      this.authService.logout();
      return throwError(() => new Error('No refresh token'));
    }

    this.refreshTokenDto.refreshToken = refreshToken;
    return this.authApi.refresh(this.refreshTokenDto).pipe(
      switchMap(response => {
        if (!response.accessToken) {
          this.authService.logout();
          return throwError(() => new Error('No access token in refresh response'));
        }

        localStorage.setItem('access_token', response.accessToken);
        return next.handle(this.addToken(req, response.accessToken));
      }),
      catchError(err => {
        this.authService.logout();
        return throwError(() => err);
      })
    );
  }
}
