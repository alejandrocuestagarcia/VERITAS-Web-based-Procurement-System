import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import {
  ProjectDto,
  ProjectModuleService,
  TeamCreateDtoDepartmentEnum,
  TeamDto,
  TeamsModuleService
} from '../../core/api';

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
  selector: 'app-team-management',
  templateUrl: './team-management.component.html',
  styleUrls: ['./team-management.component.scss']
})
export class TeamManagementComponent implements OnInit {
  readonly pageSize = 10;
  readonly departmentFilters: Array<{ key: DepartmentFilter; label: string }> = [
    { key: 'all', label: 'All Teams' },
    { key: 'it', label: 'Technology' },
    { key: 'rd', label: 'Research and Development' },
    { key: 'hr', label: 'Human Resources' },
    { key: 'sales', label: 'Sales' },
    { key: 'legal', label: 'Legal' }
  ];

  loading = false;
  error: string | null = null;

  searchTerm = '';
  activeDepartmentFilter: DepartmentFilter = 'all';
  currentPage = 1;

  totalTeams = 0;
  totalProjects = 0;
  totalDepartments = 0;

  rows: TeamRow[] = [];
  filteredRows: TeamRow[] = [];
  pagedRows: TeamRow[] = [];

  constructor(
    private teamsService: TeamsModuleService,
    private projectService: ProjectModuleService,
    private router: Router
  ) { }

  ngOnInit(): void {
    this.loadData();
  }

  updateSearch(value: string): void {
    this.searchTerm = value;
    this.currentPage = 1;
    this.applyFilters();
  }

  setDepartmentFilter(filter: DepartmentFilter): void {
    this.activeDepartmentFilter = filter;
    this.currentPage = 1;
    this.applyFilters();
  }

  navigateToAddTeam(): void {
    this.router.navigate(['/teams/add']);
  }

  toFirstPage(): void {
    this.goToPage(1);
  }

  toPreviousPage(): void {
    if (this.currentPage > 1) {
      this.goToPage(this.currentPage - 1);
    }
  }

  toNextPage(): void {
    if (this.currentPage < this.totalPages) {
      this.goToPage(this.currentPage + 1);
    }
  }

  toLastPage(): void {
    this.goToPage(this.totalPages);
  }

  goToPage(page: number): void {
    this.currentPage = Math.min(Math.max(page, 1), this.totalPages);
    this.updatePagedRows();
  }

  trackByRow(_index: number, row: TeamRow): number {
    return row.id;
  }

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.filteredRows.length / this.pageSize));
  }

  get rangeStart(): number {
    if (this.filteredRows.length === 0) {
      return 0;
    }

    return (this.currentPage - 1) * this.pageSize + 1;
  }

  get rangeEnd(): number {
    return Math.min(this.currentPage * this.pageSize, this.filteredRows.length);
  }

  get visiblePageNumbers(): number[] {
    if (this.totalPages <= 5) {
      return this.range(1, this.totalPages);
    }

    if (this.currentPage <= 3) {
      return [1, 2, 3];
    }

    if (this.currentPage >= this.totalPages - 2) {
      return [this.totalPages - 2, this.totalPages - 1, this.totalPages];
    }

    return [this.currentPage - 1, this.currentPage, this.currentPage + 1];
  }

  get showLeadingEllipsis(): boolean {
    return this.totalPages > 5 && this.visiblePageNumbers[0] > 1;
  }

  get showTrailingEllipsis(): boolean {
    return (
      this.totalPages > 5
      && this.visiblePageNumbers[this.visiblePageNumbers.length - 1] < this.totalPages
    );
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

        this.rows = this.buildTeamRows(normalizedTeams, normalizedProjects);
        this.totalTeams = this.rows.length;
        this.totalProjects = normalizedProjects.length;
        this.totalDepartments = this.countUniqueDepartments(this.rows);

        this.currentPage = 1;
        this.applyFilters();
        this.loading = false;
      },
      error: () => {
        this.error = 'Failed to load team governance data.';
        this.loading = false;
      }
    });
  }

  private applyFilters(): void {
    const normalizedSearch = this.searchTerm.trim().toLowerCase();

    this.filteredRows = this.rows.filter((row) => {
      const matchesDepartment =
        this.activeDepartmentFilter === 'all'
        || row.departmentFilter === this.activeDepartmentFilter;

      const matchesSearch =
        !normalizedSearch
        || row.name.toLowerCase().includes(normalizedSearch)
        || row.projectsText.toLowerCase().includes(normalizedSearch);

      return matchesDepartment && matchesSearch;
    });

    this.currentPage = Math.min(Math.max(this.currentPage, 1), this.totalPages);
    this.updatePagedRows();
  }

  private updatePagedRows(): void {
    if (this.filteredRows.length === 0) {
      this.pagedRows = [];
      return;
    }

    const startIndex = (this.currentPage - 1) * this.pageSize;
    this.pagedRows = this.filteredRows.slice(startIndex, startIndex + this.pageSize);
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
        projectsText: this.formatProjects(projectNames),
        statusLabel: isActive ? 'ACTIVE' : 'ON HOLD',
        isActive,
        membersCount: 0,
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

  private range(start: number, end: number): number[] {
    return Array.from({ length: end - start + 1 }, (_, idx) => start + idx);
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