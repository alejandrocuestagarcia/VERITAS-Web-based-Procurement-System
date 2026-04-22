import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SharedFormCardComponent } from './shared-form-card.component';

describe('SharedFormCardComponent', () => {
  let component: SharedFormCardComponent;
  let fixture: ComponentFixture<SharedFormCardComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [SharedFormCardComponent]
    });
    fixture = TestBed.createComponent(SharedFormCardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
