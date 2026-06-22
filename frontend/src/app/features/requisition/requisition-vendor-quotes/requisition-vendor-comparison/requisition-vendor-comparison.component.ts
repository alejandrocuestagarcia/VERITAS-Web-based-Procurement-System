import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { RequisitionModuleService, RequisitionDto, RequisitionQuotesModuleService, QuoteDto } from 'src/app/core/api';
import { ToastService } from '../../../../core/services/toast.service';
import { extractErrorMessage } from '../../../../shared/utils/error-utils';

@Component({
  selector: 'app-requisition-vendor-comparison',
  templateUrl: './requisition-vendor-comparison.component.html',
  styleUrls: ['./requisition-vendor-comparison.component.scss']
})
export class RequisitionVendorComparisonComponent implements OnInit {
  requestId!: number;
  requisition: RequisitionDto | null = null;
  quotes: QuoteDto[] = [];
  recommendedQuote: QuoteDto | null = null;
  budgetCeiling = 0;
  loading = true;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private requisitionService: RequisitionModuleService,
    private quotesService: RequisitionQuotesModuleService,
    private toastService: ToastService
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

  loadQuotes(): void {
    this.quotesService.getQuotesForRequest(this.requestId).subscribe({
      next: (quotes) => {
        this.quotes = quotes || [];
        if (this.quotes.length > 0) {
          this.recommendedQuote = this.getRecommendedQuote();
          const maxQuoteAmount = Math.max(...this.quotes.map(q => q.totalAmountEuro ?? q.totalAmount ?? 0));
          this.budgetCeiling = Math.round(maxQuoteAmount * 1.15 / 1000) * 1000;
        }
        this.loading = false;
      },
      error: (err) => {
        console.error('Failed to load quotes', err);
        this.toastService.showError(extractErrorMessage(err, 'Failed to load vendor quotes'));
        this.loading = false;
      }
    });
  }

  getRecommendedQuote(): QuoteDto | null {
    if (!this.quotes || this.quotes.length === 0) return null;
    return this.quotes.reduce((prev, curr) => {
      const prevEuro = prev.totalAmountEuro ?? prev.totalAmount ?? 0;
      const currEuro = curr.totalAmountEuro ?? curr.totalAmount ?? 0;
      return prevEuro < currEuro ? prev : curr;
    });
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
      this.loading = true;
      this.quotesService.selectQuote(this.requisition.id, quote.quoteId).subscribe({
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
  }

  close(): void {
    if (this.requisition?.id) {
      this.router.navigate(['/requisitions', this.requisition.id, 'vendor-quotes']);
    } else {
      this.router.navigate(['/requisitions']);
    }
  }
}
