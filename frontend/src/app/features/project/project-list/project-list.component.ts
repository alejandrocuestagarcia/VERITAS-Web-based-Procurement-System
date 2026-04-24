import { Component, OnInit, ViewChild } from '@angular/core';
import { ProjectModuleService, ProjectDto } from '../../../core/api';
import { MatTableDataSource } from '@angular/material/table';
import { SharedTableComponent } from '../../../shared/components/table/shared-table.component';

@Component({
  selector: 'app-project-list',
  templateUrl: './project-list.component.html',
})
export class ProjectListComponent implements OnInit {
  @ViewChild(SharedTableComponent) sharedTable!: SharedTableComponent;

  displayedColumns = ['name', 'team', 'budget', 'startDate', 'endDate', 'actions'];
  dataSource = new MatTableDataSource<ProjectDto>([]);

  loading = false;
  error: string | null = null;

  constructor(private projectService: ProjectModuleService) { }

  ngOnInit(): void {
    this.loading = true;
    this.projectService.getAllProjects().subscribe({
      next: (projects) => {
        this.dataSource.data = projects;
        this.loading = false;
      },
      error: () => {
        this.error = 'Failed to load projects.';
        this.loading = false;
      }
    });
  }

  applyFilter(value: string): void {
    this.dataSource.filter = value.trim().toLowerCase();
    if (this.dataSource.paginator) this.dataSource.paginator.firstPage();
  }

  editProject(project: ProjectDto) {
    return ""
  }

  deleteProject(project: ProjectDto) {
    return ""
  }
}
