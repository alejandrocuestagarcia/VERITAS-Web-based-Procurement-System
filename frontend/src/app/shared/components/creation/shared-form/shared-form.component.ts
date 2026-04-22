import {Component, EventEmitter, Input, Output} from '@angular/core';

@Component({
  selector: 'app-shared-form',
  templateUrl: './shared-form.component.html',
  styleUrls: ['./shared-form.component.scss'],
})
export class SharedFormComponent {
  @Input() title: string = '';
  @Input() subtitle: string = '';
  @Input() routingSubtitle: string = '';
  @Input() submitButtonLabel: string = 'Save';
  @Input() submitButtonIcon: string = '';
  @Input() loading: boolean = false;

  @Output() submitted = new EventEmitter<void>();
  @Output() cancelled = new EventEmitter<void>();
}
