import { Component, Inject, Optional } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";

@Component({
  selector: 'app-reject-dialog',
  templateUrl: './reject-dialog.component.html',
  styles: [
  ]
})
export class RejectDialogComponent {
  reason: string = '';
  revisionRequired: boolean = false;

  constructor(
    public dialogRef: MatDialogRef<RejectDialogComponent>,
    @Optional() @Inject(MAT_DIALOG_DATA) public data: { isRevert?: boolean } | null
  ) {}

  onCancel(): void {
    this.dialogRef.close();
  }

  onConfirm(): void {
    if (this.reason.trim()) {
      this.dialogRef.close({
        reason: this.reason.trim(),
        revisionRequired: this.revisionRequired
      });
    }
  }
}
