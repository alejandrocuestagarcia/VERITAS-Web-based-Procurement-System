import { Component, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { UserDto, RequisitionDto } from '../../../core/api';
import { FormControl } from '@angular/forms';
import { Observable } from 'rxjs';
import { map, startWith } from 'rxjs/operators';

export interface UserDeletionDialogData {
  user: UserDto;
  pendingRequests: RequisitionDto[];
  fallbackUsers: UserDto[];
}

@Component({
  selector: 'app-user-deletion-dialog',
  templateUrl: './user-deletion-dialog.component.html',
})
export class UserDeletionDialogComponent implements OnInit {
  selectedFallbackUserId?: number;
  userSearchControl = new FormControl('');
  filteredUsers!: Observable<UserDto[]>;

  constructor(
    public dialogRef: MatDialogRef<UserDeletionDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: UserDeletionDialogData
  ) { }

  ngOnInit() {
    this.filteredUsers = this.userSearchControl.valueChanges.pipe(
      startWith(''),
      map(value => {
        if (typeof value === 'string') {
          this.selectedFallbackUserId = undefined;
        }
        const name = typeof value === 'string' ? value : (value as any)?.name;
        return name ? this.usersFilter(name as string) : this.data.fallbackUsers.slice();
      }),
    );
  }

  private usersFilter(name: string): UserDto[] {
    const filterValue = name.toLowerCase();
    return this.data.fallbackUsers.filter(user =>
      user.name?.toLowerCase().includes(filterValue) ||
      user.email?.toLowerCase().includes(filterValue)
    );
  }

  formatUserSelection(user: UserDto): string {
    return user && user.name ? user.name : '';
  }

  onUserSelected(event: any) {
    this.selectedFallbackUserId = event.option.value.id;
  }

  onCancel(): void {
    this.dialogRef.close();
  }

  onConfirm(): void {
    if (this.data.pendingRequests.length > 0 && !this.selectedFallbackUserId) {
      return;
    }
    this.dialogRef.close(this.selectedFallbackUserId || null);
  }
}
