import {Component, Input} from '@angular/core';

@Component({
  selector: 'app-shared-form-field',
  templateUrl: './shared-form-field.component.html',
  styleUrls: ['./shared-form-field.component.scss'],
})
export class SharedFormFieldComponent {
  @Input() label: string = '';
  @Input() fullWidth: boolean = false;
}
