import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { environment } from 'src/environments/environment';
import { RequisitionModuleService, RequisitionDto, QuoteDto, RequisitionQuotesModuleService, InvoiceCreateDto, InvoiceCreateDtoCurrencyEnum, InvoiceDto } from '../../../../core/api';
import { ToastService } from '../../../../core/services/toast.service';
import { extractErrorMessage } from '../../../../shared/utils/error-utils';
import { MatDialog } from '@angular/material/dialog';
import { MatTableDataSource } from '@angular/material/table';
import { ConfirmationDialogComponent } from '../../../../shared/components/confirmation-dialog/confirmation-dialog.component';

import { forkJoin } from 'rxjs';

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
  displayedColumns = ['vendor', 'currency', 'totalAmount', 'totalAmountEuro', 'status', 'actions'];

  isInvoiceDrawerOpen = false;
  isUploadingInvoice = false;
  invoiceFile: File | null = null;
  invoiceForm!: FormGroup;
  hasInvoice = false;
  isViewingInvoice = false;
  isEditingInvoice = false;
  invoice: InvoiceDto | null = null;
  invoiceAttachment: any = null;

  get hasPreferredQuote(): string {
    const selected = this.quotes.find(q => q.isSelected);
    return selected?.vendor?.vendorName || 'None';
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
      invoiceNumber: ['', [Validators.required, Validators.pattern(/.*\S.*/)]],
      totalAmount: [null, [Validators.required, Validators.min(0.01)]],
      dueDate: ['', Validators.required],
      invoiceDate: ['', Validators.required]
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

  findInvoiceAttachment(): void {
    if (this.invoice && this.requisition?.attachments) {
      this.invoiceAttachment = this.requisition.attachments.find(
        (a: any) => a.invoiceId === this.invoice?.invoiceId
      ) || null;
    } else {
      this.invoiceAttachment = null;
    }
  }

  loadRequisition(): void {
    this.loading = true;
    forkJoin({
      req: this.requisitionService.getRequestById(this.requisitionId),
      canAct: this.requisitionService.canAct(this.requisitionId)
    }).subscribe({
      next: ({ req, canAct }) => {
        this.requisition = req;
        if (this.requisition.state === 'FINISHED') {
          this.toastService.showError('Cannot manage quotes for a finished request.');
          this.router.navigate([`/requisitions/${this.requisitionId}`]);
          return;
        }
        if (!canAct) {
          this.toastService.showError('You are not responsible for the current workflow step of this requisition.');
          this.router.navigate([`/requisitions/${this.requisitionId}`]);
          return;
        }
        this.findInvoiceAttachment();
      },
      error: (err: any) => {
        console.error('Failed to load requisition', err);
        this.toastService.showError(extractErrorMessage(err, 'Failed to load requisition'));
        this.router.navigate(['/dashboard']);
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
        this.toastService.showError(extractErrorMessage(err, 'Failed to load vendor quotes'));
        this.loading = false;
      }
    });
  }

  checkInvoiceExists(): void {
    this.requisitionService.getInvoice(this.requisitionId).subscribe({
      next: (inv) => {
        this.hasInvoice = true;
        this.invoice = inv;
        this.findInvoiceAttachment();
      },
      error: () => {
        this.hasInvoice = false;
        this.invoice = null;
        this.invoiceAttachment = null;
      }
    });
  }

  selectQuote(quote: QuoteDto): void {
    if (!quote.quoteId) return;
    if (quote.isSelected) return;

    if (this.hasInvoice) {
      const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
        maxWidth: '500px',
        data: {
          title: 'Select Quote',
          message: 'Selecting a different quote will automatically delete the currently uploaded invoice since it was created for the previous quote. Do you want to proceed?'
        }
      });

      dialogRef.afterClosed().subscribe((confirmed: any) => {
        if (confirmed) {
          this.executeQuoteSelection(quote);
        }
      });
    } else {
      this.executeQuoteSelection(quote);
    }
  }

  private executeQuoteSelection(quote: QuoteDto): void {
    if (!quote.quoteId) return;
    this.submitting = true;
    this.quoteService.selectQuote(this.requisitionId, quote.quoteId).subscribe({
      next: () => {
        this.toastService.showSuccess('Quote selected successfully');
        this.loadQuotes();
        this.checkInvoiceExists();
        this.submitting = false;
      },
      error: (err: any) => {
        console.error('Failed to select quote', err);
        this.toastService.showError(extractErrorMessage(err, 'Failed to select quote'));
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
            this.toastService.showError(extractErrorMessage(err, 'Failed to delete quote'));
            this.loading = false;
          }
        });
      }
    });
  }

  viewInvoice(): void {
    if (!this.invoice) return;
    this.isViewingInvoice = true;
    this.isEditingInvoice = false;
    this.invoiceFile = null;

    this.invoiceForm.patchValue({
      invoiceNumber: this.invoice.invoiceNumber,
      totalAmount: this.invoice.totalAmount,
      dueDate: this.invoice.dueDate ? new Date(this.invoice.dueDate) : null,
      invoiceDate: this.invoice.invoiceDate ? new Date(this.invoice.invoiceDate) : null
    });
    this.invoiceForm.disable();
    this.isInvoiceDrawerOpen = true;
  }

  startEditingInvoice(): void {
    this.isEditingInvoice = true;
    this.invoiceForm.enable();
  }

  cancelEditingInvoice(): void {
    if (this.isViewingInvoice) {
      if (this.isEditingInvoice) {
        this.isEditingInvoice = false;
        this.invoiceForm.disable();
        if (this.invoice) {
          this.invoiceForm.patchValue({
            invoiceNumber: this.invoice.invoiceNumber,
            totalAmount: this.invoice.totalAmount,
            dueDate: this.invoice.dueDate ? new Date(this.invoice.dueDate) : null,
            invoiceDate: this.invoice.invoiceDate ? new Date(this.invoice.invoiceDate) : null
          });
        }
        this.invoiceFile = null;
      } else {
        this.closeInvoiceDrawer();
      }
    } else {
      this.closeInvoiceDrawer();
    }
  }

  openInvoiceDrawer(): void {
    this.isViewingInvoice = false;
    this.isEditingInvoice = true;
    this.invoiceForm.enable();
    this.invoiceForm.reset();
    this.invoiceFile = null;

    if (this.selectedQuote?.totalAmount) {
      this.invoiceForm.patchValue({ totalAmount: this.selectedQuote.totalAmount });
    }

    const defaultDue = new Date();
    this.invoiceForm.patchValue({ dueDate: defaultDue });
    this.invoiceForm.patchValue({ invoiceDate: new Date() });

    this.isInvoiceDrawerOpen = true;
  }

  closeInvoiceDrawer(): void {
    this.isInvoiceDrawerOpen = false;
    this.invoiceFile = null;
    this.isViewingInvoice = false;
    this.isEditingInvoice = false;
    this.invoiceForm.enable();
    this.invoiceForm.reset();
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const file = input.files[0];
      if (file.size > environment.maxFileSize) {
        const maxMb = Math.round(environment.maxFileSize / (1024 * 1024));
        this.toastService.showError(`File ${file.name} exceeds the ${maxMb}MB limit.`);
        return;
      }
      this.invoiceFile = file;
    }
  }

  onFileDrop(event: DragEvent): void {
    event.preventDefault();
    if (event.dataTransfer?.files && event.dataTransfer.files.length > 0) {
      const file = event.dataTransfer.files[0];
      if (file.size > environment.maxFileSize) {
        const maxMb = Math.round(environment.maxFileSize / (1024 * 1024));
        this.toastService.showError(`File ${file.name} exceeds the ${maxMb}MB limit.`);
        return;
      }
      this.invoiceFile = file;
    }
  }

  downloadAttachment(attachment: any): void {
    if (!attachment || !attachment.attachmentId) return;
    this.requisitionService.downloadAttachment(attachment.attachmentId).subscribe({
      next: (blob) => {
        this.downloadBlob(blob, attachment.fileName || 'download');
      },
      error: (err) => {
        console.error('Failed to download attachment', err);
        this.toastService.showError('Failed to download attachment');
      }
    });
  }

  submitInvoice(): void {
    if (this.invoiceForm.invalid) {
      this.invoiceForm.markAllAsTouched();
      return;
    }

    this.isUploadingInvoice = true;
    const formValue = this.invoiceForm.value;

    const invoiceData: InvoiceCreateDto = {
      invoiceNumber: formValue.invoiceNumber,
      totalAmount: formValue.totalAmount,
      currency: (this.selectedQuote?.currency as unknown as InvoiceCreateDtoCurrencyEnum) || InvoiceCreateDtoCurrencyEnum.Eur,
      dueDate: this.formatDate(formValue.dueDate),
      invoiceDate: this.formatDate(formValue.invoiceDate)
    };

    if (this.isViewingInvoice) {
      this.requisitionService.updateInvoice(
        this.requisitionId,
        invoiceData,
        this.invoiceFile || undefined
      ).subscribe({
        next: () => {
          this.isUploadingInvoice = false;
          this.toastService.showSuccess('Invoice updated successfully');
          this.closeInvoiceDrawer();
          this.loadRequisition();
          this.checkInvoiceExists();
        },
        error: (err: any) => {
          this.isUploadingInvoice = false;
          this.toastService.showError(extractErrorMessage(err, 'Failed to update invoice'));
        }
      });
    } else {
      this.requisitionService.createInvoice(
        this.requisitionId,
        invoiceData,
        this.invoiceFile || undefined
      ).subscribe({
        next: () => {
          this.isUploadingInvoice = false;
          this.toastService.showSuccess('Invoice uploaded successfully');
          this.closeInvoiceDrawer();
          this.loadRequisition();
          this.checkInvoiceExists();
        },
        error: (err: any) => {
          this.isUploadingInvoice = false;
          this.toastService.showError(extractErrorMessage(err, 'Failed to upload invoice'));
        }
      });
    }
  }

  deleteInvoice(): void {
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        title: 'Delete Invoice',
        message: 'Are you sure you want to delete the current invoice? This action cannot be undone.'
      }
    });

    dialogRef.afterClosed().subscribe((confirmed: any) => {
      if (confirmed) {
        this.loading = true;
        this.requisitionService.deleteInvoice(this.requisitionId).subscribe({
          next: () => {
            this.toastService.showSuccess('Invoice deleted successfully');
            this.closeInvoiceDrawer();
            this.loadRequisition();
            this.checkInvoiceExists();
            this.loading = false;
          },
          error: (err: any) => {
            this.toastService.showError(extractErrorMessage(err, 'Failed to delete invoice'));
            this.loading = false;
          }
        });
      }
    });
  }

  openComparisonPage(): void {
    if (!this.requisition || this.quotes.length < 2) return;
    this.router.navigate([`/requisitions/${this.requisitionId}/compare-quotes`]);
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

  private downloadBlob(blob: Blob, fileName: string): void {
    const downloadUrl = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.style.display = 'none';
    a.href = downloadUrl;
    a.download = fileName;
    document.body.appendChild(a);
    a.click();
    // Revoke the object URL and remove the temporary link asynchronously
    // to avoid cancelling the download in some browsers.
    setTimeout(() => {
      window.URL.revokeObjectURL(downloadUrl);
      document.body.removeChild(a);
    }, 100);
  }

  private formatDate(date: Date | string | null | undefined): string {
    if (!date) return '';
    const d = new Date(date);
    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
}
