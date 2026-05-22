import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { RequisitionModuleService, RequisitionDto, RequisitionQuotesModuleService, QuoteDto } from 'src/app/core/api';
import { AuthService } from 'src/app/core/services/auth.service';
import { RejectDialogComponent} from "../../../shared/components/reject-dialog/reject-dialog.component";
import {MatDialog} from "@angular/material/dialog";
import {ToastService} from "../../../core/services/toast.service";
import { Location } from '@angular/common';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

@Component({
  selector: 'app-requisition-detail',
  templateUrl: './requisition-detail.component.html'
})
export class RequisitionDetailComponent implements OnInit {
  request: RequisitionDto | null = null;
  selectedQuote: QuoteDto | null = null;
  loading = false;
  role: string = this.authService.getRole() ?? '';

  isDrawerOpen = false;
  isDrawerExpanded = false;
  pdfUrl: SafeResourceUrl | null = null;
  rawPdfUrl = '';
  isProcessingPayment = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private requisitionService: RequisitionModuleService,
    private quotesService: RequisitionQuotesModuleService,
    public authService: AuthService,
    private dialog: MatDialog,
    private toastService: ToastService,
    private sanitizer: DomSanitizer,
    private location: Location
  ) { }

  private requestId!: number;

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (id) {
      this.loadRequest(id);
      this.requestId = id;
    } else {
      this.router.navigate(['/requisitions']);
    }
  }

  loadRequest(id: number): void {
    this.loading = true;
    this.requisitionService.getRequestById(id).subscribe({
      next: (req) => {
        this.request = req;
        this.loadSelectedQuote(id);
      },
      error: (err) => {
        console.error('Failed to load request details', err);
        this.loading = false;
      }
    });
  }

  loadSelectedQuote(id: number): void {
    this.quotesService.getQuotesForRequest(id).subscribe({
      next: (quotes) => {
        if (quotes && Array.isArray(quotes)) {
          this.selectedQuote = quotes.find(q => q.isSelected) || null;
        } else {
          this.selectedQuote = null;
        }
        this.loading = false;
      },
      error: (err) => {
        console.error('Failed to load quotes for request', err);
        this.selectedQuote = null;
        this.loading = false;
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/dashboard']);
  }

  modifyRequest(): void {
    if (this.request?.id) {
      this.router.navigate(['/requisitions/edit', this.request.id]);
    }
  }

  rejectRequest(): void {
    const dialogRef = this.dialog.open(RejectDialogComponent, {
      width: '80vw',
      maxWidth: '550px',
      disableClose: true
    });

    dialogRef.afterClosed().subscribe((reason: string | undefined) => {
      if (!reason) {
        return;
      }

      this.loading = true;
      this.requisitionService.rejectRequest(this.request!.id!, {reason: reason}).subscribe({
        next: () => {
          this.loading = false;
          this.toastService.showInfo("Requisition rejected successfully!");
          this.goBack();
        },
        error: (err) => {
          this.toastService.showError(err.error);
          this.loading = false;
        }
      });
    });
  }

  approveRequest(): void {
    if (!this.request || !this.request.id) return;
    this.loading = true;
    this.requisitionService.approveRequest(this.request.id).subscribe({
      next: () => {
        this.loading = false;
        this.toastService.showInfo("Requisition approved successfully!");
        this.goBack();
      },
      error: (err) => {
        this.toastService.showError(err.error);
        this.loading = false;
      }
    });
  }

  submitRequest(): void {
    if (!this.request || !this.request.id) return;

    this.loading = true;

    this.requisitionService.submitRequest(this.request.id).subscribe({
      next: () => {
        this.loading = false;
        this.toastService.showInfo("Requisition submitted successfully!");
        this.goBack();
      },
      error: (err) => {
        this.toastService.showError(err.error);
        this.loading = false;
      }
    });

  }

  formatRole(role: string | undefined): string {
    if (!role) return '';
    return role.replace(/_/g, ' ').replace(/\w\S*/g, txt =>
      txt.charAt(0).toUpperCase() + txt.substring(1).toLowerCase()
    );
  }

  viewVendorQuotes(): void {
    if (this.request) {
      this.router.navigate([`/requisitions/${this.request.id}/vendor-quotes`]);
    }
  }

  downloadAttachment(attachment: any): void {
    if (!attachment || !attachment.attachmentId) return;
    const url = `http://localhost:8080/api/v1/requisitions/attachments/${attachment.attachmentId}`;
    const token = this.authService.getToken();
    const headers: any = {};
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    fetch(url, { headers })
      .then(response => {
        if (!response.ok) throw new Error('Download failed');
        return response.blob();
      })
      .then(blob => {
        const downloadUrl = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.style.display = 'none';
        a.href = downloadUrl;
        a.download = attachment.fileName || 'download';
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(downloadUrl);
        document.body.removeChild(a);
      })
      .catch(err => {
        console.error('Failed to download attachment', err);
        alert('Failed to download attachment');
      });
  }

  processPayment(): void {
    if (!this.request) return;
    this.isDrawerOpen = true;
    this.isDrawerExpanded = false;

    const invoicePdf = this.request.attachments?.find(
      (a: any) => a.fileType === 'application/pdf'
    );

    if (invoicePdf) {
      this.rawPdfUrl = `http://localhost:8080/api/v1/requisitions/attachments/${invoicePdf.attachmentId}`;
      this.pdfUrl = this.sanitizer.bypassSecurityTrustResourceUrl(this.rawPdfUrl);
    } else {
      this.pdfUrl = null;
      this.rawPdfUrl = '';
    }
  }

  closePaymentDrawer(): void {
    this.isDrawerOpen = false;
    this.isDrawerExpanded = false;
    this.pdfUrl = null;
    this.rawPdfUrl = '';
  }

  toggleDrawerExpand(): void {
    this.isDrawerExpanded = !this.isDrawerExpanded;
  }

  confirmPayment(): void {
    if (!this.request?.id) return;
    this.isProcessingPayment = true;
    this.requisitionService.processPayment(this.request.id).subscribe({
      next: () => {
        this.isProcessingPayment = false;
        this.toastService.showSuccess('Payment processed successfully');
        this.closePaymentDrawer();
        this.loadRequest(this.requestId);
      },
      error: (err) => {
        this.isProcessingPayment = false;
        this.toastService.showError('Failed to process payment');
      }
    });
  }
}
