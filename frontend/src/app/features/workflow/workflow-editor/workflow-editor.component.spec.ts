import { ComponentFixture, TestBed } from '@angular/core/testing';

import { WorkflowEditorComponent } from './workflow-editor.component';

describe('WorkflowEditorComponent', () => {
  let component: WorkflowEditorComponent;
  let fixture: ComponentFixture<WorkflowEditorComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [WorkflowEditorComponent]
    });
    fixture = TestBed.createComponent(WorkflowEditorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
