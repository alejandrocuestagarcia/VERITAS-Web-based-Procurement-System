import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { VendorModuleService } from '../../../core/api';
import { VendorDto } from '../../../core/api';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ToastService } from '../../../core/services/toast.service';
import { extractErrorMessage } from '../../../shared/error-utils';

@Component({
  selector: 'app-vendor-create',
  templateUrl: './vendor-create.component.html',
  styleUrls: ['./vendor-create.component.scss']
})
export class VendorCreateComponent implements OnInit {
  vendorForm!: FormGroup;
  loading = false;

  constructor(
    private fb: FormBuilder,
    private vendorService: VendorModuleService,
    private router: Router,
    private toastService: ToastService,
  ) { }

  ngOnInit(): void {
    this.initForm();
  }

  private initForm(): void {
    this.vendorForm = this.fb.group({
      name: ['', Validators.required],
      taxId: ['', Validators.required],
      description: ['', Validators.required],
      primaryContactName: [''],
      primaryContactEmail: ['', Validators.email],
    });
  }

  onSubmit(): void {
    if (this.vendorForm.valid) {
      this.loading = true;

      const request: VendorDto = {
        vendorName: this.vendorForm.value.name,
        taxId: this.vendorForm.value.taxId,
        description: this.vendorForm.value.description,
        primaryContactName: this.vendorForm.value.primaryContactName,
        primaryContactEmail: this.vendorForm.value.primaryContactEmail,
        communicationScore: 0,
        deliveryScore: 0,
        qualityScore: 0,
        overallScore: 0
      };

      this.vendorService.createVendor(request).subscribe({
        next: () => {
          this.loading = false;
          this.toastService.showSuccess('Vendor added successfully');
          this.router.navigate(['/vendors']);
        },
        error: err => {
          this.loading = false;
          this.toastService.showError(extractErrorMessage(err, 'Failed to add vendor'));
        }
      })
    } else {
      this.vendorForm.markAllAsTouched();
      this.toastService.showError('Please correct the highlighted errors before submitting.');
    }
  }

  onCancel(): void {
    this.router.navigate(['/vendors']);
  }
}
