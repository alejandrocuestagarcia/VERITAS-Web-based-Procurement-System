import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { RequisitionModuleService, RequisitionDto, RequisitionQuotesModuleService, QuoteDto } from '../../../../core/api';
import { ToastService } from '../../../../core/services/toast.service';
import { extractErrorMessage } from '../../../../shared/utils/error-utils';

@Component({
  selector: 'app-requisition-vendor-quote-view',
  templateUrl: './requisition-vendor-quote-view.component.html',
})
export class RequisitionVendorQuoteViewComponent implements OnInit {
  requisitionId!: number;
  quoteId!: number;
  requisition: RequisitionDto | null = null;
  quote: QuoteDto | null = null;
  loading = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private quoteService: RequisitionQuotesModuleService,
    private requisitionService: RequisitionModuleService,
    private toastService: ToastService
  ) {}

  ngOnInit(): void {
    this.requisitionId = Number(this.route.snapshot.paramMap.get('id'));
    this.quoteId = Number(this.route.snapshot.paramMap.get('quoteId'));

    if (!this.requisitionId || !this.quoteId) {
      this.router.navigate(['/requisitions']);
      return;
    }

    this.loadData();
  }

  loadData(): void {
    this.loading = true;

    this.requisitionService.getRequestById(this.requisitionId).subscribe({
      next: (req: RequisitionDto) => {
        this.requisition = req;
      },
      error: (err: any) => {
        console.error('Failed to load requisition', err);
        this.toastService.showError(extractErrorMessage(err, 'Failed to load requisition'));
      }
    });

    this.quoteService.getQuote(this.requisitionId, this.quoteId).subscribe({
      next: (q: QuoteDto) => {
        this.quote = q;
        this.loading = false;
      },
      error: (err: any) => {
        console.error('Failed to load quote details', err);
        this.toastService.showError(extractErrorMessage(err, 'Failed to load quote details'));
        this.router.navigate([`/requisitions/${this.requisitionId}/vendor-quotes`]);
        this.loading = false;
      }
    });
  }

  getLinkedItemName(requestItemId: number | null | undefined): string {
    if (!requestItemId || !this.requisition?.items) {
      return 'No direct link';
    }

    const item = this.requisition.items.find(i => i.id === requestItemId);
    if (!item) return 'Linked item not found';

    const unit = item.unit ? item.unit.charAt(0).toUpperCase() + item.unit.slice(1).toLowerCase() : '';
    return `${item.name} (Qty: ${item.quantity} ${unit})`;
  }

  edit(): void {
    this.router.navigate([`/requisitions/${this.requisitionId}/vendor-quotes/edit/${this.quoteId}`]);
  }

  back(): void {
    this.router.navigate([`/requisitions/${this.requisitionId}/vendor-quotes`]);
  }
}
