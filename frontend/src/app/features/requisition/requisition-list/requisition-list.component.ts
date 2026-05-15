import { Component, OnInit } from '@angular/core';
import { RequisitionModuleService, RequisitionDto, ProjectModuleService, ProjectDto } from 'src/app/core/api';
import { Router } from '@angular/router';
import { MatTableDataSource } from '@angular/material/table';
import { PageEvent } from '@angular/material/paginator';

@Component({
  selector: 'app-requisition-list',
  templateUrl: './requisition-list.component.html'
})
export class RequisitionListComponent implements OnInit {
  requests = new MatTableDataSource<RequisitionDto>();
  loading = false;

  page = 0;
  size = 10;
  totalElements = 0;

  status = '';
  search = '';
  projectSearch = '';
  selectedProjectId: number | '' = '';
  projects: ProjectDto[] = [];

  readonly statuses = [
    { value: 'OPEN', label: 'Open' },
    { value: 'CLOSED', label: 'Closed' },
  ];

  displayedColumns = ['requestName', 'projectName', 'workflowName', 'isClosed', 'actions'];

  constructor(
    private requisitionService: RequisitionModuleService,
    private projectService: ProjectModuleService,
    private router: Router
  ) { }

  ngOnInit(): void {
    this.loadProjects();
    this.loadRequests();
  }

  loadProjects(): void {
    this.projectService.getAllProjects().subscribe({
      next: (projects) => this.projects = projects,
      error: (err) => console.error('Failed to load projects', err)
    });
  }

  loadRequests(): void {
    this.loading = true;
    this.requisitionService.getRequests(
      this.status || undefined,
      this.search || undefined,
      this.selectedProjectId !== '' ? this.selectedProjectId : undefined,
      this.page,
      this.size
    ).subscribe({
      next: (response) => {
        this.requests.data = response.content || [];
        this.totalElements = response.totalElements || 0;
        this.loading = false;
      },
      error: (err) => {
        console.error('Failed to load requests', err);
        this.loading = false;
      }
    });
  }

  onSearchChange(searchTerm: string): void {
    this.search = searchTerm;
    this.page = 0;
    this.loadRequests();
  }

  onPageChange(event: PageEvent): void {
    this.page = event.pageIndex;
    this.size = event.pageSize;
    this.loadRequests();
  }

  onFilterChange(): void {
    this.page = 0;
    this.loadRequests();
  }

  clearFilters(): void {
    this.status = '';
    this.selectedProjectId = '';
    this.page = 0;
    this.loadRequests();
  }

  getFilteredProjects(): ProjectDto[] {
    if (!this.projectSearch) return this.projects;
    const search = this.projectSearch.toLowerCase();
    return this.projects.filter(p => p.name?.toLowerCase().includes(search));
  }

  onOpenedChange(opened: boolean): void {
    if (!opened) {
      this.projectSearch = '';
    }
  }

  onStatusFilterChange(value: string | undefined): void {
    const newStatus = value || '';
    if (this.status !== newStatus) {
      this.status = newStatus;
      this.onFilterChange();
    }
  }

  viewDetails(id: number): void {
    this.router.navigate(['/requisitions', id]);
  }

  createNewRequest(): void {
    this.router.navigate(['/requisitions/create']);
  }
}
