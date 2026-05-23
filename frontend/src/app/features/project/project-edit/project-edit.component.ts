import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators} from "@angular/forms";
import { ActivatedRoute, Router } from "@angular/router";
import { ToastService } from '../../../core/services/toast.service';
import {ProjectModuleService, TeamDto, TeamsModuleService} from "../../../core/api";

@Component({
  selector: 'app-project-edit',
  templateUrl: './project-edit.component.html',
  styles: [
  ]
})
export class ProjectEditComponent implements OnInit{
  projectForm!: FormGroup;
  loading = false;
  projectId!: number;
  teams: TeamDto[] = [];

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private route: ActivatedRoute,
    private projectService: ProjectModuleService,
    private teamService: TeamsModuleService,
    private toastService: ToastService
  ) { }

  ngOnInit(): void {
    this.projectId = Number(this.route.snapshot.paramMap.get('id'));
    this.initForm();
    this.loadProject();
    this.loadTeams();

  }

  private initForm(): void {
    this.projectForm = this.fb.group({
      name: ['', Validators.required],
      teamId: [null, Validators.required],
      budget: [null, [Validators.required, Validators.min(1)]],
      startDate: [null, Validators.required],
      endDate: [null, Validators.required],
    });
  }

  private loadTeams(): void {
    this.teamService.getAllTeams().subscribe({
      next: (teams) => {
        this.teams = teams;

        this.loadProject();
      },
      error: () => this.toastService.showError('Failed to load organizational team selections.')
    });
  }

  private loadProject(): void {
    this.projectService.getProject(this.projectId).subscribe({
      next: (project) => {
        this.projectForm.patchValue({
          name: project.name,
          teamId: project.teamId,
          budget: project.budget,
          startDate: project.startDate,
          endDate: project.endDate,
        });
      },
      error: () => {
        this.toastService.showError('Failed to load project details.');
        this.router.navigate(['/projects']);
      }
    });
  }

  onSubmit(): void {
    if (this.projectForm.valid) {
      this.loading = true;
      const formValue = this.projectForm.getRawValue();

      const request = {
        name: formValue.name,
        teamId: formValue.teamId,
        budget: formValue.budget,
        startDate: formValue.startDate,
        endDate: formValue.endDate,
      };

      this.projectService.editProject(this.projectId, request).subscribe({
        next: () => {
          this.loading = false;
          this.toastService.showSuccess('Project updated successfully');
          this.router.navigate(['/projects']);
        },
        error: (err) => {
          this.loading = false;
          console.log(err)
          if (err.status === 400) {
            if (err.error instanceof Blob) {
              err.error.text().then((text: string) => {
                this.toastService.showError(text);
              });
            } else {
              this.toastService.showError(err.error);
            }
          } else {
            this.toastService.showError('Failed to update project. Please try again.');
          }
        }
      });
    } else {
      this.projectForm.markAllAsTouched();
      this.toastService.showError('Please correct the highlighted errors before submitting.');
    }
  }

  onCancel(): void {
    this.router.navigate(['/projects']);
  }
}
