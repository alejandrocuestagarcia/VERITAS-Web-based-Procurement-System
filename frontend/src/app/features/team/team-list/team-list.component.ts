import { Component, OnInit, ViewChild, AfterViewInit } from '@angular/core';
import { Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import { MatTableDataSource } from '@angular/material/table';
import { SharedTableComponent } from '../../../shared/components/table/shared-table.component';
import {
  ProjectDto,
  ProjectModuleService,
  TeamDto,
  TeamsModuleService
} from '../../../core/api';

type DepartmentFilter = 'all' | 'it' | 'rd' | 'hr' | 'sales' | 'legal';

interface TeamRow {
  id: number;
  name: string;
  departmentLabel: string;
  departmentFilter: DepartmentFilter;
  projectsText: string;
  statusLabel: 'ACTIVE' | 'ON HOLD';
  isActive: boolean;
  membersCount: number;
  icon: string;
}

@Component({
  selector: 'app-team-list',
  templateUrl: './team-list.component.html',
  styleUrls: ['./team-list.component.scss']
})
export class TeamListComponent implements OnInit, AfterViewInit {
  @ViewChild(SharedTableComponent) sharedTable!: SharedTableComponent;

  readonly departmentFilters: Array<{ key: DepartmentFilter; label: string }> = [
    { key: 'it', label: 'Technology' },
    { key: 'rd', label: 'Research and Development' },
    { key: 'hr', label: 'Human Resources' },
    { key: 'sales', label: 'Sales' },
    { key: 'legal', label: 'Legal' }
  ];

  displayedColumns: string[] = ['identity', 'department', 'projects', 'status', 'actions'];
  dataSource = new MatTableDataSource<TeamRow>([]);

  loading = false;
  error: string | null = null;

  activeDepartmentFilter: DepartmentFilter = 'all';
  private currentSearchString = "";

  totalTeams = 0;
  totalProjects = 0;
  totalDepartments = 0;

  constructor(
    private teamsService: TeamsModuleService,
    private projectService: ProjectModuleService,
    private router: Router
  ) { }

  ngOnInit(): void {
    this.loadData();
  }

  ngAfterViewInit(): void {
    if (this.sharedTable) {
      this.dataSource.paginator = this.sharedTable.paginator;
    }
    this.dataSource.filterPredicate = (data: TeamRow, filter: string) => {
      const searchTerms = JSON.parse(filter);

      const matchesDepartment = searchTerms.department === 'all' || data.departmentFilter === searchTerms.department;
      const matchesSearch = !searchTerms.search
                            || data.name.toLowerCase().includes(searchTerms.search)
                            || data.projectsText.toLowerCase().includes(searchTerms.search);

      return matchesDepartment && matchesSearch;
    };
    // Initialize the filter so it shows everything first
    this.applyFilters();
  }

  onSearchChanged(value: string): void {
    this.currentSearchString = value.trim().toLowerCase();
    this.applyFilters();
  }

  onDepartmentFilterChange(value: any): void {
    this.activeDepartmentFilter = value ?? 'all';
    this.applyFilters();
  }

  private applyFilters() {
    this.dataSource.filter = JSON.stringify({
      department: this.activeDepartmentFilter,
      search: this.currentSearchString
    });
    if (this.dataSource.paginator) {
      this.dataSource.paginator.firstPage();
    }
  }

  navigateToAddTeam(): void {
    this.router.navigate(['/teams/create']);
  }

  private loadData(): void {
    this.loading = true;
    this.error = null;

    forkJoin({
      teams: this.teamsService.getAllTeams(),
      projects: this.projectService.getAllProjects()
    }).subscribe({
      next: ({ teams, projects }) => {
        const normalizedTeams = this.toArray<TeamDto>(teams);
        const normalizedProjects = this.toArray<ProjectDto>(projects);

        const rows = this.buildTeamRows(normalizedTeams, normalizedProjects);
        this.dataSource.data = rows;
        this.totalTeams = rows.length;
        this.totalProjects = normalizedProjects.length;
        this.totalDepartments = this.countUniqueDepartments(rows);

        this.applyFilters();
        this.loading = false;
      },
      error: () => {
        this.error = 'Failed to load team governance data.';
        this.loading = false;
      }
    });
  }

  private buildTeamRows(teams: TeamDto[], projects: ProjectDto[]): TeamRow[] {
    const projectsByTeam = this.groupProjectsByTeam(projects);

    return teams.map((team, index) => {
      const teamName = this.getSafeTeamName(team.name, index);
      const department = this.mapDepartment(team.department);
      const projectNames = projectsByTeam.get(teamName.toLowerCase()) ?? [];
      const isActive = team.isActive !== false;

      return {
        id: team.id ?? index + 1,
        name: teamName,
        departmentLabel: department.label,
        departmentFilter: department.filter,
        projectNames: projectNames,
        projectsText: this.formatProjects(projectNames),
        statusLabel: isActive ? 'ACTIVE' : 'ON HOLD',
        isActive,
        membersCount: team.members?.length ?? 0,
        icon: this.resolveIcon(department.filter)
      };
    });
  }

  private countUniqueDepartments(rows: TeamRow[]): number {
    return new Set(rows.map((row) => row.departmentLabel)).size;
  }

  private getSafeTeamName(rawName: string | undefined, index: number): string {
    const trimmed = rawName?.trim();
    return trimmed && trimmed.length > 0 ? trimmed : `Team ${index + 1}`;
  }

  private groupProjectsByTeam(projects: ProjectDto[]): Map<string, string[]> {
    const projectMap = new Map<string, string[]>();

    projects.forEach((project) => {
      const teamName = project.teamName?.trim().toLowerCase();
      const projectName = project.name?.trim();

      if (!teamName || !projectName) {
        return;
      }

      const current = projectMap.get(teamName) ?? [];
      current.push(projectName);
      projectMap.set(teamName, current);
    });

    return projectMap;
  }

  private mapDepartment(rawDepartment: string | undefined): {
    label: string;
    filter: DepartmentFilter;
  } {
    const normalized = (rawDepartment ?? '').toUpperCase();

    switch (normalized) {
      case 'IT':
        return { label: 'IT', filter: 'it' };
      case 'RD':
        return { label: 'R&D', filter: 'rd' };
      case 'HR':
        return { label: 'HR', filter: 'hr' };
      case 'SALES':
        return { label: 'Sales', filter: 'sales' };
      case 'LEGAL':
        return { label: 'Legal', filter: 'legal' };
      default:
        return { label: this.humanizeDepartment(normalized), filter: 'all' };
    }
  }

  private humanizeDepartment(department: string): string {
    if (!department) {
      return 'Unassigned';
    }

    return department
      .toLowerCase()
      .split('_')
      .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
      .join(' ');
  }

  private formatProjects(projectNames: string[]): string {
    if (projectNames.length === 0) {
      return 'No projects assigned';
    }

    if (projectNames.length === 1) {
      return projectNames[0];
    }

    if (projectNames.length === 2) {
      return `${projectNames[0]}, ${projectNames[1]}`;
    }

    return `${projectNames[0]}, ...`;
  }

  private resolveIcon(filter: DepartmentFilter): string {
    switch (filter) {
      case 'it':
        return 'dns';
      case 'rd':
        return 'science';
      case 'hr':
        return 'badge';
      case 'sales':
        return 'trending_up';
      case 'legal':
        return 'gavel';
      default:
        return 'groups';
    }
  }

  private toArray<T>(value: unknown): T[] {
    if (Array.isArray(value)) {
      return value as T[];
    }

    if (!value || typeof value !== 'object') {
      return [];
    }

    const wrappers = ['content', 'items', 'data', 'results'];
    for (const key of wrappers) {
      const candidate = (value as Record<string, unknown>)[key];
      if (Array.isArray(candidate)) {
        return candidate as T[];
      }
    }

    return [];
  }
}
