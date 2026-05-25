import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RequisitionModuleService, RequisitionDto, QuoteDto, RequisitionQuotesModuleService, InvoiceCreateDto } from '../../../../core/api';
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

  isInvoiceDrawerOpen = false;
  isUploadingInvoice = false;
  invoiceFile: File | null = null;
  invoiceForm!: FormGroup;
  hasInvoice = false;

  get hasPreferredQuote(): string {
    return this.quotes.some(q => q.isSelected) ? 'Selected' : 'None';
  }

  get selectedQuote(): QuoteDto | null {
    return this.quotes.find(q => q.isSelected) || null;
  }

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private quoteService: RequisitionQuotesModuleService,
    private requisitionService: RequisitionModuleService,
    private toastService: ToastService,
    private dialog: MatDialog,
    private fb: FormBuilder
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

    this.invoiceForm = this.fb.group({
      invoiceNumber: ['', Validators.required],
      totalAmount: [null, [Validators.required, Validators.min(0.01)]],
      dueDate: ['', Validators.required],
      invoiceDate: ['']
    });
  }

  ngOnInit(): void {
    this.requisitionId = Number(this.route.snapshot.paramMap.get('id'));
    if (this.requisitionId) {
      this.loadRequisition();
      this.loadQuotes();
      this.checkInvoiceExists();
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

  checkInvoiceExists(): void {
    this.requisitionService.getInvoice(this.requisitionId).subscribe({
      next: () => {
        this.hasInvoice = true;
      },
      error: () => {
        this.hasInvoice = false;
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

  openInvoiceDrawer(): void {
    this.invoiceForm.reset();
    this.invoiceFile = null;

    if (this.selectedQuote?.totalAmount) {
      this.invoiceForm.patchValue({ totalAmount: this.selectedQuote.totalAmount });
    }

    const defaultDue = new Date();
    this.invoiceForm.patchValue({ dueDate: defaultDue.toISOString().split('T')[0] });

    this.invoiceForm.patchValue({ invoiceDate: new Date().toISOString().split('T')[0] });

    this.isInvoiceDrawerOpen = true;
  }

  closeInvoiceDrawer(): void {
    this.isInvoiceDrawerOpen = false;
    this.invoiceFile = null;
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const file = input.files[0];
      if (file.type !== 'application/pdf') {
        this.toastService.showError('Only PDF files are allowed');
        return;
      }
      this.invoiceFile = file;
    }
  }

  onFileDrop(event: DragEvent): void {
    event.preventDefault();
    if (event.dataTransfer?.files && event.dataTransfer.files.length > 0) {
      const file = event.dataTransfer.files[0];
      if (file.type !== 'application/pdf') {
        this.toastService.showError('Only PDF files are allowed');
        return;
      }
      this.invoiceFile = file;
    }
  }

  submitInvoice(): void {
    if (this.invoiceForm.invalid) return;

    this.isUploadingInvoice = true;
    const formValue = this.invoiceForm.value;

    const invoiceData: InvoiceCreateDto = {
      invoiceNumber: formValue.invoiceNumber,
      totalAmount: formValue.totalAmount,
      dueDate: formValue.dueDate,
      invoiceDate: formValue.invoiceDate || undefined
    };

    this.requisitionService.createInvoice(
      this.requisitionId,
      invoiceData,
      this.invoiceFile || undefined
    ).subscribe({
      next: () => {
        this.isUploadingInvoice = false;
        this.toastService.showSuccess('Invoice uploaded successfully');
        this.closeInvoiceDrawer();
        this.hasInvoice = true;
      },
      error: (err: any) => {
        this.isUploadingInvoice = false;
        const message = err?.error || 'Failed to upload invoice';
        this.toastService.showError(message);
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
