import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HeaderComponent } from './header.component';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RequisitionModuleService } from 'src/app/core/api';
import { AuthService } from '../../services/auth.service';
import { of } from 'rxjs';
import { NO_ERRORS_SCHEMA } from '@angular/core';

describe('HeaderComponent', () => {
  let component: HeaderComponent;
  let fixture: ComponentFixture<HeaderComponent>;

  beforeEach(async () => {
    const requisitionServiceSpy = jasmine.createSpyObj('RequisitionModuleService', ['getRequests']);
    requisitionServiceSpy.getRequests.and.returnValue(of({ content: [], totalElements: 0 }));

    const authServiceSpy = jasmine.createSpyObj('AuthService', ['getUsername', 'getRole', 'getName', 'isLoggedIn', 'getDecodedToken']);
    authServiceSpy.getUsername.and.returnValue('test@veritas.com');
    authServiceSpy.getRole.and.returnValue('REQUESTER');
    authServiceSpy.getName.and.returnValue('Test User');
    authServiceSpy.isLoggedIn.and.returnValue(true);
    authServiceSpy.getDecodedToken.and.returnValue({ sub: 'test@veritas.com', name: 'Test User', role: 'REQUESTER' });

    await TestBed.configureTestingModule({
      imports: [
        HttpClientTestingModule,
        RouterTestingModule,
        ReactiveFormsModule,
        FormsModule,
        MatIconModule,
        MatTooltipModule
      ],
      declarations: [HeaderComponent],
      providers: [
        { provide: RequisitionModuleService, useValue: requisitionServiceSpy },
        { provide: AuthService, useValue: authServiceSpy }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();

    fixture = TestBed.createComponent(HeaderComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
