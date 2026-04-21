import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import {
  TeamsModuleService,
  UserCreationRequestDto,
  UserCreationRequestDtoDepartmentEnum,
  UserDtoRoleEnum,
  UserModuleService
} from "../../core/api";

@Component({
  selector: 'app-user-create',
  templateUrl: './user-create.component.html',
  styleUrls: ['./user-create.component.scss']
})
export class UserCreateComponent implements OnInit {
  ngOnInit(): void {
    this.initForm();
  }


  userForm!: FormGroup;

  roles = Object.values(UserDtoRoleEnum);
  teams$: any = [];
  departments$ = Object.values(UserCreationRequestDtoDepartmentEnum);

  constructor(private fb: FormBuilder, private router: Router, private userService: UserModuleService, private teamService: TeamsModuleService, private snackBar: MatSnackBar) {
  }


  private initForm(): void {
    this.userForm = this.fb.group({
      name: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      password: ['', Validators.required],

      role: [null, Validators.required],
      // teamId: [null, Validators.required],
      teamId: [null],
      department: [null, Validators.required],

      promoteToTeamLeader: [false],
    })
  }

  onSubmit(): void {


    if (this.userForm.valid) {
      const request: UserCreationRequestDto = {
        name: this.userForm.value.name,
        email: this.userForm.value.email,
        password: this.userForm.value.password,
        role: this.userForm.value.role,
        teamId: 3, // Explicitly setting dummy ID while functionality not implemented
        department: this.userForm.value.department,
        promoteToTeamLeader: this.userForm.value.promoteToTeamLeader

      };

      this.userService.createUser(request).subscribe({
        next: () => {
          this.snackBar.open('User created successfully', 'Close', { duration: 3000 });
          this.router.navigate(['/users']);
        },
        error: err => {
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
