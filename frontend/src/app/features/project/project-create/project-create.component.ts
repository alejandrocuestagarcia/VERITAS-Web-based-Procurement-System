import {Component, OnInit} from '@angular/core';
import {AbstractControl, FormBuilder, FormGroup, ValidationErrors, ValidatorFn, Validators} from "@angular/forms";
import {
  ProjectCreationDto, ProjectModuleService, TeamsModuleService, TeamDto
} from "../../../core/api";
import {Router} from "@angular/router";
import {MatSnackBar} from "@angular/material/snack-bar";

@Component({
  selector: 'app-project-create',
  templateUrl: './project-create.component.html',
  styleUrls: ['./project-create.component.scss']
})
export class ProjectCreateComponent implements OnInit {
  projectForm!: FormGroup;
  loading = false;

  teams: TeamDto[] = [];

  constructor(private fb: FormBuilder,
              private router: Router,
              private projectService: ProjectModuleService,
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
    this.projectForm = this.fb.group({
      name: ['', Validators.required],
      projectKey: ['', [Validators.required]],
      teamId: [null, Validators.required],
      startDate: [null, [Validators.required, this.dateNotInPastValidator()]],
      endDate: [null, [Validators.required, this.dateNotInPastValidator()]],
      budget: [0, [Validators.required, Validators.min(1)]]
    },
      {
        validators: this.dateRangeValidator()
      });
  }

  onSubmit(): void {
    if (this.projectForm.valid) {
      this.loading = true;

      const request: ProjectCreationDto = {
        name: this.projectForm.value.name,
        projectKey: this.projectForm.value.projectKey,
        teamId: this.projectForm.value.teamId,
        startDate: this.formatDate(this.projectForm.value.startDate),
        endDate: this.formatDate(this.projectForm.value.endDate),
        budget: this.projectForm.value.budget,

      };

      this.projectService.createProject(request).subscribe({
        next: () => {
          this.loading = false;
          this.snackBar.open('Project created successfully', 'Close', { duration: 3000 });
          this.router.navigate(['/projects']);
        },
        error: err => {
          this.loading = false;

          const message = err?.error && typeof err.error === 'object'
            ? Object.values(err.error).join(', ')
            : err?.error || 'Unknown error';

          this.snackBar.open('Failed: ' + message, 'Close', { duration: 5000 });
        }
      })
    } else {
      this.projectForm.markAllAsTouched();
      this.snackBar.open('Please correct the highlighted errors before submitting.', 'Close', { duration: 4000 });
    }
  }

  onCancel(): void {
    this.router.navigate(['/projects']);
  }

  private dateNotInPastValidator(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      if (!control.value) return null;

      const today = new Date();
      today.setHours(0, 0, 0, 0);

      const date = new Date(control.value);

      return date < today ? { pastDate: true } : null;
    };
  }

  private dateRangeValidator(): ValidatorFn {
    return (group: AbstractControl): ValidationErrors | null => {
      const start = group.get('startDate')?.value;
      const end = group.get('endDate')?.value;

      if (!start || !end) return null;

      return new Date(end) <= new Date(start) ? { invalidDateRange: true } : null;
    };
  }

  private formatDate(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');

    return `${year}-${month}-${day}`;
  }
}
