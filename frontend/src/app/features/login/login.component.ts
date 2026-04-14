import {Component} from '@angular/core';
import {AuthModuleService, LoginRequestDto} from "../../core/api";
import {Router} from "@angular/router";
import {MatSnackBar} from "@angular/material/snack-bar";

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent {
  hidePassword = true

  loginRequest: LoginRequestDto = {
    email: '',
    password: ''
  }

  constructor(
    private authApi: AuthModuleService,
    private router: Router,
    private snackBar: MatSnackBar
  ) {}

  onLogin() {

    this.authApi.login(this.loginRequest).subscribe({
      next: data => {
        console.log("Got")
        console.log(data);
        console.log(data.accessToken);
        if (data.accessToken && data.refreshToken) {
          localStorage.setItem('access_token', data.accessToken);
          localStorage.setItem('refresh_token', data.refreshToken);

          const returnUrl = localStorage.getItem('redirectUrl') || '/dashboard';
          this.router.navigate([returnUrl]);
        }
      },
      error: err => {
        console.log(err);
        this.showError(err.error);
      }
    })

  }

  private showError(message: string) {
    this.snackBar.open(message, 'Close', {
      duration: 3000,
      horizontalPosition: 'center',
      verticalPosition: "bottom",
      panelClass: ['error-snackbar']
    })
  }
}
