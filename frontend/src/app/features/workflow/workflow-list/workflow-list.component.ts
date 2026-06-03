import {Component, OnInit, ViewChild} from '@angular/core';
import {MatTableDataSource} from '@angular/material/table';
import {PageEvent} from '@angular/material/paginator';
import {AuthService} from "../../../core/services/auth.service";
import {Pageable, WorkflowModuleService} from "../../../core/api";
import {WorkflowDto} from "../../../core/api";
import {SharedTableComponent} from "../../../shared/components/table/shared-table.component";
import { MatDialog } from "@angular/material/dialog";
import { ToastService } from "../../../core/services/toast.service";
import { ConfirmationDialogComponent } from "../../../shared/components/confirmation-dialog/confirmation-dialog.component";

@Component({
  selector: 'app-workflow-list',
  templateUrl: './workflow-list.component.html'
})
export class WorkflowListComponent implements OnInit {
  dataSource = new MatTableDataSource<WorkflowDto>();
  displayedColumns: string[] = ['name', 'description', 'department', 'status', 'actions'];

  totalElements = 0;
  pageSize = 10;
  currentPage = 0;
  currentSearch = '';
  currentIsActive: boolean | undefined = undefined;
  loading = false;

  readonly statusOptions = ['ALL', 'ACTIVE', 'INACTIVE'];
  selectedStatus = 'ALL';

  @ViewChild(SharedTableComponent) sharedTable!: SharedTableComponent;

  constructor(
    private workflowService: WorkflowModuleService,
    public authService: AuthService,
    private dialog: MatDialog,
    private toastService: ToastService
  ) {
  }

  ngOnInit(): void {
    this.loadWorkflows();
  }

  get isFinanceOfficer(): boolean {
    return this.authService.hasRole('FINANCE_OFFICER');
  }

  get isAdministrator(): boolean {
    return this.authService.hasRole('ADMINISTRATOR');
  }

  loadWorkflows(): void {
    this.loading = true;

    const pageable: any = {
      page: this.currentPage,
      size: this.pageSize,
      sort: ['name,asc']
    };

    this.workflowService.getAllWorkflows(pageable, this.currentSearch, this.currentIsActive).subscribe({
      next: (response) => {
        this.dataSource.data = response.content || [];
        this.totalElements = response.totalElements || 0;
        this.loading = false;
      },
      error: (err) => {
        console.error("Fetch failed", err);
        this.loading = false;
      }
    });
  }

  onPageChange(event: PageEvent): void {
    this.currentPage = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadWorkflows();
  }

  onSearch(query: string): void {
    this.currentSearch = query;
    this.currentPage = 0;
    this.sharedTable.resetToFirstPage();
    this.loadWorkflows();
  }

  onStatusFilterChange(value: string): void {
    this.currentPage = 0;
    this.selectedStatus = value;
    if (value === 'ACTIVE') {
      this.currentIsActive = true;
    } else if (value === 'INACTIVE') {
      this.currentIsActive = false;
    } else {
      this.currentIsActive = undefined;
    }
    this.sharedTable.resetToFirstPage();
    this.loadWorkflows();

  }

  deleteWorkflow(workflow: WorkflowDto): void {
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      width: '450px',
      data: {
        title: 'Delete Workflow',
        message: 'Are you sure you want to delete the workflow',
        highlightText: workflow.name,
        messageSuffix: '?',
        subMessage: 'This will disable the workflow, preventing it from being selected for new procurement requests. Existing requests using this workflow will not be affected.'
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.loading = true;
        this.workflowService.deleteWorkflow(workflow.id!).subscribe({
          next: () => {
            this.toastService.showSuccess('Workflow deleted successfully');
            this.loadWorkflows();
          },
          error: (err) => {
            this.toastService.showError('Failed to delete workflow');
            this.loading = false;
          }
        });
      }
    });
  }
}
