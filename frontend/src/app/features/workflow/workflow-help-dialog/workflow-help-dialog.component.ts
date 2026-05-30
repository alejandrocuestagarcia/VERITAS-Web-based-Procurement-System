// AI-GENERATED
import { Component, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

export type HelpTab = 'structure' | 'steps' | 'transitions' | 'routing';

@Component({
  selector: 'app-workflow-help-dialog',
  templateUrl: './workflow-help-dialog.component.html',
  styles: [`
    .help-code-block {
      background-color: #0f172a;
      color: #e2e8f0;
      font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
      padding: 0.75rem 1rem;
      border-radius: 0.5rem;
      font-size: 0.8125rem;
      overflow-x: auto;
    }
  `]
})
export class WorkflowHelpDialogComponent implements OnInit {
  activeTab: HelpTab = 'structure';

  constructor(
    public dialogRef: MatDialogRef<WorkflowHelpDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { tab?: HelpTab }
  ) {}

  ngOnInit(): void {
    if (this.data?.tab) {
      this.activeTab = this.data.tab;
    }
  }

  setTab(tab: HelpTab): void {
    this.activeTab = tab;
  }

  onClose(): void {
    this.dialogRef.close();
  }
}
