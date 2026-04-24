import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import {
  TeamsModuleService,
  UserCreationRequestDto,
  UserCreationRequestDtoDepartmentEnum,
  UserDtoRoleEnum,
  UserModuleService,
  TeamDto
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
  departments = Object.values(UserCreationRequestDtoDepartmentEnum);
  teams: TeamDto[] = [];

  constructor(private fb: FormBuilder,
              private router: Router,
              private userService: UserModuleService,
              private teamService: TeamsModuleService,
              private snackBar: MatSnackBar) {
  }

  ngOnInit(): void {
    this.initForm();
    this.loadTeams();
  }

  private loadTeams(): void {
    this.teamService.getAllTeams().subscribe({
      next: (teams) => {
        this.teams = teams;
      },
      error: () => {
        this.snackBar.open('Failed to load teams', 'Close', { duration: 3000 });
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
      department: [null, Validators.required],
      promoteToTeamLeader: [false],
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
        department: this.userForm.value.department,
        promoteToTeamLeader: this.userForm.value.promoteToTeamLeader

      };

      this.userService.createUser(request).subscribe({
        next: () => {
          this.loading = false;
          this.snackBar.open('User created successfully', 'Close', { duration: 3000 });
          this.router.navigate(['/users']);
        },
        error: err => {
          this.loading = false;
          this.snackBar.open('Failed to create user. Please try again.', 'Close', { duration: 5000 });
        }
      })
    } else {
      this.userForm.markAllAsTouched();
      this.snackBar.open('Please correct the highlighted errors before submitting.', 'Close', { duration: 4000 });
    }
  }

  onCancel(): void {
    this.router.navigate(['/users']);
  }
}
