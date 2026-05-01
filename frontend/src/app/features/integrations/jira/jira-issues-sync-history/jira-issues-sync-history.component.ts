import {Component, OnInit} from '@angular/core';
import {MatTableDataSource} from "@angular/material/table";
import { Location } from '@angular/common';
import {JiraConfigControllerService} from "../../../../core/api";

@Component({
  selector: 'app-jira-settings-sync-history',
  templateUrl: './jira-issues-sync-history.component.html'
})
export class JiraIssuesSyncHistoryComponent implements OnInit {
  loading = false;
  totalElements = 0;
  displayedColumns = ['requestName', 'requestKey', 'user', 'timestamp', 'action'];
  dataSource = new MatTableDataSource<any>([]);

  constructor(
    private jiraConfigService: JiraConfigControllerService,
    private location: Location
  ) {}

  ngOnInit() {
    this.loadAuditLogs();
  }

  loadAuditLogs() {
    this.loading = true;
    this.jiraConfigService.getJiraSyncAudit().subscribe({
      next: (logs) => {
        this.dataSource.data = logs;
        this.totalElements = logs.length;
        this.loading = false;
      },
      error: () => this.loading = false
    });
  }

  goBack() {
    this.location.back();
  }

  onSearchChanged(value: string): void {
    this.dataSource.filter = value.trim().toLowerCase();
    if (this.dataSource.paginator) this.dataSource.paginator.firstPage();
  }

}
