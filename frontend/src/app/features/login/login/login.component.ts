import {Component} from '@angular/core';
import {AuthResponseDto, LoginRequestDto} from "../../../core/api";
import {Router} from "@angular/router";
import {MatSnackBar} from "@angular/material/snack-bar";
import {AuthService} from "../../../core/services/auth.service";

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
})
export class LoginComponent {
  hidePassword = true

  loginRequest: LoginRequestDto = {
    email: '',
    password: ''
  }

  constructor(
    private authService: AuthService,
    private router: Router,
    private snackBar: MatSnackBar
  ) {
  }

  onLogin() {
    this.authService.login(this.loginRequest).subscribe({
      next: (res: AuthResponseDto) => {
        if (res.requiresPasswordChange) {
          this.router.navigate(['/force-password-reset']);
        } else {
          const returnUrl = localStorage.getItem('redirectUrl') || '/dashboard';
          this.router.navigate([returnUrl]);
        }
      },
      error: (err: any) => {
        this.showError(err.error);
      }
    });
  }

  private showError(message: string) {
    this.snackBar.open(message, 'Close', {
      duration: 3000,
      horizontalPosition: 'end',
      verticalPosition: "top",
      panelClass: ['error-snackbar']
    })
  }
}
