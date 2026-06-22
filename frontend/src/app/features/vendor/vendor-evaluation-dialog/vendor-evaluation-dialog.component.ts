import { Component, Inject } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { VendorModuleService, VendorRatingDto } from '../../../core/api';
import { ToastService } from '../../../core/services/toast.service';
import { extractErrorMessage } from '../../../shared/utils/error-utils';

export interface VendorEvaluationDialogData {
  vendorId: number;
  requestId: number;
  vendorName: string;
}

@Component({
  selector: 'app-vendor-evaluation-dialog',
  templateUrl: './vendor-evaluation-dialog.component.html',
})
export class VendorEvaluationDialogComponent {
  public form: FormGroup;
  public loading: boolean = false;

  constructor(
    private fb: FormBuilder,
    private dialogRef: MatDialogRef<VendorEvaluationDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: VendorEvaluationDialogData,
    private vendorService: VendorModuleService,
    private toastService: ToastService
  ) {
    this.form = this.fb.group({
      communicationScore: [5, [Validators.required, Validators.min(0), Validators.max(10)]],
      deliveryScore: [5, [Validators.required, Validators.min(0), Validators.max(10)]],
      qualityScore: [5, [Validators.required, Validators.min(0), Validators.max(10)]]
    });
  }

  public onSubmit(): void {
    if (this.form.invalid) return;

    this.loading = true;
    const ratingData: VendorRatingDto = {
      communicationScore: this.form.value.communicationScore,
      deliveryScore: this.form.value.deliveryScore,
      qualityScore: this.form.value.qualityScore
    };

    this.vendorService.rateVendor(this.data.vendorId, this.data.requestId, ratingData).subscribe({
      next: () => {
        this.toastService.showSuccess('Vendor evaluated successfully');
        this.loading = false;
        this.dialogRef.close(true);
      },
      error: (err) => {
        this.loading = false;
        this.toastService.showError(extractErrorMessage(err, 'Failed to evaluate vendor'));
      }
    });
  }

  public onCancel(): void {
    this.dialogRef.close(false);
  }
}
