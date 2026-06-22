import { Component, OnInit, ViewChild, ElementRef } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { environment } from 'src/environments/environment';
import { forkJoin, Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { RequisitionModuleService, RequisitionDto, RequisitionQuotesModuleService, QuoteDto, InvoiceDto, AuditModuleService, AuditLogDto } from 'src/app/core/api';
import { AuthService } from 'src/app/core/services/auth.service';
import { RejectDialogComponent} from "../../../shared/components/reject-dialog/reject-dialog.component";
import { AssigneeSelectDialogComponent } from '../../../shared/components/assignee-select-dialog/assignee-select-dialog.component';
import {MatDialog} from "@angular/material/dialog";
import {ToastService} from "../../../core/services/toast.service";
import { Location } from '@angular/common';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import {
  ConfirmationDialogComponent
} from "../../../shared/components/confirmation-dialog/confirmation-dialog.component";
import { WorkflowEditorComponent } from '../../workflow/workflow-editor/workflow-editor.component';
import { extractErrorMessage } from 'src/app/shared/utils/error-utils';

interface ActionConfig {
  icon: string;
  color: string;
  bg: string;
}

const ACTION_CONFIGS: Record<string, ActionConfig> = {
  APPROVE: { icon: 'check_circle', color: 'text-emerald-600', bg: 'bg-emerald-100' },
  REVERT: { icon: 'undo', color: 'text-amber-600', bg: 'bg-amber-100' },
  REJECT: { icon: 'cancel', color: 'text-red-600', bg: 'bg-red-100' },
  SUBMIT: { icon: 'send', color: 'text-blue-600', bg: 'bg-blue-100' },
  PAID: { icon: 'payments', color: 'text-lime-600', bg: 'bg-lime-100' },
  REQUISITION_EDITED: { icon: 'edit', color: 'text-orange-600', bg: 'bg-orange-100' },
  NOTIFICATION_SENT: { icon: 'notifications', color: 'text-indigo-600', bg: 'bg-indigo-100' },
  JIRA_SYNC: { icon: 'sync', color: 'text-violet-600', bg: 'bg-violet-100' },
  JIRA_UNSYNC: { icon: 'sync_disabled', color: 'text-stone-600', bg: 'bg-stone-100' },
  JIRA_COMMENT_POSTED: { icon: 'comment', color: 'text-fuchsia-600', bg: 'bg-fuchsia-100' },
  REQUESTER_CHANGED: { icon: 'person_edit', color: 'text-yellow-600', bg: 'bg-yellow-100' }
};

const DEFAULT_ACTION_CONFIG: ActionConfig = {
  icon: 'info', color: 'text-slate-500', bg: 'bg-slate-100'
};

@Component({
  selector: 'app-requisition-detail',
  templateUrl: './requisition-detail.component.html'
})
export class RequisitionDetailComponent implements OnInit {
  request: RequisitionDto | null = null;
  selectedQuote: QuoteDto | null = null;
  allQuotes: QuoteDto[] = [];
  loading = false;
  role: string = this.authService.getRole() ?? '';
  canAct = false;

  isDragging = false;
  isUploadingAttachment = false;
  @ViewChild('fileInput') fileInput!: ElementRef<HTMLInputElement>;

  isDrawerOpen = false;
  isDrawerExpanded = false;
  pdfUrl: SafeResourceUrl | null = null;
  rawPdfUrl = '';
  isProcessingPayment = false;
  invoice: InvoiceDto | null = null;

  auditLogs: AuditLogDto[] = [];
  auditLogsLoading = false;

  get invoiceToQuoteDiff(): number | null {
    if (this.invoice?.totalAmount == null || this.selectedQuote?.totalAmount == null) return null;
    return this.invoice.totalAmount - this.selectedQuote.totalAmount;
  }

  get canUploadAttachments(): boolean {
    return this.canAct && this.request?.state !== 'DRAFT' && this.request?.state !== 'FINISHED';
  }

  get missingApproveRequirements(): string {
    if (!this.request || this.request.responsibleRole !== 'PROCUREMENT_OFFICER') return '';
    const missing: string[] = [];
    if (!this.selectedQuote) {
      missing.push('select a quote');
    }
    if (!this.invoice) {
      missing.push('add an invoice');
    }
    if (missing.length === 0) return '';
    return 'Please ' + missing.join(' and ') + ' to approve.';
  }

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private requisitionService: RequisitionModuleService,
    private quotesService: RequisitionQuotesModuleService,
    private auditService: AuditModuleService,
    public authService: AuthService,
    private dialog: MatDialog,
    private toastService: ToastService,
    private sanitizer: DomSanitizer,
    private location: Location
  ) { }

  private requestId!: number;

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      const id = Number(params.get('id'));
      if (id) {
        this.loadRequest(id);
        this.requestId = id;
      } else {
        this.router.navigate(['/requisitions']);
      }
    });
  }

  loadRequest(id: number): void {
    this.loading = true;
    this.requisitionService.getRequestById(id).subscribe({
      next: (req) => {
        this.request = req;
        this.loadSelectedQuote(id);
        this.loadInvoice(id);
        this.checkCanAct(id);
        this.loadAuditLogs(id);
      },
      error: (err) => {
        console.error('Failed to load request details', err);
        this.loading = false;
        this.toastService.showError(extractErrorMessage(err, 'Failed to load request details'));
        this.router.navigate(['/dashboard']);
      }
    });
  }

  loadAuditLogs(id: number): void {
    this.auditLogsLoading = true;
    this.auditService.getAuditLogs(id).subscribe({
      next: (logs: AuditLogDto[]) => {
        this.auditLogs = logs || [];
        this.auditLogs.sort((a, b) => {
          const ta = a.timestamp ? new Date(a.timestamp).getTime() : 0;
          const tb = b.timestamp ? new Date(b.timestamp).getTime() : 0;
          return tb - ta; // Descending order: latest first
        });
        this.auditLogsLoading = false;
      },
      error: (err) => {
        console.error('Failed to load audit logs', err);
        this.toastService.showError(extractErrorMessage(err, 'Failed to load audit logs'));
        this.auditLogs = [];
        this.auditLogsLoading = false;
      }
    });
  }

  exportAuditPdf(): void {
    if (!this.requestId) return;
    this.auditService.exportAuditPdf(this.requestId).subscribe({
      next: (blob: Blob) => {
        this.downloadBlob(blob, `audit_report_req_${this.requestId}.pdf`);
      },
      error: (err) => {
        console.error('Failed to export audit PDF', err);
        this.toastService.showError(extractErrorMessage(err, 'Failed to export audit report'));
      }
    });
  }

  formatAction(action: string | undefined): string {
    if (!action) return '';
    return action.replace(/_/g, ' ').replace(/\w\S*/g, txt =>
      txt.charAt(0).toUpperCase() + txt.substring(1).toLowerCase()
    );
  }

  private getActionConfig(action: string | undefined): ActionConfig {
    if (!action) return DEFAULT_ACTION_CONFIG;
    return ACTION_CONFIGS[action] || DEFAULT_ACTION_CONFIG;
  }

  getActionIcon(action: string | undefined): string {
    return this.getActionConfig(action).icon;
  }

  getActionColor(action: string | undefined): string {
    return this.getActionConfig(action).color;
  }

  getActionBgColor(action: string | undefined): string {
    return this.getActionConfig(action).bg;
  }

  loadInvoice(id: number): void {
    this.requisitionService.getInvoice(id).subscribe({
      next: (inv) => {
        this.invoice = inv;
      },
      error: (err) => {
        console.error('Failed to load invoice', err);
        this.invoice = null;
      }
    });
  }

  checkCanAct(id: number): void {
    this.requisitionService.canAct(id).subscribe({
      next: (res) => {
        this.canAct = res;
      },
      error: (err) => {
        console.error('Failed to check canAct', err);
        this.canAct = false;
      }
    });
  }

  loadSelectedQuote(id: number): void {
    this.quotesService.getQuotesForRequest(id).subscribe({
      next: (quotes) => {
        this.allQuotes = quotes || [];
        if (quotes && Array.isArray(quotes)) {
          this.selectedQuote = quotes.find(q => q.isSelected) || null;
        } else {
          this.selectedQuote = null;
        }
        this.loading = false;
      },
      error: (err) => {
        console.error('Failed to load quotes for request', err);
        this.allQuotes = [];
        this.selectedQuote = null;
        this.loading = false;
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/dashboard']);
  }

  isRequester(): boolean {
    return this.request !== null && this.request.requesterId === this.authService.getUserId();
  }

  modifyRequest(): void {
    if (this.request?.id) {
      this.router.navigate(['/requisitions/edit', this.request.id]);
    }
  }

  revertRequest(): void {
    const dialogRef = this.dialog.open(RejectDialogComponent, {
      width: '80vw',
      maxWidth: '550px',
      disableClose: true,
      data: { isRevert: true }
    });

    dialogRef.afterClosed().subscribe((result: { reason: string, revisionRequired: boolean } | undefined) => {
      if (!result || !result.reason) {
        return;
      }

      this.loading = true;
      this.requisitionService.revertRequest(this.request!.id!, {
        reason: result.reason,
        revisionRequired: result.revisionRequired
      }).subscribe({
        next: () => {
          this.loading = false;
          this.toastService.showInfo("Requisition reverted successfully!");
          this.goBack();
        },
        error: (err) => {
          this.toastService.showError(extractErrorMessage(err, 'Failed to revert requisition'));
          this.loading = false;
        }
      });
    });
  }

  rejectRequest(): void {
    const dialogRef = this.dialog.open(RejectDialogComponent, {
      width: '80vw',
      maxWidth: '550px',
      disableClose: true
    });

    dialogRef.afterClosed().subscribe((result: { reason: string, revisionRequired: boolean } | undefined) => {
      if (!result || !result.reason) {
        return;
      }

      this.loading = true;
      this.requisitionService.rejectRequest(this.request!.id!, {reason: result.reason}).subscribe({
        next: () => {
          this.loading = false;
          this.toastService.showInfo("Requisition rejected successfully!");
          this.goBack();
        },
        error: (err) => {
          this.toastService.showError(extractErrorMessage(err, 'Failed to reject requisition'));
          this.loading = false;
        }
      });
    });
  }

  approveRequest(): void {
    if (!this.request || !this.request.id) return;

    if (this.request.responsibleRole === 'PROCUREMENT_OFFICER') {
      if (!this.selectedQuote || !this.invoice) {
        this.toastService.showError(this.missingApproveRequirements);
        return;
      }
    }

    this.loading = true;
    this.requisitionService.getNextStepRole(this.request.id).subscribe({
      next: (role) => {
        if (role) {
          if (role === 'REQUESTER') {
            this.executeApproval(null);
            return;
          }

          this.requisitionService.getEligibleAssignees(this.request!.id!, role).subscribe({
            next: (users) => {
              this.loading = false;
              const activeUsers = users.filter(u => u.active !== false);

              const dialogRef = this.dialog.open(AssigneeSelectDialogComponent, {
                width: '80vw',
                maxWidth: '550px',
                disableClose: true,
                data: {
                  role: role,
                  users: activeUsers
                }
              });

              dialogRef.afterClosed().subscribe((assigneeId: number | null | undefined) => {
                if (assigneeId === undefined) {
                  return;
                }
                this.executeApproval(assigneeId);
              });
            },
            error: (err) => {
              this.loading = false;
              this.toastService.showError('Failed to fetch assignees for role ' + role);
            }
          });
        } else {
          this.executeApproval(null);
        }
      },
      error: (err) => {
        this.loading = false;
        this.toastService.showError('Failed to determine next step role');
      }
    });
  }

  private executeApproval(assigneeId: number | null): void {
    this.loading = true;
    const nextAssigneeId = assigneeId !== null ? assigneeId : undefined;
    this.requisitionService.approveRequest(this.request!.id!, nextAssigneeId).subscribe({
      next: () => {
        this.loading = false;
        this.toastService.showInfo("Requisition approved successfully!");
        this.goBack();
      },
      error: (err) => {
        this.toastService.showError(extractErrorMessage(err, 'Approval failed'));
        this.loading = false;
      }
    });
  }

  submitRequest(): void {
    if (!this.request || !this.request.id) return;

    this.loading = true;
    this.requisitionService.getNextStepRole(this.request.id).subscribe({
      next: (role) => {
        if (role) {
          if (role === 'REQUESTER') {
            this.executeSubmission(null);
            return;
          }

          this.requisitionService.getEligibleAssignees(this.request?.id!, role).subscribe({
            next: (users) => {
              this.loading = false;
              const activeUsers = users.filter(u => u.active !== false);

              const dialogRef = this.dialog.open(AssigneeSelectDialogComponent, {
                width: '80vw',
                maxWidth: '550px',
                disableClose: true,
                data: {
                  role: role,
                  users: activeUsers
                }
              });

              dialogRef.afterClosed().subscribe((assigneeId: number | null | undefined) => {
                if (assigneeId === undefined) {
                  return;
                }
                this.executeSubmission(assigneeId);
              });
            },
            error: (err) => {
              this.loading = false;
              this.toastService.showError('Failed to fetch assignees for role ' + role);
            }
          });
        } else {
          this.executeSubmission(null);
        }
      },
      error: (err) => {
        this.loading = false;
        this.toastService.showError('Failed to determine next step role');
      }
    });
  }

  private executeSubmission(assigneeId: number | null): void {
    this.loading = true;
    const nextAssigneeId = assigneeId !== null ? assigneeId : undefined;
    this.requisitionService.submitRequest(this.request!.id!, nextAssigneeId).subscribe({
      next: () => {
        this.loading = false;
        this.toastService.showInfo("Requisition submitted successfully!");
        this.goBack();
      },
      error: (err) => {
        this.toastService.showError(extractErrorMessage(err, 'Submission failed'));
        this.loading = false;
      }
    });
  }

  cancelRequest(): void {
    if (!this.request || !this.request.id) return;

    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        title: 'Cancel Requisition',
        message: 'Are you sure you want to cancel this requisition? This action cannot be undone.'
      }
    });

    dialogRef.afterClosed().subscribe((confirmed) => {
      if (confirmed) {
        this.loading = true;
        this.requisitionService.cancelRequest(this.request!.id!).subscribe({
          next: () => {
            this.loading = false;
            this.toastService.showInfo("Requisition cancelled successfully!");
            this.goBack();
          },
          error: (err) => {
            this.toastService.showError(extractErrorMessage(err, 'Cancellation failed'));
            this.loading = false;
          }
        });
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
    this.requisitionService.downloadAttachment(attachment.attachmentId).subscribe({
      next: (blob) => {
        this.downloadBlob(blob, attachment.fileName || 'download');
      },
      error: (err) => {
        console.error('Failed to download attachment', err);
        this.toastService.showError('Failed to download attachment')
      }
    });
  }

  deleteAttachment(attachment: any): void {
    const ref = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        title: 'Delete attachment',
        message: `Are you sure you want to delete attachment "${attachment.fileName}"?`
      }
    });

    ref.afterClosed().subscribe((confirmed) => {
      if (confirmed) {
        this.requisitionService.deleteAttachment(attachment.attachmentId).subscribe({
          next: () => {
            this.toastService.showSuccess('Attachment deleted');
            this.loadRequest(this.requestId);
          },
          error: () => this.toastService.showError('Failed to delete attachment')
        });
      }
    });
  }

  processPayment(): void {
    if (!this.request) return;
    this.isDrawerOpen = true;
    this.isDrawerExpanded = false;
    this.invoice = null;

    this.requisitionService.getInvoice(this.requestId).subscribe({
      next: (inv) => this.invoice = inv,
      error: () => this.invoice = null
    });

    const invoicePdf = this.request.attachments?.find(
      (a: any) => a.fileType === 'application/pdf'
    );

    if (invoicePdf && invoicePdf.attachmentId) {
      this.requisitionService.downloadAttachment(invoicePdf.attachmentId).subscribe({
        next: (blob) => {
          const blobUrl = window.URL.createObjectURL(blob);
          this.rawPdfUrl = blobUrl;
          this.pdfUrl = this.sanitizer.bypassSecurityTrustResourceUrl(blobUrl);
        },
        error: (err) => {
          console.error('Failed to load PDF preview', err);
          this.pdfUrl = null;
          this.rawPdfUrl = '';
        }
      });
    } else {
      this.pdfUrl = null;
      this.rawPdfUrl = '';
    }
  }

  closePaymentDrawer(): void {
    this.isDrawerOpen = false;
    this.isDrawerExpanded = false;
    if (this.rawPdfUrl && this.rawPdfUrl.startsWith('blob:')) {
      window.URL.revokeObjectURL(this.rawPdfUrl);
    }
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
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.isProcessingPayment = false;
        this.toastService.showError(extractErrorMessage(err, 'Failed to process payment'));
      }
    });
  }

  triggerFileInput(): void {
    this.fileInput.nativeElement.click();
  }

  onFileSelected(event: any): void {
    const files = event.target.files;
    if (files && files.length > 0) {
      this.uploadFiles(files);
    }
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging = true;
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging = false;
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging = false;
    const files = event.dataTransfer?.files;
    if (files && files.length > 0) {
      this.uploadFiles(files);
    }
  }

  private uploadFiles(files: FileList): void {
    if (!this.request || !this.request.id) return;

    this.isUploadingAttachment = true;
    const uploadTasks: Observable<any>[] = [];

    for (let i = 0; i < files.length; i++) {
      const file = files[i];
      if (file.size > environment.maxFileSize) {
        const maxMb = Math.round(environment.maxFileSize / (1024 * 1024));
        this.toastService.showError(`File ${file.name} exceeds the ${maxMb}MB limit.`);
        continue;
      }
      uploadTasks.push(
        this.requisitionService.uploadAttachment(this.request.id, file as any).pipe(
          catchError((err) => {
            this.toastService.showError(extractErrorMessage(err, 'Failed to upload ' + file.name));
              return of(null);
            })
        )
      );
    }

    if (uploadTasks.length === 0) {
      this.isUploadingAttachment = false;
      return;
    }

    forkJoin(uploadTasks).subscribe({
      next: (results) => {
        this.isUploadingAttachment = false;
        const successfulCount = results.filter(res => res !== null).length;
        if (successfulCount > 0) {
          this.toastService.showSuccess(`${successfulCount} file(s) uploaded successfully`);
          this.loadRequest(this.requestId);
        }
      },
      error: (err) => {
        this.isUploadingAttachment = false;
        this.toastService.showError('Upload failed');
      }
    });
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

  viewWorkflow(id: number | undefined): void {
    if (id) {
      this.dialog.open(WorkflowEditorComponent, {
        width: '90vw',
        height: '90vh',
        maxWidth: '1200px',
        panelClass: 'overflow-hidden',
        data: { mode: 'view', id: id, currentStatus: this.request?.status }
      });
    }
  }
}
