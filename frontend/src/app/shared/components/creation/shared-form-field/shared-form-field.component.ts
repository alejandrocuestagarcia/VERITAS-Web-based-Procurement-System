import { Component, Input, HostBinding } from '@angular/core';

@Component({
  selector: 'app-shared-form-field',
  templateUrl: './shared-form-field.component.html',
  styleUrls: ['./shared-form-field.component.scss'],
})
export class SharedFormFieldComponent {
  @Input() label: string = '';
  @Input() fullWidth: boolean = false;
  @Input() required: boolean = false;

  @HostBinding('class.md:col-span-2') get mdColSpan2() { return this.fullWidth; }
  @HostBinding('class.col-span-2') get colSpan2() { return this.fullWidth; }
  @HostBinding('class.block') get block() { return true; }
}
