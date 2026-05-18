import {Component, OnInit, ViewChild} from '@angular/core';
import { Router } from '@angular/router';
import { ToastService } from '../../../../../core/services/toast.service';
import { JiraConfigControllerService, Pageable} from '../../../../../core/api';
import { JiraConfigResponseDto } from '../../../../../core/api';
import { MatTableDataSource } from '@angular/material/table';
import { MatDialog } from '@angular/material/dialog';
import { ConfirmationDialogComponent } from '../../../../../shared/components/confirmation-dialog/confirmation-dialog.component';
import {SharedTableComponent} from "../../../../../shared/components/table/shared-table.component";
import {PageEvent} from "@angular/material/paginator";

@Component({
  selector: 'app-jira-settings-list',
  templateUrl: './jira-settings-list.component.html',
})
export class JiraSettingsListComponent implements OnInit {
  title = 'Jira Integrations';
  subtitle = 'Manage your dynamic Jira polling configurations.';

  displayedColumns = ['name', 'jiraUrl', 'syncInterval', 'status', 'nextSync', 'actions'];
  dataSource = new MatTableDataSource<JiraConfigResponseDto>();

  loading = false;
  totalPageElements = 0;
  pageSize = 10;
  currentPage = 0;
  currentSearch = '';

  @ViewChild(SharedTableComponent) sharedTable!: SharedTableComponent;

  constructor(
    private jiraConfigService: JiraConfigControllerService,
    private toastService: ToastService,
    private router: Router,
    private dialog: MatDialog
  ) { }

  ngOnInit(): void {
    this.loadSettings();
  }

  loadSettings(): void {
    this.loading = true;

    const pageable: any = {
      page: this.currentPage,
      size: this.pageSize,
      sort: ['name,asc']
    };

    this.jiraConfigService.getAllConfigs(pageable,this.currentSearch).subscribe({
      next: (response) => {
        this.dataSource.data = response.content || [];
        this.totalPageElements = response.totalElements || 0;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.toastService.showError('Failed to load Jira configs');
      }
    });
  }

  isRunning(config: JiraConfigResponseDto): boolean {
    return config.syncIntervalMinutes > 0;
  }

  getNextSyncTime(config: JiraConfigResponseDto): Date | null {
    return config.nextSyncTime ? new Date(config.nextSyncTime) : null;
  }

  editConfig(config: JiraConfigResponseDto): void {
    this.router.navigate(['/integrations/edit', config.id]);
  }

  triggerSync(config: JiraConfigResponseDto): void {
    this.toastService.showInfo('Triggering sync...');
    this.jiraConfigService.triggerSync(config.id).subscribe({
      next: () => this.toastService.showInfo('Sync completed successfully'),
      error: () => this.toastService.showError('Sync failed. Please check logs.')
    });
  }

  deleteConfig(config: JiraConfigResponseDto): void {
    const ref = this.dialog.open(ConfirmationDialogComponent, {
      data: { title: 'Delete Integration', message: `Are you sure you want to delete "${config.name}"?` }
    });
    ref.afterClosed().subscribe((confirmed) => {
      if (confirmed) {
        this.jiraConfigService.deleteConfig(config.id).subscribe({
          next: () => {
            this.toastService.showSuccess('Integration deleted successfully.');
            this.loadSettings();
          },
          error: () => this.toastService.showError('Failed to delete integration.')
        });
      }
    });
  }

  onPageChange(event: PageEvent): void {
    this.currentPage = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadSettings();
  }
  onSearchChanged(value: string): void {
    this.currentSearch = value;
    this.currentPage = 0;
    this.sharedTable.resetToFirstPage();
    this.loadSettings();
  }
  syncAll():void {
    this.toastService.showSuccess('Triggering all sync...');
    this.jiraConfigService.triggerAllSyncs().subscribe({
      next: () => this.toastService.showInfo('Syncs completed successfully'),
      error: () => this.toastService.showError('Syncs failed. Please check logs.')
    });
  }
}
