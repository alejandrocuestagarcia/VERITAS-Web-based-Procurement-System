import { Component, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { VendorModuleService } from '../../core/api/api/vendorModule.service';
import { VendorDto } from '../../core/api/model/vendorDto';
import { NgForm } from '@angular/forms';

@Component({
  selector: 'app-vendor-create',
  templateUrl: './vendor-create.component.html',
  styleUrls: ['./vendor-create.component.scss']
})
export class VendorCreateComponent {
  @ViewChild('vendorForm') vendorForm!: NgForm;

  vendor: VendorDto = {
    vendorName: '',
    taxId: '',
    description: '',
    primaryContactName: '',
    primaryContactEmail: '',
    communicationScore: 0,
    deliveryScore: 0,
    qualityScore: 0,
    overallScore: 0
  };

  loading = false;
  error: string | null = null;

  constructor(
    private vendorService: VendorModuleService,
    private router: Router
  ) { }

  discard() {
    this.router.navigate(['/projects']); // Fallback to projects for now, or just /
  }

  saveChanges() {
    this.loading = true;
    this.error = null;

    if (this.vendorForm.invalid) {
      this.error = 'Please fill in all required fields.';
      this.loading = false;

      this.vendorForm.form.markAllAsTouched();
      return;
    }

    this.vendorService.createVendor(this.vendor).subscribe({
      next: () => {
        this.loading = false;
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.loading = false;
        this.error = 'Failed to create vendor. Please try again.';
        console.error('Error creating vendor:', err);
      }
    });
  }
}
