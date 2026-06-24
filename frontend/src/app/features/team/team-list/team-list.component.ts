import { Component, OnInit, ViewChild, AfterViewInit, HostListener } from '@angular/core';
import { Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import { MatTableDataSource } from '@angular/material/table';
import { SharedTableComponent } from '../../../shared/components/table/shared-table.component';
import {
  DepartmentDto,
  DepartmentsModuleService,
  ProjectDto,
  ProjectModuleService,
  TeamDto,
  TeamsModuleService
} from '../../../core/api';
import { MatDialog } from '@angular/material/dialog';
import { ConfirmationDialogComponent } from '../../../shared/components/confirmation-dialog/confirmation-dialog.component';
import { ToastService } from '../../../core/services/toast.service';
import { extractErrorMessage } from '../../../shared/utils/error-utils';

type DepartmentFilter = string;

interface TeamRow {
  id: number;
  name: string;
  departmentLabel: string;
  departmentFilter: DepartmentFilter;
  projectsText: string;
  statusLabel: 'Active' | 'Inactive';
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

  departmentFilters: Array<{ key: string; label: string }> = [];

  displayedColumns: string[] = ['identity', 'department', 'projects', 'status', 'actions'];
  dataSource = new MatTableDataSource<TeamRow>([]);

  loading = false;
  error: string | null = null;

  activeDepartmentFilter: DepartmentFilter = 'all';
  private currentSearchString = "";

  activeDropdown: string | null = null;
  dropdownX = 0;
  dropdownY = 0;
  departmentSearch = '';

  totalTeams = 0;
  totalProjects = 0;
  totalDepartments = 0;

  constructor(
    private teamsService: TeamsModuleService,
    private projectService: ProjectModuleService,
    private departmentsService: DepartmentsModuleService,
    private router: Router,
    private dialog: MatDialog,
    private toastService: ToastService
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

//AI-Generated
  toggleDropdown(name: string, event: MouseEvent): void {
    event.stopPropagation();
    if (this.activeDropdown === name) {
      this.activeDropdown = null;
      return;
    }
    const btn = event.currentTarget as HTMLElement;
    const rect = btn.getBoundingClientRect();

    const estH = 240;
    const estW = 224;
    const spaceBelow = window.innerHeight - rect.bottom - 8;

    this.dropdownY = spaceBelow >= estH
      ? rect.bottom + 6
      : Math.max(8, rect.top - estH - 6);

    this.dropdownX = Math.min(rect.left, window.innerWidth - estW - 8);

    this.activeDropdown = name;
    if (name === 'department') this.departmentSearch = '';
  }

  setDepartmentFilter(key: string): void {
    this.activeDepartmentFilter = key || 'all';
    this.departmentSearch = '';
    this.activeDropdown = null;
    this.applyFilters();
  }

  getFilteredDepartments(): Array<{ key: string; label: string }> {
    if (!this.departmentSearch) return this.departmentFilters;
    const search = this.departmentSearch.toLowerCase();
    return this.departmentFilters.filter(d => d.label.toLowerCase().includes(search));
  }

  getSelectedDepartmentLabel(): string {
    if (this.activeDepartmentFilter === 'all') return 'Department';
    const filter = this.departmentFilters.find(d => d.key === this.activeDepartmentFilter);
    return filter ? filter.label : 'Department';
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    const target = event.target as HTMLElement;
    if (!target.closest('.req-filter-pill-wrapper') && !target.closest('.req-dropdown')) {
      this.activeDropdown = null;
    }
  }

  @HostListener('window:scroll', [])
  onWindowScroll(): void {
    this.activeDropdown = null;
  }
//AI-Generated end

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

  navigateToEditTeam(row: TeamRow): void {
    this.router.navigate(['/teams/edit', row.id]);
  }

  deleteTeam(row: TeamRow): void {
    const ref = this.dialog.open(ConfirmationDialogComponent, {
      data: { title: 'Delete Team', message: `Are you sure you want to delete "${row.name}"?` }
    });
    ref.afterClosed().subscribe((confirmed) => {
      if (confirmed) {
        this.teamsService.deleteTeam(row.id).subscribe({
          next: (res) => {
            this.toastService.showSuccess(res?.['message'] || `Team "${row.name}" deleted successfully.`);
            this.loadData();
          },
          error: (err) => {
            this.toastService.showError(extractErrorMessage(err, `Failed to delete team "${row.name}"`));
          }
        });
      }
    });
  }

  private loadData(): void {
    this.loading = true;
    this.error = null;

    forkJoin({
      teams: this.teamsService.getAllTeams(true),
      projects: this.projectService.getAllProjects(true),
      departments: this.departmentsService.getAllDepartments(true)
    }).subscribe({
      next: ({ teams, projects, departments }) => {
        const normalizedTeams = this.toArray<TeamDto>(teams);
        const normalizedProjects = this.toArray<ProjectDto>(projects);
        const normalizedDepts = this.toArray<DepartmentDto>(departments);

        this.departmentFilters = normalizedDepts
          .filter(d => !!d.name)
          .map(d => ({
            key: d.name.toLowerCase(),
            label: d.name
          }));

        const rows = this.buildTeamRows(normalizedTeams, normalizedProjects);
        this.dataSource.data = rows;
        this.totalTeams = rows.length;
        this.totalProjects = normalizedProjects.length;
        this.totalDepartments = normalizedDepts.length;

        this.applyFilters();
        this.loading = false;
      },
      error: (err) => {
        this.toastService.showError(extractErrorMessage(err, 'Failed to load team governance data'));
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
        statusLabel: isActive ? 'Active' : 'Inactive',
        isActive,
        membersCount: team.members?.length ?? 0,
        icon: "groups"
      };
    });
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
    filter: string;
  } {
    const normalized = (rawDepartment ?? '').trim();
    if (!normalized) {
      return { label: 'Unassigned', filter: 'unassigned' };
    }

    return {
      label: normalized,
      filter: normalized.toLowerCase()
    };
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
