import {Component, OnInit} from '@angular/core';
import {FormBuilder, FormGroup, Validators} from "@angular/forms";
import {
  TeamDto,
  TeamsModuleService, UserCreationRequestDtoDepartmentEnum, UserDtoRoleEnum,
  UserModuleService
} from "../../../core/api";
import {ActivatedRoute, Router} from "@angular/router";
import { AuthService } from '../../../core/services/auth.service';
import {MatSnackBar} from "@angular/material/snack-bar";
import {ResetPasswordDialogComponent} from "../../login/reset-password-dialog/reset-password-dialog.component";
import {MatDialog} from "@angular/material/dialog";

@Component({
  selector: 'app-user-edit',
  templateUrl: './user-edit.component.html',
  styleUrls: ['./user-edit.component.scss']
})
export class UserEditComponent implements OnInit {
  userForm!: FormGroup;
  loading = false;
  userId!: number;

  roles = Object.values(UserDtoRoleEnum);
  departments = Object.values(UserCreationRequestDtoDepartmentEnum);
  teams: TeamDto[] = [];

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private route: ActivatedRoute,
    private userService: UserModuleService,
    private teamService: TeamsModuleService,
    private authService: AuthService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.userId = Number(this.route.snapshot.paramMap.get('id'));
    this.initForm();
    this.loadTeams();
    this.loadUser();
  }

  private initForm(): void {
    this.userForm = this.fb.group({
      name: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      role: [null, Validators.required],
      teamId: [null],
      department: [null, Validators.required],
      isTeamLeader: [false],
    });

    this.userForm.get('isTeamLeader')?.valueChanges.subscribe(isLeader => {
      const teamControl = this.userForm.get('teamId');
      if (isLeader) {
        teamControl?.disable();
      } else {
        teamControl?.enable();
      }
    });

    this.userForm.get('teamId')?.valueChanges.subscribe(teamId => {
      const leaderControl = this.userForm.get('isTeamLeader');
      if (!teamId) {
        leaderControl?.disable();
      } else {
        leaderControl?.enable();
      }
    });
  }

  private loadTeams(): void {
    this.teamService.getAllTeams().subscribe({
      next: (teams) => this.teams = teams,
      error: () => this.snackBar.open('Failed to load teams.', 'Close', { duration: 3000 })
    });
  }

  private loadUser(): void {
    this.userService.getUserByIdForEdit(this.userId).subscribe({
      next: (user) => {
        this.userForm.patchValue({
          name: user.name,
          email: user.email,
          role: user.role,
          teamId: user.teamId,
          department: user.department,
          isTeamLeader: user.isTeamLeader ?? false,
        });

        if (user.isTeamLeader) {
          this.userForm.get('teamId')?.disable();
        }

        if (user.role === 'ADMINISTRATOR') {
          this.userForm.get('role')?.disable();
        }

      },
      error: () => {
        this.snackBar.open('Failed to load user.', 'Close', { duration: 3000 });
        this.router.navigate(['/users']);
      }
    });
  }

  onSubmit(): void {
    if (this.userForm.valid) {
      this.loading = true;

      const formValue = this.userForm.getRawValue();

      const request = {
        name: formValue.name,
        email: formValue.email,
        role: formValue.role,
        teamId: formValue.teamId,
        department: formValue.department,
        isTeamLeader: formValue.isTeamLeader,
      };

      this.userService.editUser(this.userId, request).subscribe({
        next: () => {
          this.loading = false;
          this.snackBar.open('User updated successfully', 'Close', { duration: 3000 });
          this.router.navigate(['/users']);
        },
        error: (err) => {
          this.loading = false;
          const message = err.status === 409 ? 'Email is already in use.'
            : err.status === 400 ? err.error : 'Failed to update user. Please try again.';
          this.snackBar.open(message, 'Close', { duration: 5000 });
        }
      });
    } else {
      this.userForm.markAllAsTouched();
      this.snackBar.open('Please correct the highlighted errors before submitting.', 'Close', { duration: 4000 });
    }
  }

  onCancel(): void {
    this.router.navigate(['/users']);
  }

  onResetPassword(): void {
    const dialogRef = this.dialog.open(ResetPasswordDialogComponent, {
      width: '600px',
      data: { user: this.userForm.get('email')?.value }
    });

    dialogRef.afterClosed().subscribe((tempPassword: string | null) => {
      if (!tempPassword) return;

      this.authService.adminResetPassword(this.userId, tempPassword).subscribe({
        next: () => {
          this.snackBar.open(
            'Password reset successful. Account flagged for mandatory change.',
            'Close',
            { duration: 5000, panelClass: ['success-snackbar'] }
          );
        },
        error: (err: any) => {
          this.snackBar.open(err.error?.message || 'Failed to reset password', 'Close');
        }
      });
    });
  }
}
