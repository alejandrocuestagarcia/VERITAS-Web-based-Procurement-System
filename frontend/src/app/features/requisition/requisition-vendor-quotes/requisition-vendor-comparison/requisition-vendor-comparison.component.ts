import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { RequisitionModuleService, RequisitionDto, RequisitionQuotesModuleService, QuoteDto, RecommendedQuoteDto } from 'src/app/core/api';
import { ToastService } from '../../../../core/services/toast.service';
import { extractErrorMessage } from '../../../../shared/utils/error-utils';
import { MatDialog } from '@angular/material/dialog';
import { ConfirmationDialogComponent } from '../../../../shared/components/confirmation-dialog/confirmation-dialog.component';

@Component({
  selector: 'app-requisition-vendor-comparison',
  templateUrl: './requisition-vendor-comparison.component.html',
  styleUrls: ['./requisition-vendor-comparison.component.scss']
})
export class RequisitionVendorComparisonComponent implements OnInit {
  requestId!: number;
  requisition: RequisitionDto | null = null;
  quotes: QuoteDto[] = [];
  recommendations: RecommendedQuoteDto[] = [];
  recommendedQuote: QuoteDto | null = null;
  budgetCeiling = 0;
  loading = true;
  hasInvoice = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private requisitionService: RequisitionModuleService,
    private quotesService: RequisitionQuotesModuleService,
    private toastService: ToastService,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    const idStr = this.route.snapshot.paramMap.get('id');
    if (idStr) {
      this.requestId = Number(idStr);
      this.loadData();
    } else {
      this.router.navigate(['/requisitions']);
    }
  }

  loadData(): void {
    this.loading = true;
    this.requisitionService.getRequestById(this.requestId).subscribe({
      next: (req) => {
        this.requisition = req;
        if (this.requisition.state === 'FINISHED') {
          this.toastService.showError('Cannot compare quotes for a finished request.');
          this.router.navigate([`/requisitions/${this.requestId}`]);
          return;
        }
        this.checkInvoiceExists();
        this.loadQuotes();
      },
      error: (err) => {
        console.error('Failed to load requisition details', err);
        this.toastService.showError(extractErrorMessage(err, 'Failed to load requisition details'));
        this.loading = false;
        this.router.navigate(['/requisitions']);
      }
    });
  }

  checkInvoiceExists(): void {
    this.requisitionService.getInvoice(this.requestId).subscribe({
      next: () => {
        this.hasInvoice = true;
      },
      error: () => {
        this.hasInvoice = false;
      }
    });
  }

  loadQuotes(): void {
    this.loading = true;
    this.quotesService.getQuoteRecommendations(this.requestId).subscribe({
      next: (recommendations) => {
        this.recommendations = recommendations || [];
        this.quotes = this.recommendations.map(r => r.quote).filter((q): q is QuoteDto => !!q);

        const topRec = this.recommendations.find(r => r.rank === 1);
        this.recommendedQuote = topRec?.quote || null;

        if (this.quotes.length > 0) {
          const maxQuoteAmount = Math.max(...this.quotes.map(q => q.totalAmountEuro ?? q.totalAmount ?? 0));
          this.budgetCeiling = Math.round(maxQuoteAmount * 1.15 / 1000) * 1000;
        }
        this.loading = false;
      },
      error: (err) => {
        console.error('Failed to load quote recommendations', err);
        this.toastService.showError(extractErrorMessage(err, 'Failed to load vendor quotes'));
        this.loading = false;
      }
    });
  }

  getRecommendationForQuote(quote: QuoteDto): RecommendedQuoteDto | undefined {
    return this.recommendations.find(r => r.quote?.quoteId === quote.quoteId);
  }

  getScoreColor(score: number | undefined): string {
    if (score === undefined || score === null) return 'bg-gray-200';
    if (score >= 0.8) return 'bg-blue-500';
    if (score >= 0.5) return 'bg-amber-500';
    return 'bg-rose-500';
  }

  getScoreTextColor(score: number | undefined): string {
    if (score === undefined || score === null) return 'text-gray-400';
    if (score >= 0.8) return 'text-blue-600';
    if (score >= 0.5) return 'text-amber-600';
    return 'text-rose-600';
  }

  getExchangeRateLabel(quote: QuoteDto): string | null {
    if (!quote.currency || quote.currency === 'EUR') return null;
    if (!quote.totalAmountEuro || !quote.totalAmount || quote.totalAmountEuro === 0) return null;
    const rate = (quote.totalAmount / quote.totalAmountEuro).toFixed(4);
    const source = (quote as any).exchangeRateSource ? ` · ${(quote as any).exchangeRateSource}` : '';
    return `Rate: 1 EUR = ${rate} ${quote.currency}${source}`;
  }

  getBaseAmountEuro(quote: QuoteDto): number | null {
    if (quote.baseAmount === undefined || quote.baseAmount === null) return null;
    if (quote.currency === 'EUR') return quote.baseAmount;
    if (!quote.totalAmountEuro || !quote.totalAmount || quote.totalAmountEuro === 0) return null;
    const rate = quote.totalAmount / quote.totalAmountEuro;
    return quote.baseAmount / rate;
  }

  getShippingCostsEuro(quote: QuoteDto): number | null {
    if (quote.shippingCosts === undefined || quote.shippingCosts === null) return null;
    if (quote.currency === 'EUR') return quote.shippingCosts;
    if (!quote.totalAmountEuro || !quote.totalAmount || quote.totalAmountEuro === 0) return null;
    const rate = quote.totalAmount / quote.totalAmountEuro;
    return quote.shippingCosts / rate;
  }

  hasNonEuroQuote(): boolean {
    return this.quotes.some(q => q.currency && q.currency !== 'EUR');
  }


  getRatingColor(score: number | undefined): string {
    if (score === undefined || score === null) return 'bg-gray-200';
    if (score >= 8.0) return 'bg-emerald-500';
    if (score >= 5.0) return 'bg-amber-500';
    return 'bg-rose-500';
  }

  getRatingTextColor(score: number | undefined): string {
    if (score === undefined || score === null) return 'text-gray-400';
    if (score >= 8.0) return 'text-emerald-600';
    if (score >= 5.0) return 'text-amber-600';
    return 'text-rose-600';
  }

  getLeadTimeDays(quote: QuoteDto): number {
    return quote.shippingTime ?? 0;
  }

  getVendorInitials(name: string | undefined): string {
    if (!name) return 'V';
    return name.split(' ').map(n => n[0]).join('').substring(0, 2).toUpperCase();
  }

  selectQuote(quote: QuoteDto): void {
    if (quote.quoteId && this.requisition?.id) {
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
  }

  private executeQuoteSelection(quote: QuoteDto): void {
    this.loading = true;
    this.quotesService.selectQuote(this.requisition!.id!, quote.quoteId!).subscribe({
      next: () => {
        this.toastService.showSuccess('Quote selected successfully');
        this.router.navigate(['/requisitions', this.requisition!.id, 'vendor-quotes']);
      },
      error: (err) => {
        console.error('Failed to select quote', err);
        this.toastService.showError(extractErrorMessage(err, 'Failed to select quote'));
        this.loading = false;
      }
    });
  }

  close(): void {
    if (this.requisition?.id) {
      this.router.navigate(['/requisitions', this.requisition.id, 'vendor-quotes']);
    } else {
      this.router.navigate(['/requisitions']);
    }
  }

  getTooltipText(rec: RecommendedQuoteDto): string {
    const pricePercent = rec.priceScore != null ? Math.round(rec.priceScore * 100) : 0;
    const vendorPercent = rec.vendorScore != null ? Math.round(rec.vendorScore * 100) : 0;
    const leadTimePercent = rec.leadTimeScore != null ? Math.round(rec.leadTimeScore * 100) : 0;

    const priceSuffix = pricePercent === 100 ? ' (Cheapest)' : '';
    const leadTimeSuffix = leadTimePercent === 100 ? ' (Fastest)' : '';
    const vendorSuffix = vendorPercent === 100 ? ' (Best)' : '';

    return `Breakdown:\n` +
           `• Price Score: ${pricePercent}%${priceSuffix}\n` +
           `• Vendor Score: ${vendorPercent}%${vendorSuffix}\n` +
           `• Lead Time Score: ${leadTimePercent}%${leadTimeSuffix}`;
  }
}
