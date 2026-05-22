import { Component } from '@angular/core';
import {MatDialogRef} from "@angular/material/dialog";

@Component({
  selector: 'app-reject-dialog',
  templateUrl: './reject-dialog.component.html',
  styles: [
  ]
})
export class RejectDialogComponent {
  reason: string = '';

  constructor(public dialogRef: MatDialogRef<RejectDialogComponent>) {}

  onCancel(): void {
    this.dialogRef.close();
  }

  onConfirm(): void {
    if (this.reason.trim()) {
      this.dialogRef.close(this.reason.trim());
    }
  }
}
