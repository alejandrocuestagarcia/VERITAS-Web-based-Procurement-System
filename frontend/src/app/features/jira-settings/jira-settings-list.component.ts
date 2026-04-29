import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { JiraConfigControllerService } from '../../core/api/api/jiraConfigController.service';
import { JiraConfigDto } from '../../core/api/model/jiraConfigDto';
import { MatTableDataSource } from '@angular/material/table';

@Component({
  selector: 'app-jira-settings-list',
  templateUrl: './jira-settings-list.component.html',
})
export class JiraSettingsListComponent implements OnInit {
  title = 'Jira Integrations';
  subtitle = 'Manage your dynamic Jira polling configurations.';

  displayedColumns = ['name', 'jiraUrl', 'syncInterval', 'status', 'nextSync', 'actions'];
  dataSource = new MatTableDataSource<JiraConfigDto>();

  loading = false;
  totalPageElements = 0;

  constructor(
    private jiraConfigService: JiraConfigControllerService,
    private snackBar: MatSnackBar,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadSettings();
  }

  loadSettings(): void {
    this.loading = true;
    this.jiraConfigService.getAllConfigs().subscribe({
      next: (data) => {
        this.dataSource.data = data;
        this.totalPageElements = data.length;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.snackBar.open('Failed to load Jira configs', 'Close', { duration: 3000 });
      }
    });
  }

  isRunning(config: JiraConfigDto): boolean {
    return (config.syncIntervalMinutes ?? 0) > 0;
  }

  getNextSyncTime(config: JiraConfigDto): Date | null {
    return config.nextSyncTime ? new Date(config.nextSyncTime) : null;
  }

  editConfig(config: JiraConfigDto): void {
    this.router.navigate(['/integrations/edit', config.id]);
  }

  triggerSync(config: JiraConfigDto): void {
    this.snackBar.open('Triggering sync...', '', { duration: 2000 });
    this.jiraConfigService.triggerSync(config.id!).subscribe({
      next: () => this.snackBar.open('Sync completed successfully', 'Close', { duration: 3000 }),
      error: () => this.snackBar.open('Sync failed. Please check logs.', 'Close', { duration: 3000 })
    });
  }

  deleteConfig(config: JiraConfigDto): void {
    return;
  }

  onPageChange(event: any): void {}
  onSearchChanged(event: any): void {}
}
