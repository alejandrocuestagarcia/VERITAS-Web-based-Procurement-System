import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ProjectCreateComponent } from './project-create.component';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ToastService } from '../../../core/services/toast.service';

describe('ProjectCreateComponent', () => {
  let component: ProjectCreateComponent;
  let fixture: ComponentFixture<ProjectCreateComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [ProjectCreateComponent],
      imports: [HttpClientTestingModule, ReactiveFormsModule, MatSnackBarModule],
      providers: [
        { provide: ToastService, useValue: { showSuccess: jasmine.createSpy(), showError: jasmine.createSpy() } }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    });
    fixture = TestBed.createComponent(ProjectCreateComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
