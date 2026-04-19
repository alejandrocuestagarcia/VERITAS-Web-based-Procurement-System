import {AfterViewInit, Component, OnInit, ViewChild} from '@angular/core';
import { ProjectModuleService, ProjectDto} from '../../core/api';
import {MatPaginator} from "@angular/material/paginator";
import {MatTableDataSource} from "@angular/material/table";

@Component({
  selector: 'app-project-list',
  templateUrl: './project-list.component.html',
  styleUrls: ['./project-list.component.scss']
})
export class ProjectListComponent implements OnInit, AfterViewInit {
  @ViewChild(MatPaginator) paginator!: MatPaginator;

  displayedColumns = ['name', 'team', 'budget', 'startDate', 'endDate'];
  dataSource = new MatTableDataSource<ProjectDto>([]);

  loading = false;
  error: string | null = null;

  constructor(private projectService: ProjectModuleService) {}

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

  ngAfterViewInit(): void {
    this.dataSource.paginator = this.paginator;
  }

  applyFilter(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.dataSource.filter = value.trim().toLowerCase();
    if (this.dataSource.paginator) this.dataSource.paginator.firstPage();
  }
}
