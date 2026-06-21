import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ToastService } from '../../../core/services/toast.service';
import { AuthService } from '../../../core/services/auth.service';
import { extractErrorMessage } from '../../../shared/error-utils';

@Component({
  selector: 'app-reset-password',
  templateUrl: './reset-password.component.html'
})
export class ResetPasswordComponent {
  resetForm: FormGroup;
  hidePassword = true;
  hideConfirmPassword = true;
  loading = false;
  token = '';
  tokenMissing = false;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private route: ActivatedRoute,
    private router: Router,
    private toastService: ToastService
  ) {
    this.resetForm = this.fb.group({
      newPassword: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', [Validators.required]]
    }, { validators: this.passwordMatchValidator });

    this.route.queryParamMap.subscribe(params => {
      this.token = params.get('token') || '';
      this.tokenMissing = !this.token;
    });
  }

  passwordMatchValidator(g: FormGroup) {
    return g.get('newPassword')?.value === g.get('confirmPassword')?.value
      ? null : { mismatch: true };
  }

  onSubmit() {
    if (this.resetForm.invalid || this.tokenMissing) {
      this.resetForm.markAllAsTouched();
      return;
    }

    this.loading = true;
    const password = this.resetForm.value.newPassword as string;

    this.authService.confirmPasswordReset(this.token, password).subscribe({
      next: () => {
        this.loading = false;
        this.toastService.showSuccess('Password reset successful. You can now log in.');
        this.router.navigate(['/login']);
      },
      error: (err: any) => this.handleResetError(err)
    });
  }

  onRequestNew() {
    this.router.navigate(['/forgot-password']);
  }

  private handleResetError(err: any): void {
    this.loading = false;
    this.toastService.showError(extractErrorMessage(err, 'Password Reset failed'));
  }
}
