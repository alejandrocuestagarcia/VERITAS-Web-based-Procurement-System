import { Component, Input } from '@angular/core';
import { FormGroup } from '@angular/forms';

@Component({
  selector: 'app-shared-rating-slider',
  templateUrl: './shared-rating-slider.component.html',
})
export class SharedRatingSliderComponent {
  @Input() form!: FormGroup;
  @Input() controlName!: string;
  @Input() label!: string;
  @Input() icon!: string;
}
