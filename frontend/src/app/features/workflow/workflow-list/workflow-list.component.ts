                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                import {Component, OnInit, ViewChild} from '@angular/core';
import { MatTableDataSource } from '@angular/material/table';
import { PageEvent } from '@angular/material/paginator';
import { AuthService } from "../../../core/services/auth.service";
import {Pageable, WorkflowModuleService} from "../../../core/api";
import { WorkflowDto } from "../../../core/api";
import {SharedTableComponent} from "../../../shared/components/table/shared-table.component";

@Component({
  selector: 'app-workflow-list',
  templateUrl: './workflow-list.component.html'
})
export class WorkflowListComponent implements OnInit {
  dataSource = new MatTableDataSource<WorkflowDto>();
  displayedColumns: string[] = ['name', 'description',/* 'team' ,*/ 'status', 'actions'];

  totalElements = 0;
  pageSize = 10;
  currentPage = 0;
  currentSearch = '';
  currentIsActive : boolean | undefined = undefined;
  loading = false;

  readonly statusOptions = ['ALL', 'ACTIVE', 'INACTIVE'];
  selectedStatus = 'ALL';

  @ViewChild(SharedTableComponent) sharedTable!: SharedTableComponent;

  constructor(
    private workflowService: WorkflowModuleService,
    public authService: AuthService
  ) {}

  ngOnInit(): void {
    this.loadWorkflows();
  }

  get isFinanceOfficer(): boolean {
    return this.authService.hasRole('FINANCE_OFFICER');
  }

  loadWorkflows(): void {
    this.loading = true;

    const pageable: any = {
      page: this.currentPage,
      size: this.pageSize,
      sort: ['name,asc']
    };

    this.workflowService.getAllWorkflows(pageable, this.currentSearch,this.currentIsActive).subscribe({
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
}
