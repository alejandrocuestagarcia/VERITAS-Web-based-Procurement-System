import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { ToastService } from '../../../core/services/toast.service';
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
  }

  onSubmit(): void {
    if (this.userForm.valid) {
      this.loading = true;

      const request: UserCreationRequestDto = {
        name: this.userForm.value.name,
        email: this.userForm.value.email,
        password: this.userForm.value.password,
        role: this.userForm.value.role,
        teamId: this.userForm.value.teamId,
        departmentId: this.userForm.value.departmentId,
        promoteToTeamLeader: this.userForm.value.promoteToTeamLeader
      };

      this.userService.createUser(request).subscribe({
        next: () => {
          this.loading = false;
          this.toastService.showSuccess('User created successfully');
          this.router.navigate(['/users']);
        },
        error: err => {
          this.loading = false;
          this.toastService.showError('Failed to create user. Please try again.');
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
