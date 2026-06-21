// AI-GENERATED
import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';
import { extractErrorMessage } from '../../../shared/utils/error-utils';

@Component({
  selector: 'app-force-password-reset',
  templateUrl: './force-password-reset.component.html',
})
export class ForcePasswordResetComponent {
  resetForm: FormGroup;
  hidePassword = true;
  hideConfirmPassword = true;
  loading = false;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private toastService: ToastService
  ) {
    this.resetForm = this.fb.group({
      newPassword: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', [Validators.required]]
    }, { validators: this.passwordMatchValidator });
  }

  passwordMatchValidator(g: FormGroup) {
    return g.get('newPassword')?.value === g.get('confirmPassword')?.value
      ? null : { 'mismatch': true };
  }

  onSubmit() {
    if (this.resetForm.valid) {
      this.loading = true;
      const password = this.resetForm.value.newPassword;

      // authService.completePasswordChange resets the flag on the backend
      this.authService.completePasswordChange(password).subscribe({
        next: () => {
          localStorage.removeItem('requires_password_change');
          this.router.navigate(['/dashboard']);
          this.toastService.showSuccess('Password successfully updated.');
        },
        error: (err: any) => {
          this.loading = false;
          this.toastService.showError(extractErrorMessage(err, 'Update failed'));
        }
      });
    }
  }

  onCancel() {
    // If they cancel a mandatory security action, they must be logged out
    this.authService.logout();
  }
}
