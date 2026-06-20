import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { VendorEditDto, VendorModuleService } from '../../../core/api';
import { ToastService } from '../../../core/services/toast.service';
import { extractErrorMessage } from '../../../shared/utils/error-utils';

@Component({
  selector: 'app-vendor-edit',
  templateUrl: './vendor-edit.component.html',
  styleUrls: ['./vendor-edit.component.scss']
})
export class VendorEditComponent implements OnInit {
  vendorForm!: FormGroup;
  loading = false;
  vendorId!: number;

  constructor(
    private fb: FormBuilder,
    private vendorService: VendorModuleService,
    private route: ActivatedRoute,
    private router: Router,
    private toastService: ToastService
  ) {}

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    this.vendorId = idParam ? Number(idParam) : NaN;
    this.initForm();

    if (!this.vendorId) {
      this.toastService.showError('Invalid vendor ID.');
      this.router.navigate(['/vendors']);
      return;
    }

    this.loadVendor();
  }

  private initForm(): void {
    this.vendorForm = this.fb.group({
      vendorName: ['', [Validators.required, Validators.maxLength(120)]],
      taxId: ['', [Validators.required, Validators.maxLength(60)]],
      description: ['', [Validators.required, Validators.maxLength(3000)]],
      primaryContactName: ['', Validators.maxLength(120)],
      primaryContactEmail: ['', [Validators.email, Validators.maxLength(120)]],
    });
  }

  private loadVendor(): void {
    this.vendorService.getVendor(this.vendorId).subscribe({
      next: (vendor) => {
        this.vendorForm.patchValue({
          vendorName: vendor.vendorName,
          taxId: vendor.taxId,
          description: vendor.description,
          primaryContactName: vendor.primaryContactName ?? '',
          primaryContactEmail: vendor.primaryContactEmail ?? '',
        });
      },
      error: (err) => {
        this.toastService.showError(extractErrorMessage(err, 'Failed to load vendor details'));
        this.router.navigate(['/vendors']);
      }
    });
  }

  onSubmit(): void {
    if (this.vendorForm.valid) {
      this.loading = true;
      const formValue = this.vendorForm.value;

      const request: VendorEditDto = {
        vendorName: formValue.vendorName?.trim(),
        taxId: formValue.taxId?.trim(),
        description: formValue.description?.trim(),
        primaryContactName: this.normalizeOptional(formValue.primaryContactName),
        primaryContactEmail: this.normalizeOptional(formValue.primaryContactEmail),
      };

      this.vendorService.editVendor(this.vendorId, request).subscribe({
        next: () => {
          this.loading = false;
          this.toastService.showSuccess('Vendor updated successfully');
          this.router.navigate(['/vendors']);
        },
        error: (err) => {
          this.loading = false;
          this.toastService.showError(extractErrorMessage(err, 'Failed to update vendor'));
        }
      });
    } else {
      this.vendorForm.markAllAsTouched();
      this.toastService.showError('Please correct the highlighted errors before submitting.');
    }
  }

  onCancel(): void {
    this.router.navigate(['/vendors']);
  }

  private normalizeOptional(value: string | null | undefined): string | undefined {
    if (!value) {
      return undefined;
    }

    const trimmed = value.trim();
    return trimmed.length ? trimmed : undefined;
  }
}
