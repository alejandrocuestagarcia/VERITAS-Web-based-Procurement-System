import { Component, OnInit, ViewChild } from '@angular/core';
import { MatTableDataSource } from "@angular/material/table";
import { Pageable, VendorDto, VendorModuleService } from "../../../core/api";
import { PageEvent } from "@angular/material/paginator";
import { SharedTableComponent } from "../../../shared/components/table/shared-table.component";
import { MatDialog } from '@angular/material/dialog';
import { ConfirmationDialogComponent } from '../../../shared/components/confirmation-dialog/confirmation-dialog.component';
import { Router } from '@angular/router';

@Component({
  selector: 'app-vendor-list',
  templateUrl: './vendor-list.component.html',
})
export class VendorListComponent implements OnInit {

  dataSource = new MatTableDataSource<VendorDto>();
  totalVendorCount = 0;
  totalPageElements = 0;
  displayedColumns: string[] = ['vendorName', 'taxId', 'rating', 'contact', 'actions'];
  loading = false;
  averageScore = 0.0;
  subtitle = "Configure Vendors for potential Company Procurements.";
  title = "Vendor Management";

  minimumRating = 0.0;
  currentSearchString = "";

  @ViewChild(SharedTableComponent) sharedTable!: SharedTableComponent;

  constructor(
    private readonly vendorService: VendorModuleService,
    private dialog: MatDialog,
    private readonly router: Router
  ) { }

  ngOnInit(): void {
    this.loadVendors(0, 10);
    this.loadVendorStats();
  }

  onSearchChanged(value: string) {
    this.currentSearchString = value;
    this.sharedTable.resetToFirstPage();
    this.loadVendors(0, 10);
  }

  onSliderChange(value: number): void {
    this.minimumRating = value;
    this.sharedTable.resetToFirstPage();
    this.loadVendors(0, 10);
  }

  clearRating(): void {
    this.minimumRating = 0.0;
    this.sharedTable.resetToFirstPage();
    this.loadVendors(0, 10);
  }

  onPageChange(event: PageEvent): void {
    this.loadVendors(event.pageIndex, event.pageSize);
  }

  private loadVendors(page: number, size: number): void {
    this.loading = true;

    const pageable: Pageable = {
      page: page,
      size: size,
      sort: ['vendorName,asc']
    };

    this.vendorService.getAllVendors(pageable, this.currentSearchString, this.minimumRating).subscribe({
      next: (response) => {
        this.dataSource.data = response.content || [];
        this.totalPageElements = response.totalElements || 0;
        this.loading = false;
      },
      error: (err) => {
        console.error('VERITAS Error:', err);
        this.loading = false;
      }
    });
  }

  editVendor(vendor: VendorDto) {
    if (!vendor.id) {
      return;
    }

    this.router.navigate(['/vendors/edit', vendor.id]);
  }

  deleteVendor(vendor: VendorDto) {
    const ref = this.dialog.open(ConfirmationDialogComponent, {
      data: { title: 'Delete Vendor', message: `Are you sure you want to delete "${vendor.vendorName}"?` }
    });
    ref.afterClosed().subscribe((confirmed) => {
      if (confirmed) {
        // TODO: call deleteVendor
      }
    });
  }

  private loadVendorStats() {
    this.vendorService.getVendorStats().subscribe({
      next: (response) => {
        this.totalVendorCount = response.total ?? 0;
        this.averageScore = response.averageRating ?? 0.0;
      },
      error: (err) => {
        console.error('Error fetching user stats:', err);
      }
    })

  }
}
