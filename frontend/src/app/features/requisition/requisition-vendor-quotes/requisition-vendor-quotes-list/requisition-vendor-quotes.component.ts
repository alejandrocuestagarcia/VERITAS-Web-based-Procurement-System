import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { RequisitionModuleService, RequisitionDto, QuoteDto, RequisitionQuotesModuleService } from '../../../../core/api';
import { ToastService } from '../../../../core/services/toast.service';
import { MatDialog } from '@angular/material/dialog';
import { MatTableDataSource } from '@angular/material/table';
import { ConfirmationDialogComponent } from '../../../../shared/components/confirmation-dialog/confirmation-dialog.component';

@Component({
  selector: 'app-requisition-vendor-quotes',
  templateUrl: './requisition-vendor-quotes.component.html',
})
export class RequisitionVendorQuotesComponent implements OnInit {
  requisitionId!: number;
  requisition: RequisitionDto | null = null;
  quotes: QuoteDto[] = [];
  loading = false;
  submitting = false;

  dataSource = new MatTableDataSource<QuoteDto>();
  displayedColumns = ['vendor', 'currency', 'totalAmount', 'status', 'actions'];

  get hasPreferredQuote(): string {
    return this.quotes.some(q => q.isSelected) ? 'Selected' : 'None';
  }

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private quoteService: RequisitionQuotesModuleService,
    private requisitionService: RequisitionModuleService,
    private toastService: ToastService,
    private dialog: MatDialog
  ) {
    this.dataSource.filterPredicate = (data: QuoteDto, filter: string) => {
      const vendorName = data.vendor?.vendorName?.toLowerCase() || '';
      const currency = data.currency?.toLowerCase() || '';
      const totalAmount = data.totalAmount?.toString() || '';
      const status = data.isSelected ? 'selected' : 'pending';
      const searchTerms = filter.toLowerCase();

      return vendorName.includes(searchTerms) ||
             currency.includes(searchTerms) ||
             totalAmount.includes(searchTerms) ||
             status.includes(searchTerms);
    };
  }

  ngOnInit(): void {
    this.requisitionId = Number(this.route.snapshot.paramMap.get('id'));
    if (this.requisitionId) {
      this.loadRequisition();
      this.loadQuotes();
    } else {
      this.router.navigate(['/requisitions']);
    }
  }

  applyFilter(value: string): void {
    this.dataSource.filter = value.trim().toLowerCase();
  }

  loadRequisition(): void {
    this.requisitionService.getRequestById(this.requisitionId).subscribe({
      next: (req: RequisitionDto) => {
        this.requisition = req;
      },
      error: (err: any) => {
        console.error('Failed to load requisition', err);
        this.toastService.showError('Failed to load requisition');
      }
    });
  }

  loadQuotes(): void {
    this.loading = true;
    this.quoteService.getQuotesForRequest(this.requisitionId).subscribe({
      next: (quotes: QuoteDto[]) => {
        this.quotes = quotes || [];
        this.dataSource.data = this.quotes;
        this.loading = false;
      },
      error: (err: any) => {
        console.error('Failed to load quotes', err);
        this.toastService.showError('Failed to load vendor quotes');
        this.loading = false;
      }
    });
  }

  selectQuote(quote: QuoteDto): void {
    if (!quote.quoteId) return;
    this.submitting = true;
    this.quoteService.selectQuote(this.requisitionId, quote.quoteId).subscribe({
      next: () => {
        this.toastService.showSuccess('Quote selected successfully');
        this.loadQuotes();
        this.submitting = false;
      },
      error: (err: any) => {
        console.error('Failed to select quote', err);
        this.toastService.showError('Failed to select quote');
        this.submitting = false;
      }
    });
  }

  deleteQuote(quote: QuoteDto): void {
    if (!quote.quoteId) return;
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        title: 'Delete Quote',
        message: `Are you sure you want to delete the quote from "${quote.vendor?.vendorName || 'Vendor'}"?`
      }
    });

    dialogRef.afterClosed().subscribe((confirmed: any) => {
      if (confirmed) {
        this.loading = true;
        this.quoteService.deleteQuote(this.requisitionId, quote.quoteId!).subscribe({
          next: () => {
            this.toastService.showSuccess('Quote deleted successfully');
            this.loadQuotes();
          },
          error: (err: any) => {
            console.error('Failed to delete quote', err);
            this.toastService.showError('Failed to delete quote');
            this.loading = false;
          }
        });
      }
    });
  }

  navigateToCreate(): void {
    this.router.navigate([`/requisitions/${this.requisitionId}/vendor-quotes/create`]);
  }

  navigateToEdit(quoteId: number): void {
    this.router.navigate([`/requisitions/${this.requisitionId}/vendor-quotes/edit/${quoteId}`]);
  }

  navigateToView(quoteId: number): void {
    this.router.navigate([`/requisitions/${this.requisitionId}/vendor-quotes/view/${quoteId}`]);
  }

  goBack(): void {
    this.router.navigate([`/requisitions/${this.requisitionId}`]);
  }
}
