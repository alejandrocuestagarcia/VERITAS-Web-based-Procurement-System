// AI-GENERATED

import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { of } from 'rxjs';

import {
  ProjectModuleService,
  TeamsModuleService
} from '../../core/api';
import { TeamManagementComponent } from './team-management.component';

describe('TeamManagementComponent', () => {
  let component: TeamManagementComponent;
  let fixture: ComponentFixture<TeamManagementComponent>;

  const teamsModuleServiceStub = {
    getAllTeams: () =>
      of([
        {
          id: 1,
          name: 'Cloud Infrastructure',
          department: 'IT',
          isActive: true
        },
        {
          id: 2,
          name: 'Internal Audit Alpha',
          department: 'LEGAL',
          isActive: false
        }
      ])
  };

  const projectModuleServiceStub = {
    getAllProjects: () =>
      of([
        {
          id: 1,
          name: 'Data Lake Migration',
          teamName: 'Cloud Infrastructure'
        }
      ])
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [TeamManagementComponent],
      imports: [RouterTestingModule],
      providers: [
        { provide: TeamsModuleService, useValue: teamsModuleServiceStub },
        { provide: ProjectModuleService, useValue: projectModuleServiceStub }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();

    fixture = TestBed.createComponent(TeamManagementComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should derive project labels and default members count', () => {
    expect(component.pagedRows.length).toBe(2);
    expect(component.pagedRows[0].projectsText).toBe('Data Lake Migration');
    expect(component.pagedRows[0].membersCount).toBe(0);
  });

  it('should filter by department and search term', () => {
    component.setDepartmentFilter('it');
    expect(component.filteredRows.length).toBe(1);

    component.updateSearch('data lake');
    expect(component.filteredRows.length).toBe(1);

    component.updateSearch('non-existing-query');
    expect(component.filteredRows.length).toBe(0);
  });
});
