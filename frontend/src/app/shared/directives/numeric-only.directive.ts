import { Directive, ElementRef, HostListener, Input } from '@angular/core';

@Directive({
  selector: '[appNumericOnly]',
  standalone: true
})
export class NumericOnlyDirective {
  @Input() appNumericOnly: 'integer' | 'decimal' = 'decimal';

  constructor(private el: ElementRef<HTMLInputElement>) {}

  @HostListener('keydown', ['$event'])
  onKeyDown(event: KeyboardEvent): void {
    if (this.appNumericOnly === 'integer') {
      this.handleInteger(event);
    } else {
      this.handleDecimal(event);
    }
  }

  @HostListener('paste', ['$event'])
  onPaste(event: ClipboardEvent): void {
    const pasted = event.clipboardData?.getData('text') ?? '';
    const regex = this.appNumericOnly === 'integer'
      ? /^\d+$/
      : /^\d*\.?\d*$/;
    if (!regex.test(pasted)) {
      event.preventDefault();
    }
  }

  private handleInteger(event: KeyboardEvent): void {
    if (['Backspace', 'Delete', 'ArrowLeft', 'ArrowRight', 'ArrowUp', 'ArrowDown', 'Tab', 'Home', 'End'].includes(event.key)) return;
    if (/^\d$/.test(event.key)) return;
    event.preventDefault();
  }

  private handleDecimal(event: KeyboardEvent): void {
    if (['Backspace', 'Delete', 'ArrowLeft', 'ArrowRight', 'ArrowUp', 'ArrowDown', 'Tab', 'Home', 'End', '.'].includes(event.key)) return;
    if (/^\d$/.test(event.key)) return;
    event.preventDefault();
  }
}
