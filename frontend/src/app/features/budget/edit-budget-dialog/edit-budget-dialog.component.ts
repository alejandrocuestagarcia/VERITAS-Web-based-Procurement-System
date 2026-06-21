import { Component, Inject, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { FinancialGovernanceModuleService } from '../../../core/api';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-edit-budget-dialog',
  templateUrl: './edit-budget-dialog.component.html'
})
export class EditBudgetDialogComponent implements OnInit {
  budgetForm!: FormGroup;
  loading = false;

  constructor(
    private fb: FormBuilder,
    private financialService: FinancialGovernanceModuleService,
    private toastService: ToastService,
    private dialogRef: MatDialogRef<EditBudgetDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { totalBudget: number, safetyBuffer: number, exists: boolean }
  ) {}

  ngOnInit(): void {
    this.budgetForm = this.fb.group({
      totalBudget: [this.data.totalBudget || 0, [Validators.required, Validators.min(0)]],
      safetyBuffer: [this.data.safetyBuffer || 0, [Validators.required, Validators.min(0), Validators.max(100)]]
    });
  }

  onSubmit(): void {
    if (this.budgetForm.invalid) {
      return;
    }

    this.loading = true;
    const formValue = this.budgetForm.value;

    const budgetDto = {
      totalAmount: formValue.totalBudget,
      safetyBuffer: formValue.safetyBuffer
    };

    const action$ = this.data.exists
      ? this.financialService.editBudget(budgetDto)
      : this.financialService.createBudget(budgetDto);

    action$.subscribe({
      next: (res) => {
        this.toastService.showSuccess('Global budget updated successfully.');
        this.dialogRef.close(true);
      },
      error: (err) => {
        this.loading = false;
        if (err?.error instanceof Blob) {
          err.error.text().then((text: string) => {
            this.toastService.showError(text || 'Failed to update global budget.');
          });
        } else {
          const errorMsg = typeof err?.error === 'string' && err.error.length > 0
            ? err.error
            : (err?.error?.message || err?.message || 'Failed to update global budget.');
          this.toastService.showError(errorMsg);
        }
      }
    });
  }

  onCancel(): void {
    this.dialogRef.close(false);
  }
}
