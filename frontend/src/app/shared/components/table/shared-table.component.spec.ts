import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SharedTableComponent } from './shared-table.component';

describe('TableComponent', () => {
  let component: SharedTableComponent;
  let fixture: ComponentFixture<SharedTableComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [SharedTableComponent]
    });
    fixture = TestBed.createComponent(SharedTableComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
