import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { ToastService } from '../../../core/services/toast.service';
import { extractErrorMessage } from '../../../shared/utils/error-utils';
import {
  TeamsModuleService,
  UserCreationRequestDto,
  UserDtoRoleEnum,
  UserModuleService,
  TeamDto,
  DepartmentsModuleService,
  DepartmentDto
} from "../../../core/api";

@Component({
  selector: 'app-user-create',
  templateUrl: './user-create.component.html',
  styleUrls: ['./user-create.component.scss']
})
export class UserCreateComponent implements OnInit {
  userForm!: FormGroup;
  loading = false;

  roles = Object.values(UserDtoRoleEnum);
  teams: TeamDto[] = [];
  departments: DepartmentDto[] = [];

  constructor(private fb: FormBuilder,
    private router: Router,
    private userService: UserModuleService,
    private teamService: TeamsModuleService,
    private departmentService: DepartmentsModuleService,
    private toastService: ToastService) {
  }

  ngOnInit(): void {
    this.initForm();
    this.loadTeams();
    this.loadDepartments();
  }

  private loadTeams(): void {
    this.teamService.getAllTeams().subscribe({
      next: (teams) => {
        this.teams = teams;
        this.updateLeaderToggleState();
      },
      error: () => {
        this.toastService.showError('Failed to load teams');
      }
    });
  }

  private loadDepartments(): void {
    this.departmentService.getAllDepartments().subscribe({
      next: (departments) => {
        this.departments = departments;
      },
      error: () => {
        this.toastService.showError('Failed to load departments');
      }
    });
  }

  private initForm(): void {
    this.userForm = this.fb.group({
      name: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      password: ['', Validators.required],
      role: [null, Validators.required],
      teamId: [null],
      departmentId: [null],
      promoteToTeamLeader: [false],
    });

    // Handle conditional requirements/visibility
    this.userForm.get('role')?.valueChanges.subscribe(role => {
      const teamId = this.userForm.get('teamId');
      const deptId = this.userForm.get('departmentId');
      const promote = this.userForm.get('promoteToTeamLeader');

      if (role === UserDtoRoleEnum.Requester) {
        teamId?.setValidators(Validators.required);
        deptId?.clearValidators();
        deptId?.setValue(null);
      } else if (role === UserDtoRoleEnum.ProcurementOfficer) {
        deptId?.setValidators(Validators.required);
        teamId?.clearValidators();
        teamId?.setValue(null);
        promote?.setValue(false);
      } else {
        teamId?.clearValidators();
        deptId?.clearValidators();
        teamId?.setValue(null);
        deptId?.setValue(null);
        promote?.setValue(false);
      }
      teamId?.updateValueAndValidity();
      deptId?.updateValueAndValidity();
    });

    this.userForm.get('teamId')?.valueChanges.subscribe(() => {
      this.updateLeaderToggleState();
    });

    this.updateLeaderToggleState();
  }

  get leaderConflictMessage(): string | null {
    const teamId = this.userForm?.get('teamId')?.value;
    if (!teamId) {
      return null;
    }

    const team = this.teams.find((candidate) => candidate.id === teamId);
    if (team?.leaderId) {
      return 'This team already has a leader. Remove the current leader before assigning a new one.';
    }

    return null;
  }

  private updateLeaderToggleState(): void {
    const promoteControl = this.userForm.get('promoteToTeamLeader');
    const teamControl = this.userForm.get('teamId');

    if (!promoteControl || !teamControl) {
      return;
    }

    const teamId = teamControl.value;
    if (!teamId) {
      promoteControl.disable({ emitEvent: false });
      promoteControl.setValue(false, { emitEvent: false });
      return;
    }

    const team = this.teams.find((candidate) => candidate.id === teamId);
    const hasLeader = !!team?.leaderId;

    if (hasLeader) {
      promoteControl.disable({ emitEvent: false });
      promoteControl.setValue(false, { emitEvent: false });
      return;
    }

    promoteControl.enable({ emitEvent: false });
  }

  onSubmit(): void {
    if (this.userForm.valid) {
      this.loading = true;

      const formValue = this.userForm.getRawValue();

      const request: UserCreationRequestDto = {
        name: formValue.name,
        email: formValue.email,
        password: formValue.password,
        role: formValue.role,
        teamId: formValue.teamId,
        departmentId: formValue.departmentId,
        promoteToTeamLeader: formValue.promoteToTeamLeader || false
      };

      this.userService.createUser(request).subscribe({
        next: () => {
          this.loading = false;
          this.toastService.showSuccess('User created successfully');
          this.router.navigate(['/users']);
        },
        error: err => {
          this.loading = false;
          this.toastService.showError(extractErrorMessage(err, 'Failed to create user'));
        }
      })
    } else {
      this.userForm.markAllAsTouched();
      this.toastService.showError('Please correct the highlighted errors before submitting.');
    }
  }

  onCancel(): void {
    this.router.navigate(['/users']);
  }
}
