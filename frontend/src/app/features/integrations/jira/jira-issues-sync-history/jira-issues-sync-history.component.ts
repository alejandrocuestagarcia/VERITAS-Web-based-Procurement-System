import { Component, OnInit, ViewChild } from '@angular/core';
import { MatTableDataSource } from "@angular/material/table";
import { Location } from '@angular/common';
import { JiraConfigControllerService } from "../../../../core/api";
import { PageEvent } from "@angular/material/paginator";
import { SharedTableComponent } from "../../../../shared/components/table/shared-table.component";

@Component({
  selector: 'app-jira-settings-sync-history',
  templateUrl: './jira-issues-sync-history.component.html'
})
export class JiraIssuesSyncHistoryComponent implements OnInit {
  loading = false;
  displayedColumns = ['requestName', 'requestKey', 'user', 'timestamp', 'action'];
  dataSource = new MatTableDataSource<any>([]);
  totalElements = 0;
  currentSearchString = "";
  private readonly PAGE_SIZE = 10;

  @ViewChild(SharedTableComponent) sharedTable!: SharedTableComponent;

  constructor(
    private jiraConfigService: JiraConfigControllerService,
    private location: Location
  ) {}

  ngOnInit() {
    this.loadAuditLogs(0, this.PAGE_SIZE);
  }

  loadAuditLogs(page: number, size: number) {
    this.loading = true;

    this.jiraConfigService.getJiraSyncAudit(page, size, this.currentSearchString).subscribe({
      next: (response) => {
        this.dataSource.data = response.content || [];
        this.totalElements = response.totalElements || 0;
        this.loading = false;
      },
      error: () => this.loading = false
    });
  }

  onPageChange(event: PageEvent): void {
    this.loadAuditLogs(event.pageIndex, event.pageSize);
  }

  onSearchChanged(value: string): void {
    this.currentSearchString = value;
    this.sharedTable.resetToFirstPage();
    this.loadAuditLogs(0, this.PAGE_SIZE);
  }

  goBack() {
    this.location.back();
  }

  getActionLabel(action: string): string {
    switch (action) {
      case 'JIRA_SYNC': return 'Sync';
      case 'JIRA_UNSYNC': return 'Unsync';
      case 'JIRA_COMMENT_POSTED': return 'Comment Posted';
      default: return action;
    }
  }

  getActionClass(action: string): string {
    switch (action) {
      case 'JIRA_SYNC': return 'bg-green-100 text-green-800 border border-green-200';
      case 'JIRA_UNSYNC': return 'bg-rose-100 text-rose-800 border border-rose-200';
      case 'JIRA_COMMENT_POSTED': return 'bg-blue-100 text-blue-800 border border-blue-200';
      default: return 'bg-slate-100 text-slate-800 border border-slate-200';
    }
  }
}
