import {Component, Input} from '@angular/core';

@Component({
  selector: 'app-shared-form-card',
  templateUrl: './shared-form-card.component.html',
  styleUrls: ['./shared-form-card.component.scss'],
})
export class SharedFormCardComponent {
  @Input() icon: string = '';
  @Input() title: string = '';
}
