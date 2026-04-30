import { ComponentFixture, TestBed } from '@angular/core/testing';

import { WorkflowViewerComponent } from './workflow-viewer.component';

describe('WorkflowViewerComponent', () => {
  let component: WorkflowViewerComponent;
  let fixture: ComponentFixture<WorkflowViewerComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [WorkflowViewerComponent]
    });
    fixture = TestBed.createComponent(WorkflowViewerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
