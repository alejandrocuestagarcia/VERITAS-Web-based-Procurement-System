import { Component, Inject, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { FinancialGovernanceModuleService } from '../../../core/api';
import { ToastService } from '../../../core/services/toast.service';
import { extractErrorMessage } from '../../../shared/utils/error-utils';

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
        this.toastService.showError(extractErrorMessage(err, 'Failed to update global budget'));
      }
    });
  }

  onCancel(): void {
    this.dialogRef.close(false);
  }
}
