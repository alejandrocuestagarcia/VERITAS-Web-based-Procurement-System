import { Component, OnInit } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ToastService } from '../../../core/services/toast.service';
import {
  DepartmentCreateDto,
  DepartmentsModuleService
} from '../../../core/api';

@Component({
  selector: 'app-department-create',
  templateUrl: './department-create.component.html',
  styleUrls: []
})
export class DepartmentCreateComponent implements OnInit {
  departmentForm!: FormGroup;
  isEditMode = false;
  departmentId!: number;

  submitting = false;
  error: string | null = null;

  constructor(
    private departmentsService: DepartmentsModuleService,
    private router: Router,
    private route: ActivatedRoute,
    private toastService: ToastService,
    private fb: FormBuilder
  ) { }

  ngOnInit(): void {
    this.departmentForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(120)]],
      budget: [null, [Validators.min(0)]]
    });

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.isEditMode = true;
      this.departmentId = Number(idParam);
      this.loadDepartment();
    }
  }

  cancel(): void {
    this.router.navigate(['/departments']);
  }

  submit(): void {
    this.error = null;

    if (this.departmentForm.invalid) {
      this.error = 'Please complete all required fields correctly.';
      this.departmentForm.markAllAsTouched();
      return;
    }

    this.submitting = true;

    const payload: DepartmentCreateDto = {
      name: this.departmentForm.value.name.trim(),
      budget: this.departmentForm.value.budget
    };

    if (this.isEditMode) {
      this.departmentsService.updateDepartment(this.departmentId, payload).subscribe({
        next: () => {
          this.submitting = false;
          this.toastService.showSuccess('Department updated successfully.');
          this.router.navigate(['/departments']);
        },
        error: (err) => {
          this.submitting = false;
          this.error = this.extractErrorMessage(err, 'Failed to update department.');
        }
      });
    } else {
      this.departmentsService.createDepartment(payload).subscribe({
        next: () => {
          this.submitting = false;
          this.toastService.showSuccess('Department created successfully.');
          this.router.navigate(['/departments']);
        },
        error: (err) => {
          this.submitting = false;
          this.error = this.extractErrorMessage(err, 'Failed to create department.');
        }
      });
    }
  }

  private loadDepartment(): void {
    this.departmentsService.getDepartment(this.departmentId).subscribe({
      next: (dept) => {
        this.departmentForm.patchValue({
          name: dept.name,
          budget: dept.budget
        });
      },
      error: () => {
        this.toastService.showError('Failed to load department details.');
        this.router.navigate(['/departments']);
      }
    });
  }

  private extractErrorMessage(err: any, fallback: string): string {
    if (typeof err?.error === 'string' && err.error.length > 0) {
      return err.error;
    }

    const backendMessage = err?.error?.message;
    if (backendMessage && typeof backendMessage === 'string') {
      return backendMessage;
    }

    return fallback;
  }
}
