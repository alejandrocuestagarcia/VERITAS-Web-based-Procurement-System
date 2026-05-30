// AI-GENERATED
import { Component, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

export type HelpTab = 'structure' | 'steps' | 'transitions' | 'routing';

@Component({
  selector: 'app-workflow-help-dialog',
  templateUrl: './workflow-help-dialog.component.html'
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
