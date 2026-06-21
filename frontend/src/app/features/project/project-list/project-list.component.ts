import { Component, OnInit, ViewChild } from '@angular/core';
import {ProjectModuleService, ProjectDto} from '../../../core/api';
import { MatTableDataSource } from '@angular/material/table';
import { SharedTableComponent } from '../../../shared/components/table/shared-table.component';
import { MatDialog } from '@angular/material/dialog';
import { ConfirmationDialogComponent } from '../../../shared/components/confirmation-dialog/confirmation-dialog.component';
import {Router} from "@angular/router";
import {AuthService} from "../../../core/services/auth.service";
import {ToastService} from "../../../core/services/toast.service";
import { extractErrorMessage } from '../../../shared/utils/error-utils';

@Component({
  selector: 'app-project-list',
  templateUrl: './project-list.component.html',
})
export class ProjectListComponent implements OnInit {
  @ViewChild(SharedTableComponent) sharedTable!: SharedTableComponent;

  displayedColumns: string[] = [];
  dataSource = new MatTableDataSource<ProjectDto>([]);

  loading = false;
  error: string | null = null;

  constructor(
    private projectService: ProjectModuleService,
    private authService: AuthService,
    private toastService: ToastService,
    private router: Router,
    private dialog: MatDialog
  ) { }

  ngOnInit(): void {
    this.setDisplayedColumns();
    this.loadData();
  }

  private loadData(): void {
    this.loading = true;
    this.projectService.getAllProjects().subscribe({
      next: (projects) => {
        this.dataSource.data = projects;
        this.loading = false;
      },
      error: (err) => {
        this.toastService.showError(extractErrorMessage(err, 'Failed to load projects'));
        this.loading = false;
      }
    });
  }

  applyFilter(value: string): void {
    this.dataSource.filter = value.trim().toLowerCase();
    if (this.dataSource.paginator) this.dataSource.paginator.firstPage();
  }

  editProject(project: ProjectDto) {
    this.router.navigate(['/projects/edit', project.id]);
  }

  deleteProject(project: ProjectDto) {
    const ref = this.dialog.open(ConfirmationDialogComponent, {
      data: { title: 'Delete Project', message: `Are you sure you want to delete "${project.name}"?` }
    });
    ref.afterClosed().subscribe((confirmed) => {
      if (confirmed) {
        this.projectService.deleteProject(project.id!).subscribe({
          next: () => {
            this.toastService.showSuccess('Project deleted successfully.');
            this.loadData();
          },
          error: (err) => {
            this.toastService.showError(extractErrorMessage(err, 'Failed to delete project'));
          }
        });
      }
    });
  }

  private setDisplayedColumns(): void {
    this.displayedColumns = ['name', 'team', 'budget', 'committed', 'actual', 'startDate', 'endDate'];

    if (this.isFinanceOfficer || this.isAdministrator) {
      this.displayedColumns.push('actions');
    }
  }

  get isFinanceOfficer(): boolean {
    return this.authService.hasRole('FINANCE_OFFICER');
  }

  get isAdministrator(): boolean {
    return this.authService.hasRole('ADMINISTRATOR');
  }
}
