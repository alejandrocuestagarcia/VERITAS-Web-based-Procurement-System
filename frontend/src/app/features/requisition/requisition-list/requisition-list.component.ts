import { Component, OnInit, HostListener } from '@angular/core';
import {
  RequisitionModuleService,
  RequisitionDto,
  ProjectModuleService,
  ProjectDto,
  UserModuleService, UserDtoRoleEnum, UserDto
} from 'src/app/core/api';
import { Router, ActivatedRoute } from '@angular/router';
import { MatTableDataSource } from '@angular/material/table';
import { PageEvent } from '@angular/material/paginator';
import {MatDialog} from "@angular/material/dialog";
import {ToastService} from "../../../core/services/toast.service";
import { extractErrorMessage } from '../../../shared/utils/error-utils';
import {
  RequisitionChangeRequesterDialogComponent
} from "../requisition-change-requester-dialog/requisition-change-requester-dialog.component";
import {AuthService} from "../../../core/services/auth.service";

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
  creatorSearch = '';
  selectedProjectId: number | '' = '';
  projects: ProjectDto[] = [];
  createdFrom = '';
  createdTo = '';
  selectedCreatorId: number | '' = '';
  creators: UserDto[] = [];

  activeDropdown: string | null = null;
  dropdownX = 0;
  dropdownY = 0;

  readonly statuses = [
    { value: 'OPEN', label: 'Open' },
    { value: 'AWAITING_PAYMENT', label: 'Awaiting Payment' },
    { value: 'CLOSED', label: 'Closed' },
    { value: 'REJECTED', label: 'Rejected' },
    { value: 'CANCELLED', label: 'Cancelled' },
  ];

  get isRequester(): boolean {
    return this.authService.hasRole('REQUESTER');
  }

  get isProcurementOfficer(): boolean {
    return this.authService.hasRole('PROCUREMENT_OFFICER');
  }

  get displayedColumns(): string[] {
    const columns = ['requestName', 'projectName', 'workflowName'];
    if (!this.isRequester) {
      columns.push('creator');
    }
    columns.push('createdAt');
    columns.push('isClosed');
    columns.push('actions');
    return columns;
  }

  constructor(
    private requisitionService: RequisitionModuleService,
    private projectService: ProjectModuleService,
    private userService: UserModuleService,
    public authService: AuthService,
    private dialog: MatDialog,
    private toastService: ToastService,
    private router: Router,
    private route: ActivatedRoute
  ) { }

  ngOnInit(): void {
    this.authService.getCurrentUser().subscribe();
    this.loadProjects();
    this.loadCreators();

    // Subscribe to query parameters to drive filtering
    this.route.queryParams.subscribe(params => {
      this.search = params['q'] || '';
      this.status = params['status'] || '';
      this.selectedProjectId = params['projectId'] ? Number(params['projectId']) : '';
      this.createdFrom = params['createdFrom'] || '';
      this.createdTo = params['createdTo'] || '';
      this.selectedCreatorId = params['creatorId'] ? Number(params['creatorId']) : '';

      this.loadRequests();
    });
  }

  loadProjects(): void {
    this.projectService.getAllProjects(true).subscribe({
      next: (projects) => this.projects = projects,
      error: (err) => console.error('Failed to load projects', err)
    });
  }

  loadCreators(): void {
    if (!this.isRequester) {
      this.userService.getAllUsers({ page: 0, size: 1000 }, "").subscribe({
        next: (response) => this.creators = (response.content || []).filter(u => u.role === UserDtoRoleEnum.Requester),
        error: (err) => console.error('Failed to load creators', err)
      });
    }
  }

  loadRequests(): void {
    this.loading = true;

    let backendStatus: string | undefined = undefined;
    if (this.status === 'OPEN') {
      backendStatus = 'OPEN';
    } else if (this.status === 'AWAITING_PAYMENT' || this.status === 'CLOSED') {
      backendStatus = 'CLOSED';
    } else if (this.status === 'REJECTED') {
      backendStatus = 'REJECTED';
    } else if (this.status === 'CANCELLED') {
      backendStatus = 'CANCELLED';
    }

    this.requisitionService.getRequests(
      backendStatus,
      this.search || undefined,
      this.selectedProjectId !== '' ? (this.selectedProjectId as number) : undefined,
      this.createdFrom || undefined,
      this.createdTo || undefined,
      this.selectedCreatorId !== '' ? (this.selectedCreatorId as number) : undefined,
      this.page,
      this.size
    ).subscribe({
      next: (response) => {
        let list = response.content || [];
        if (this.status === 'AWAITING_PAYMENT') {
          list = list.filter(req => req.isClosed && req.closedReason === 'COMPLETED' && !req.isPaid);
        } else if (this.status === 'CLOSED') {
          list = list.filter(req => req.isClosed && req.closedReason === 'COMPLETED' && req.isPaid);
        }
        this.requests.data = list;
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
    this.updateFiltersInUrl();
  }

  onPageChange(event: PageEvent): void {
    this.page = event.pageIndex;
    this.size = event.pageSize;
    this.loadRequests();
  }

  onFilterChange(): void {
    this.page = 0;
    this.updateFiltersInUrl();
  }

  clearFilters(): void {
    this.status = '';
    this.selectedProjectId = '';
    this.search = '';
    this.createdFrom = '';
    this.createdTo = '';
    this.selectedCreatorId = '';
    this.page = 0;
    this.activeDropdown = null;

    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {
        q: undefined,
        status: undefined,
        projectId: undefined,
        createdFrom: undefined,
        createdTo: undefined,
        creatorId: undefined
      }
    });
  }

  clearDateFilter(): void {
    this.createdFrom = '';
    this.createdTo = '';
    this.onFilterChange();
  }

  updateFiltersInUrl(): void {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {
        q: this.search || undefined,
        status: this.status || undefined,
        projectId: this.selectedProjectId || undefined,
        createdFrom: this.createdFrom || undefined,
        createdTo: this.createdTo || undefined,
        creatorId: this.selectedCreatorId || undefined
      },
      queryParamsHandling: 'merge'
    });
  }


  toggleDropdown(name: string, event: MouseEvent): void {
    event.stopPropagation();
    if (this.activeDropdown === name) {
      this.activeDropdown = null;
      return;
    }
    const btn = event.currentTarget as HTMLElement;
    const rect = btn.getBoundingClientRect();

    const estimatedHeights: Record<string, number> = {
      status: 140,
      project: 300,
      creator: 300,
      date: 200,
    };
    const estimatedWidths: Record<string, number> = {
      status: 160,
      project: 224,
      creator: 224,
      date: 256,
    };

    const estH = estimatedHeights[name] ?? 200;
    const estW = estimatedWidths[name] ?? 224;
    const spaceBelow = window.innerHeight - rect.bottom - 8;

    this.dropdownY = spaceBelow >= estH
      ? rect.bottom + 6
      : Math.max(8, rect.top - estH - 6);

    this.dropdownX = Math.min(rect.left, window.innerWidth - estW - 8);

    this.activeDropdown = name;
    if (name === 'project') this.projectSearch = '';
    if (name === 'creator') this.creatorSearch = '';
  }
  setFilter(type: 'status', value: string): void {
    if (type === 'status') {
      this.status = value;
    }
    this.activeDropdown = null;
    this.onFilterChange();
  }

  setProjectFilter(id: number | string | undefined): void {
    this.selectedProjectId = (id !== undefined && id !== '') ? Number(id) : '';
    this.projectSearch = '';
    this.activeDropdown = null;
    this.onFilterChange();
  }

  setCreatorFilter(id: number | string | undefined): void {
    this.selectedCreatorId = (id !== undefined && id !== '') ? Number(id) : '';
    this.creatorSearch = '';
    this.activeDropdown = null;
    this.onFilterChange();
  }

  getFilteredProjects(): ProjectDto[] {
    if (!this.projectSearch) return this.projects;
    const search = this.projectSearch.toLowerCase();
    return this.projects.filter(p => p.name?.toLowerCase().includes(search));
  }

  getFilteredCreators(): UserDto[] {
    if (!this.creatorSearch) return this.creators;
    const search = this.creatorSearch.toLowerCase();
    return this.creators.filter(c => c.name?.toLowerCase().includes(search));
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

  // ── Label helpers ──────────────────────────────────────────────────────────

  getStatusLabel(value: string): string {
    return this.statuses.find(s => s.value === value)?.label ?? value;
  }

  getDateLabel(): string {
    if (this.createdFrom && this.createdTo) {
      return `${this.formatShortDate(this.createdFrom)} – ${this.formatShortDate(this.createdTo)}`;
    }
    if (this.createdFrom) return `From ${this.formatShortDate(this.createdFrom)}`;
    if (this.createdTo)   return `Until ${this.formatShortDate(this.createdTo)}`;
    return 'Date';
  }

  private formatShortDate(dateStr: string): string {
    const d = new Date(dateStr);
    return d.toLocaleDateString('en-GB', { day: '2-digit', month: 'short' });
  }

  getActiveFiltersCount(): number {
    let count = 0;
    if (this.selectedProjectId !== '') count++;
    if (this.selectedCreatorId !== '') count++;
    if (this.createdFrom) count++;
    if (this.createdTo) count++;
    return count;
  }

  getSelectedProjectName(): string {
    if (this.selectedProjectId === '') return '';
    const p = this.projects.find(proj => proj.id === this.selectedProjectId);
    return p ? (p.name || '') : '';
  }

  getSelectedCreatorName(): string {
    if (this.selectedCreatorId === '') return '';
    const c = this.creators.find(user => user.id === this.selectedCreatorId);
    return c ? (c.name || '') : '';
  }

  viewDetails(id: number): void {
    this.router.navigate(['/requisitions', id]);
  }

  createNewRequest(): void {
    this.router.navigate(['/requisitions/create']);
  }

  get isFinanceOfficer(): boolean {
    return this.authService.hasRole('FINANCE_OFFICER');
  }

  get isAdministrator(): boolean {
    return this.authService.hasRole('ADMINISTRATOR');
  }

  changeRequester(req: RequisitionDto) {
    this.userService.getAllUsers({ page: 0, size: 1000 }, "").subscribe({
      next: (response) => {
        const fallbackUsers = response.content!.filter(u => u.id !== req.requesterId && u.active !== false && u.role === UserDtoRoleEnum.Requester && u.teamId === req.requesterTeamId);

        const dialogReturnValue = this.dialog.open(RequisitionChangeRequesterDialogComponent, {
              width: '500px',
              data: {
                requesterId: req.requesterId,
                requesterName: req.requesterName,
                fallbackUsers: fallbackUsers
              }
        });

        dialogReturnValue.afterClosed().subscribe(fallbackUserId => {
          if (fallbackUserId !== undefined) {
            this.requisitionService.changeRequester(req.id!, fallbackUserId).subscribe({
              next: () => {
                this.toastService.showSuccess('Requester changed successfully for requisition ' + req.requestName);
                this.loadRequests();
                },
              error: (err) => {
                this.toastService.showError(extractErrorMessage(err, 'Failed to change requester'));
              }
            });
          }
        });
        },
      error: (err) => {
        this.toastService.showError(extractErrorMessage(err, 'Failed to load users for fallback selection'));
      }
    });
  }
}
