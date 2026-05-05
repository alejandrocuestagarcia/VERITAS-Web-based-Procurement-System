import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { ToastService } from '../../../core/services/toast.service';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-forgot-password',
  templateUrl: './forgot-password.component.html'
})
export class ForgotPasswordComponent {
  requestForm: FormGroup;
  loading = false;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private toastService: ToastService
  ) {
    this.requestForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]]
    });
  }

  onSubmit() {
    if (this.requestForm.invalid) {
      this.requestForm.markAllAsTouched();
      return;
    }

    this.loading = true;
    const email = this.requestForm.value.email as string;

    this.authService.requestPasswordReset(email).subscribe({
      next: () => this.handleCompletion(),
      error: () => this.handleCompletion()
    });
  }

  onBackToLogin() {
    this.router.navigate(['/login']);
  }

  private handleCompletion() {
    this.loading = false;
    this.toastService.showInfo('If the email exists, a reset link has been sent.');
  }
}
