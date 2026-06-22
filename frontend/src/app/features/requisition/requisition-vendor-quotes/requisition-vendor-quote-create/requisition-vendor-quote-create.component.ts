import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, FormGroup, FormArray, Validators } from '@angular/forms';
import { RequisitionModuleService, RequisitionDto, VendorModuleService, VendorDto, QuoteCreateDto, RequisitionQuotesModuleService, QuoteDto } from '../../../../core/api';
import { ToastService } from '../../../../core/services/toast.service';
import { extractErrorMessage } from '../../../../shared/utils/error-utils';

export enum Currency {
  EUR = 'EUR',
  USD = 'USD',
  GBP = 'GBP',
  CHF = 'CHF',
  JPY = 'JPY'
}

@Component({
  selector: 'app-requisition-vendor-quote-create',
  templateUrl: './requisition-vendor-quote-create.component.html',
})
export class RequisitionVendorQuoteCreateComponent implements OnInit {
  requisitionId!: number;
  quoteId: number | null = null;
  isEditMode = false;
  requisition: RequisitionDto | null = null;
  vendors: VendorDto[] = [];
  quoteForm!: FormGroup;
  loading = false;
  submitting = false;
  error: string | null = null;

  currencies = Object.values(Currency);

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private fb: FormBuilder,
    private quoteService: RequisitionQuotesModuleService,
    private requisitionService: RequisitionModuleService,
    private vendorService: VendorModuleService,
    private toastService: ToastService
  ) {}

  ngOnInit(): void {
    this.requisitionId = Number(this.route.snapshot.paramMap.get('id'));
    const quoteIdParam = this.route.snapshot.paramMap.get('quoteId');
    if (quoteIdParam) {
      this.isEditMode = true;
      this.quoteId = Number(quoteIdParam);
    }

    if (!this.requisitionId) {
      this.router.navigate(['/requisitions']);
      return;
    }

    this.initForm();
    this.loadData();
  }

  initForm(): void {
    this.quoteForm = this.fb.group({
      vendorId: ['', Validators.required],
      currency: [Currency.EUR, [Validators.required, Validators.maxLength(3)]],
      baseAmount: [0, [Validators.required, Validators.min(1)]],
      shippingCosts: [0, [Validators.required, Validators.min(0)]],
      totalAmount: [0, [Validators.required, Validators.min(1)]],
      shippingTime: [null, [Validators.required, Validators.min(0)]],
      items: this.fb.array([])
    });

    this.quoteForm.get('baseAmount')?.valueChanges.subscribe(() => this.calculateTotal());
    this.quoteForm.get('shippingCosts')?.valueChanges.subscribe(() => this.calculateTotal());
  }

  get itemsFormArray(): FormArray {
    return this.quoteForm.get('items') as FormArray;
  }

  addItem(description = '', quantity = 1, unitPrice = 0, requestItemId: number | null = null): void {
    const itemGroup = this.fb.group({
      productDescription: [description, Validators.required],
      quantity: [quantity, [Validators.required, Validators.min(1)]],
      unitPrice: [unitPrice, [Validators.required, Validators.min(1)]],
      requestItemId: [requestItemId]
    });

    itemGroup.valueChanges.subscribe(() => this.calculateBaseAmountFromItems());

    itemGroup.get('requestItemId')?.valueChanges.subscribe((id: number | null) => {
      if (!id || !this.requisition?.items) return;

      const reqItem = this.requisition.items.find((r: any) => r.id === id);
      if (!reqItem) return;

      itemGroup.patchValue({
        productDescription: reqItem.name,
        quantity: reqItem.quantity,
        unitPrice: 0
      }, { emitEvent: false });

      this.calculateBaseAmountFromItems();
    });

    this.itemsFormArray.push(itemGroup);
    this.calculateBaseAmountFromItems();
  }

  removeItem(index: number): void {
    this.itemsFormArray.removeAt(index);
    this.calculateBaseAmountFromItems();
  }

  calculateBaseAmountFromItems(): void {
    let base = 0;
    this.itemsFormArray.controls.forEach((ctrl) => {
      const qty = ctrl.get('quantity')?.value || 0;
      const price = ctrl.get('unitPrice')?.value || 0;
      base += qty * price;
    });

    this.quoteForm.get('baseAmount')?.setValue(base, { emitEvent: false });
    this.calculateTotal();
  }

  calculateTotal(): void {
    const base = this.quoteForm.get('baseAmount')?.value || 0;
    const shipping = this.quoteForm.get('shippingCosts')?.value || 0;
    this.quoteForm.get('totalAmount')?.setValue(base + shipping, { emitEvent: false });
  }

  loadData(): void {
    this.loading = true;

    this.vendorService.getAllVendors({ page: 0, size: 1000 }, '', 0.0).subscribe({
      next: (res: any) => {
        if (res && Array.isArray(res)) {
          this.vendors = res;
        } else if (res && Array.isArray(res.content)) {
          this.vendors = res.content;
        } else {
          this.vendors = [];
        }

        this.requisitionService.getRequestById(this.requisitionId).subscribe({
          next: (req: RequisitionDto) => {
            this.requisition = req;

            if (this.isEditMode && this.quoteId) {
              this.loadQuote();
            } else {
              if (this.requisition.items && this.requisition.items.length > 0) {
                this.requisition.items.forEach((reqItem: any) => {
                  this.addItem(reqItem.name, reqItem.quantity, 0, reqItem.id);
                });
              }
              this.loading = false;
            }
          },
          error: (err: any) => {
            console.error('Failed to load requisition', err);
            this.toastService.showError(extractErrorMessage(err, 'Failed to load requisition'));
            this.loading = false;
          }
        });
      },
      error: (err: any) => {
        console.error('Failed to load vendors', err);
        this.toastService.showError(extractErrorMessage(err, 'Failed to load vendors'));
        this.loading = false;
      }
    });
  }

  loadQuote(): void {
    this.quoteService.getQuote(this.requisitionId, this.quoteId!).subscribe({
      next: (quote: QuoteDto) => {
        this.quoteForm.patchValue({
          vendorId: quote.vendorId,
          currency: quote.currency,
          baseAmount: quote.baseAmount,
          shippingCosts: quote.shippingCosts,
          totalAmount: quote.totalAmount,
          shippingTime: quote.shippingTime
        });

        while (this.itemsFormArray.length !== 0) {
          this.itemsFormArray.removeAt(0);
        }

        if (quote.items && quote.items.length > 0) {
          quote.items.forEach((item: any) => {
            this.addItem(item.productDescription, item.quantity, item.unitPrice, item.requestItemId);
          });
        }

        this.loading = false;
      },
      error: (err: any) => {
        console.error('Failed to load quote details', err);
        this.toastService.showError(extractErrorMessage(err, 'Failed to load quote details'));
        this.router.navigate([`/requisitions/${this.requisitionId}/vendor-quotes`]);
      }
    });
  }

  submit(): void {
    this.error = null;
    if (this.quoteForm.invalid) {
      this.error = 'Please complete all required fields correctly.';
      this.quoteForm.markAllAsTouched();
      return;
    }

    this.submitting = true;
    const formVal = this.quoteForm.value;

    const payload: QuoteCreateDto = {
      vendorId: Number(formVal.vendorId),
      currency: formVal.currency,
      baseAmount: formVal.baseAmount,
      shippingCosts: formVal.shippingCosts,
      totalAmount: formVal.totalAmount,
      shippingTime: Number(formVal.shippingTime),
      items: formVal.items.map((item: any) => ({
        productDescription: item.productDescription,
        quantity: Number(item.quantity),
        unitPrice: Number(item.unitPrice),
        requestItemId: item.requestItemId ? Number(item.requestItemId) : null
      }))
    };

    if (this.isEditMode && this.quoteId) {
      this.quoteService.updateQuote(this.requisitionId, this.quoteId, payload).subscribe({
        next: () => {
          this.toastService.showSuccess('Quote updated successfully');
          this.router.navigate([`/requisitions/${this.requisitionId}/vendor-quotes`]);
        },
        error: (err: any) => {
          console.error('Failed to update quote', err);
          this.error = extractErrorMessage(err, 'Failed to update quote');
          this.submitting = false;
        }
      });
    } else {
      this.quoteService.createQuote(this.requisitionId, payload).subscribe({
        next: () => {
          this.toastService.showSuccess('Quote created successfully');
          this.router.navigate([`/requisitions/${this.requisitionId}/vendor-quotes`]);
        },
        error: (err: any) => {
          console.error('Failed to create quote', err);
          this.error = extractErrorMessage(err, 'Failed to create quote');
          this.submitting = false;
        }
      });
    }
  }

  cancel(): void {
    this.router.navigate([`/requisitions/${this.requisitionId}/vendor-quotes`]);
  }

}
