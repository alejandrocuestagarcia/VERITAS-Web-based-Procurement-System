import { Component, OnInit, ViewChild, AfterViewInit } from '@angular/core';
import { Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import { MatTableDataSource } from '@angular/material/table';
import { SharedTableComponent } from '../../../shared/components/table/shared-table.component';
import { ToastService } from '../../../core/services/toast.service';
import {
  DepartmentDto,
  DepartmentsModuleService,
  TeamDto,
  TeamsModuleService
} from '../../../core/api';
import { MatDialog } from '@angular/material/dialog';
import { ConfirmationDialogComponent } from '../../../shared/components/confirmation-dialog/confirmation-dialog.component';

interface DepartmentRow {
  id: number;
  name: string;
  teamsCount: number;
  budget: number;
  committedSpend: number;
  actualSpend: number;
}

@Component({
  selector: 'app-department-list',
  templateUrl: './department-list.component.html',
  styleUrls: ['./department-list.component.scss']
})
export class DepartmentListComponent implements OnInit, AfterViewInit {
  @ViewChild(SharedTableComponent) sharedTable!: SharedTableComponent;

  displayedColumns: string[] = ['name', 'teams', 'budget', 'committed', 'actual', 'actions'];
  dataSource = new MatTableDataSource<DepartmentRow>([]);

  loading = false;
  error: string | null = null;
  private currentSearchString = '';

  totalDepartments = 0;
  totalTeams = 0;

  constructor(
    private departmentsService: DepartmentsModuleService,
    private teamsService: TeamsModuleService,
    private toastService: ToastService,
    private router: Router,
    private dialog: MatDialog
  ) { }

  ngOnInit(): void {
    this.loadData();
  }

  ngAfterViewInit(): void {
    if (this.sharedTable) {
      this.dataSource.paginator = this.sharedTable.paginator;
    }
    this.dataSource.filterPredicate = (data: DepartmentRow, filter: string) => {
      return data.name.toLowerCase().includes(filter);
    };
  }

  onSearchChanged(value: string): void {
    this.currentSearchString = value.trim().toLowerCase();
    this.dataSource.filter = this.currentSearchString;
    if (this.dataSource.paginator) {
      this.dataSource.paginator.firstPage();
    }
  }

  navigateToAddDepartment(): void {
    this.router.navigate(['/departments/create']);
  }

  navigateToEditDepartment(id: number): void {
    this.router.navigate([`/departments/edit/${id}`]);
  }

  deleteDepartment(row: DepartmentRow): void {
    const ref = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        title: 'Delete Department',
        message: `Are you sure you want to delete "${row.name}"?`
      }
    });

    ref.afterClosed().subscribe((confirmed) => {
      if (confirmed) {
        this.departmentsService.deleteDepartment(row.id).subscribe({
          next: () => {
            this.toastService.showSuccess('Department deleted successfully.');
            this.loadData();
          },
          error: () => {
            this.toastService.showError('Failed to delete department. Ensure it is not assigned to any team.');
          }
        });
      }
    });
  }

  private loadData(): void {
    this.loading = true;
    this.error = null;

    forkJoin({
      departments: this.departmentsService.getAllDepartments(),
      teams: this.teamsService.getAllTeams()
    }).subscribe({
      next: ({ departments, teams }) => {
        const normalizedDepts = this.toArray<DepartmentDto>(departments);
        const normalizedTeams = this.toArray<TeamDto>(teams);

        const rows = normalizedDepts.map((dept, index) => {
          const deptId = dept.id ?? index + 1;
          const deptName = dept.name;

          const teamsInDept = normalizedTeams.filter(
            (t) => t.department?.toLowerCase() === deptName.toLowerCase()
          ).length;

          return {
            id: deptId,
            name: deptName,
            teamsCount: teamsInDept,
            budget: dept.budget || 0,
            committedSpend: dept.committedSpend || 0,
            actualSpend: dept.actualSpend || 0
          };
        });

        this.dataSource.data = rows;
        this.totalDepartments = rows.length;
        this.totalTeams = normalizedTeams.length;
        this.loading = false;
      },
      error: () => {
        this.error = 'Failed to load department governance data.';
        this.loading = false;
      }
    });
  }

  private toArray<T>(value: unknown): T[] {
    if (Array.isArray(value)) {
      return value as T[];
    }

    if (!value || typeof value !== 'object') {
      return [];
    }

    const wrappers = ['content', 'items', 'data', 'results'];
    for (const key of wrappers) {
      const candidate = (value as Record<string, unknown>)[key];
      if (Array.isArray(candidate)) {
        return candidate as T[];
      }
    }

    return [];
  }
}
