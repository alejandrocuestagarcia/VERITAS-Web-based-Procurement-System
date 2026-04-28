import {Component, Inject} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogRef} from "@angular/material/dialog";
import {FormControl, Validators} from "@angular/forms";

@Component({
  selector: 'app-reset-password-dialog',
  templateUrl: './reset-password-dialog.component.html',
  styleUrls: ['./reset-password-dialog.component.scss']
})
export class ResetPasswordDialogComponent {
  tempPassword = new FormControl('', [Validators.required, Validators.minLength(8)]);
  hidePassword = true;

  constructor(
    public dialogRef: MatDialogRef<ResetPasswordDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { user: string }
  ) {}

  onConfirm(): void {
    if (this.tempPassword.valid) {
      this.dialogRef.close(this.tempPassword.value);
    }
  }

  onCancel(): void {
    this.dialogRef.close(null);
  }
}
