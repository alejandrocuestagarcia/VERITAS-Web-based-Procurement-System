import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from "@angular/forms";
import {
  TeamDto,
  TeamsModuleService,
  UserDtoRoleEnum,
  UserModuleService,
  DepartmentsModuleService,
  DepartmentDto
} from "../../../core/api";
import { ActivatedRoute, Router } from "@angular/router";
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';
import { ResetPasswordDialogComponent } from "../../login/reset-password-dialog/reset-password-dialog.component";
import { MatDialog } from "@angular/material/dialog";
import { extractErrorMessage } from '../../../shared/error-utils';

@Component({
  selector: 'app-user-edit',
  templateUrl: './user-edit.component.html',
  styleUrls: ['./user-edit.component.scss']
})
export class UserEditComponent implements OnInit {
  userForm!: FormGroup;
  loading = false;
  userId!: number;

  get isSelfAdmin(): boolean {
    return this.userId === this.authService.getUserId() && this.userForm?.get('role')?.value === 'ADMINISTRATOR';
  }

  roles = Object.values(UserDtoRoleEnum);
  teams: TeamDto[] = [];
  departments: DepartmentDto[] = [];

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private route: ActivatedRoute,
    private userService: UserModuleService,
    private teamService: TeamsModuleService,
    private departmentService: DepartmentsModuleService,
    private authService: AuthService,
    private dialog: MatDialog,
    private toastService: ToastService
  ) { }

  ngOnInit(): void {
    this.userId = Number(this.route.snapshot.paramMap.get('id'));
    this.initForm();
    this.loadTeams();
    this.loadDepartments();
    this.loadUser();
  }

  private initForm(): void {
    this.userForm = this.fb.group({
      name: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      role: [null, Validators.required],
      teamId: [null],
      departmentId: [null],
      isTeamLeader: [false],
    });

    this.userForm.get('role')?.valueChanges.subscribe(role => {
      this.handleRoleChange(role);
    });

    this.userForm.get('isTeamLeader')?.valueChanges.subscribe(isLeader => {
      const teamControl = this.userForm.get('teamId');
      if (isLeader) {
        teamControl?.disable({ emitEvent: false });
      } else {
        teamControl?.enable({ emitEvent: false });
      }
    });

    this.userForm.get('teamId')?.valueChanges.subscribe(() => {
      this.updateLeaderToggleState();
    });
  }

  private handleRoleChange(role: UserDtoRoleEnum): void {
    const teamId = this.userForm.get('teamId');
    const deptId = this.userForm.get('departmentId');
    const leader = this.userForm.get('isTeamLeader');

    if (role === UserDtoRoleEnum.Requester) {
      teamId?.setValidators(Validators.required);
      deptId?.clearValidators();
      deptId?.setValue(null);
    } else if (role === UserDtoRoleEnum.ProcurementOfficer) {
      deptId?.setValidators(Validators.required);
      teamId?.clearValidators();
      teamId?.setValue(null);
      leader?.setValue(false);
    } else {
      teamId?.clearValidators();
      deptId?.clearValidators();
      teamId?.setValue(null);
      deptId?.setValue(null);
      leader?.setValue(false);
    }
    teamId?.updateValueAndValidity();
    deptId?.updateValueAndValidity();
  }

  private loadTeams(): void {
    this.teamService.getAllTeams().subscribe({
      next: (teams) => {
        this.teams = teams;
        this.updateLeaderToggleState();
      },
      error: () => this.toastService.showError('Failed to load teams.')
    });
  }

  private loadDepartments(): void {
    this.departmentService.getAllDepartments().subscribe({
      next: (departments) => {
        this.departments = departments;
      },
      error: () => this.toastService.showError('Failed to load departments.')
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
          departmentId: user.departmentId,
          isTeamLeader: user.isTeamLeader ?? false,
        });

        if (user.isTeamLeader) {
          this.userForm.get('teamId')?.disable();
        }

        if (this.isSelfAdmin) {
          this.userForm.get('role')?.disable();
        }


        this.handleRoleChange(user.role as unknown as UserDtoRoleEnum);
        this.updateLeaderToggleState();
      },
      error: () => {
        this.toastService.showError('Failed to load user.');
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
        departmentId: formValue.departmentId,
        isTeamLeader: formValue.isTeamLeader,
      };

      this.userService.editUser(this.userId, request).subscribe({
        next: () => {
          this.loading = false;
          this.toastService.showSuccess('User updated successfully');
          this.router.navigate(['/users']);
        },
        error: (err) => {
          this.loading = false;
          this.toastService.showError(extractErrorMessage(err, 'Failed to update user'));
        }
      });
    } else {
      this.userForm.markAllAsTouched();
      this.toastService.showError('Please correct the highlighted errors before submitting.');
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
          this.toastService.showSuccess(
            'Password reset successful. Account flagged for mandatory change.'
          );
        },
        error: (err: any) => {
          this.toastService.showError(extractErrorMessage(err, 'Failed to reset password'));
        }
      });
    });
  }

  get leaderConflictMessage(): string | null {
    const teamId = this.userForm?.get('teamId')?.value;
    if (!teamId) {
      return null;
    }

    const team = this.teams.find((candidate) => candidate.id === teamId);
    if (team?.leaderId && team.leaderId !== this.userId) {
      return 'This team already has a leader. Remove the current leader before assigning a new one.';
    }

    return null;
  }

  private updateLeaderToggleState(): void {
    const leaderControl = this.userForm.get('isTeamLeader');
    const teamControl = this.userForm.get('teamId');

    if (!leaderControl || !teamControl) {
      return;
    }

    const teamId = teamControl.value;
    if (!teamId) {
      leaderControl.disable({ emitEvent: false });
      leaderControl.setValue(false, { emitEvent: false });
      return;
    }

    const team = this.teams.find((candidate) => candidate.id === teamId);
    const hasDifferentLeader = team?.leaderId && team.leaderId !== this.userId;

    if (hasDifferentLeader) {
      leaderControl.disable({ emitEvent: false });
      leaderControl.setValue(false, { emitEvent: false });
      return;
    }

    leaderControl.enable({ emitEvent: false });
  }
}
