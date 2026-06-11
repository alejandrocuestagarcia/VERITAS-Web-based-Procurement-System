import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { RequisitionModuleService, RequisitionDto, RequisitionQuotesModuleService, QuoteDto } from 'src/app/core/api';
import { ToastService } from '../../../../core/services/toast.service';

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
        this.toastService.showError('Failed to load requisition details');
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
          const maxQuoteAmount = Math.max(...this.quotes.map(q => this.convertAmount(q.totalAmount, q.currency)));
          this.budgetCeiling = Math.round(maxQuoteAmount * 1.15 / 1000) * 1000;
        }
        this.loading = false;
      },
      error: (err) => {
        console.error('Failed to load quotes', err);
        this.toastService.showError('Failed to load vendor quotes');
        this.loading = false;
      }
    });
  }

  convertAmount(amount: number | undefined, currency: string | undefined): number {
    if (amount === undefined) return 0;
    if (!currency || currency === 'EUR') return amount;

    const rates: Record<string, number> = {
      'USD': 0.92,
      'GBP': 1.17,
      'CHF': 1.03,
      'JPY': 0.006
    };

    const rate = rates[currency.toUpperCase()] || 1.0;
    return amount * rate;
  }

  getRecommendedQuote(): QuoteDto | null {
    if (!this.quotes || this.quotes.length === 0) return null;
    return this.quotes.reduce((prev, curr) => {
      const prevConverted = this.convertAmount(prev.totalAmount, prev.currency);
      const currConverted = this.convertAmount(curr.totalAmount, curr.currency);
      return prevConverted < currConverted ? prev : curr;
    });
  }

  get totalQuantity(): number {
    return this.requisition?.items?.reduce((sum, item) => sum + (item.quantity || 0), 0) || 0;
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
          this.toastService.showError('Failed to select quote');
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
