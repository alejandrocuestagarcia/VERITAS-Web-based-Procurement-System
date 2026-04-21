// AI-GENERATED

import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { RouterTestingModule } from '@angular/router/testing';
import { of } from 'rxjs';

import {
  TeamsModuleService,
  UserModuleService
} from '../../core/api';
import { TeamCreateComponent } from './team-create.component';

describe('TeamCreateComponent', () => {
  let component: TeamCreateComponent;
  let fixture: ComponentFixture<TeamCreateComponent>;

  const userModuleServiceStub = {
    getAllUsers: () => of([
      {
        id: 11,
        name: 'Jonathan Doe',
        email: 'jonathan.doe@veritas.com',
        role: 'FINANCE_OFFICER'
      }
    ])
  };

  const teamsModuleServiceStub = {
    createTeam: () => of({ id: 1 })
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [TeamCreateComponent],
      imports: [FormsModule, RouterTestingModule, MatSnackBarModule],
      providers: [
        { provide: UserModuleService, useValue: userModuleServiceStub },
        { provide: TeamsModuleService, useValue: teamsModuleServiceStub }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();

    fixture = TestBed.createComponent(TeamCreateComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load lead options', () => {
    expect(component.leadOptions.length).toBe(1);
    expect(component.leadOptions[0].displayName).toBe('Jonathan Doe');
  });

  it('should expose selected lead as owner member', () => {
    component.model.leaderId = 11;
    expect(component.leadMember?.displayName).toBe('Jonathan Doe');
  });
});
