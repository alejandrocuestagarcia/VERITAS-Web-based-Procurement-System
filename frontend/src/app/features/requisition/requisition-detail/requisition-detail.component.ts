import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { RequisitionModuleService, RequisitionDto, RequisitionQuotesModuleService, QuoteDto, UserModuleService } from 'src/app/core/api';
import { AuthService } from 'src/app/core/services/auth.service';
import { RejectDialogComponent} from "../../../shared/components/reject-dialog/reject-dialog.component";
import { AssigneeSelectDialogComponent } from '../../../shared/components/assignee-select-dialog/assignee-select-dialog.component';
import {MatDialog} from "@angular/material/dialog";
import {ToastService} from "../../../core/services/toast.service";
import { Location } from '@angular/common';

@Component({
  selector: 'app-requisition-detail',
  templateUrl: './requisition-detail.component.html'
})
export class RequisitionDetailComponent implements OnInit {
  request: RequisitionDto | null = null;
  selectedQuote: QuoteDto | null = null;
  loading = false;
  role: string = this.authService.getRole() ?? '';
  canAct = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private requisitionService: RequisitionModuleService,
    private quotesService: RequisitionQuotesModuleService,
    public authService: AuthService,
    private dialog: MatDialog,
    private toastService: ToastService,
    private location: Location
  ) { }

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (id) {
      this.loadRequest(id);
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
        this.checkCanAct(id);
      },
      error: (err) => {
        console.error('Failed to load request details', err);
        this.loading = false;
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

  isRequester(): boolean {
    return this.request !== null && this.request.requesterId === this.authService.getUserId();
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
        this.toastService.showError(err.error?.message || err.error || "Approval failed");
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
        this.toastService.showError(err.error?.message || err.error || "Submission failed");
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
    this.requisitionService.downloadAttachment(attachment.attachmentId)
      .subscribe({
        next: (blob: Blob) => {
          const downloadUrl = window.URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.style.display = 'none';
          a.href = downloadUrl;
          a.download = attachment.fileName || 'download';
          document.body.appendChild(a);
          a.click();
          window.URL.revokeObjectURL(downloadUrl);
          document.body.removeChild(a);
        },
        error: (err) => {
          console.error('Failed to download attachment', err);
          alert('Failed to download attachment');
        }
      });
  }
}
