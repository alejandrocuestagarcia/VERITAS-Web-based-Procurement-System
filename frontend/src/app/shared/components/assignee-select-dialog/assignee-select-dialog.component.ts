import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { UserDto } from 'src/app/core/api';

export interface AssigneeSelectDialogData {
  role: string;
  users: UserDto[];
}

@Component({
  selector: 'app-assignee-select-dialog',
  templateUrl: './assignee-select-dialog.component.html'
})
export class AssigneeSelectDialogComponent {
  selectedAssigneeId: number = -1;

  constructor(
    public dialogRef: MatDialogRef<AssigneeSelectDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: AssigneeSelectDialogData
  ) {}

  onCancel(): void {
    this.dialogRef.close();
  }

  onConfirm(): void {
    // If selectedAssigneeId is -1, it means global pool (null)
    this.dialogRef.close(this.selectedAssigneeId === -1 ? null : this.selectedAssigneeId);
  }

  formatRole(role: string): string {
    if (!role) return '';
    return role.replace(/_/g, ' ').replace(/\w\S*/g, txt =>
      txt.charAt(0).toUpperCase() + txt.substring(1).toLowerCase()
    );
  }
}
