import {Component, OnInit} from '@angular/core';
import {FormBuilder, FormGroup, Validators} from '@angular/forms';
import {Router} from '@angular/router';
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

  constructor(private fb: FormBuilder, private router: Router, private userService: UserModuleService, private teamService: TeamsModuleService) {
  }


  private initForm(): void {
    this.userForm = this.fb.group({
      name: ['', Validators.required],
      email: ['', Validators.required],
      //how to handle setting the password?
      password: [''],

      userRole: [null, Validators.required],
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
        password: "blabla",
        userRole: this.userForm.value.userRole,
        teamId: 3, // Explicitly setting dummy ID while functionality not implemented
        department: this.userForm.value.department,
        promoteToTeamLeader: this.userForm.value.promoteToTeamLeader

      };

      console.log(request);
      this.userService.createUser(request).subscribe({
        next: () => {
          this.router.navigate(['/users']);
        },
        error: err => {
          console.log(err);
        }
      })
    }
  }

  onCancel(): void {
    this.router.navigate(['/users']);
  }

}
