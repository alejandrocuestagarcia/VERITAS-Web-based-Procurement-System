import {Component} from '@angular/core';
import {AuthModuleService, LoginRequestDto} from "../../core/api";
import {Router} from "@angular/router";

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
    private router: Router
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
      }
    })

  }
}
