import { Component, OnInit } from '@angular/core';
import {
  RequisitionModuleService,
  RequisitionDto,
  ProjectModuleService,
  ProjectDto,
  UserModuleService, UserDtoRoleEnum
} from 'src/app/core/api';
import { Router } from '@angular/router';
import { MatTableDataSource } from '@angular/material/table';
import { PageEvent } from '@angular/material/paginator';
import {MatDialog} from "@angular/material/dialog";
import {ToastService} from "../../../core/services/toast.service";
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
  selectedProjectId: number | '' = '';
  projects: ProjectDto[] = [];

  readonly statuses = [
    { value: 'OPEN', label: 'Open' },
    { value: 'AWAITING_PAYMENT', label: 'Awaiting Payment' },
    { value: 'CLOSED', label: 'Closed' },
  ];

  displayedColumns = ['requestName', 'projectName', 'workflowName', 'isClosed', 'actions'];

  constructor(
    private requisitionService: RequisitionModuleService,
    private projectService: ProjectModuleService,
    private userService: UserModuleService,
    private authService: AuthService,
    private dialog: MatDialog,
    private toastService: ToastService,
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

    let backendStatus: string | undefined = undefined;
    if (this.status === 'OPEN') {
      backendStatus = 'OPEN';
    } else if (this.status === 'AWAITING_PAYMENT' || this.status === 'CLOSED') {
      backendStatus = 'CLOSED';
    }

    this.requisitionService.getRequests(
      backendStatus,
      this.search || undefined,
      this.selectedProjectId !== '' ? this.selectedProjectId : undefined,
      this.page,
      this.size
    ).subscribe({
      next: (response) => {
        let list = response.content || [];
        if (this.status === 'AWAITING_PAYMENT') {
          list = list.filter(req => req.isClosed && !req.isPaid);
        } else if (this.status === 'CLOSED') {
          list = list.filter(req => req.isClosed && req.isPaid);
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
                this.toastService.showError("Requester change failed: " + err.message);
              }
            });
          }
        });
        },
      error: (err) => {
        this.toastService.showError("Could not fetch users for fallback selection: " + err.message);
      }
    });
  }
}
