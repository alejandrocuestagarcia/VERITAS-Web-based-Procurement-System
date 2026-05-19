import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import {
  RequisitionModuleService,
  RequisitionDto,
  ProjectModuleService
} from '../../core/api';
import { AuthService } from '../../core/services/auth.service';
import { ToastService } from "../../core/services/toast.service";
import { MatDialog } from '@angular/material/dialog';
import { VendorEvaluationDialogComponent } from '../vendor/vendor-evaluation-dialog/vendor-evaluation-dialog.component';

interface DashboardStats {
  total: number;
  openRequisitions: number;
  finishedRequisitions: number;
  activeProjects: number;
  pendingActions: number;
}

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit {
  public pendingRequisitions: RequisitionDto[] = [];
  public inProgressRequisitions: RequisitionDto[] = [];
  public closedRequisitions: RequisitionDto[] = [];
  public displayedColumns: string[] = ['requestName', 'projectName', 'vendorName', 'status', 'actions'];
  public loading: boolean = false;
  public userName: string = 'User';
  public greeting: string = '';
  public userRole: string = '';

  public stats: DashboardStats = {
    total: 0,
    openRequisitions: 0,
    finishedRequisitions: 0,
    activeProjects: 0,
    pendingActions: 0
  };

  constructor(
    private requisitionService: RequisitionModuleService,
    private projectService: ProjectModuleService,
    private authService: AuthService,
    private toastService: ToastService,
    private router: Router,
    private dialog: MatDialog
  ) { }

  public ngOnInit(): void {
    this.initializeDashboard();
  }

  private initializeDashboard(): void {
    this.userRole = this.authService.getRole() || '';
    this.setGreeting();
    this.fetchUserProfile();
    this.loadDashboardData();
  }

  private loadDashboardData(): void {
    this.loading = true;
    const isRequester = this.authService.hasRole('REQUESTER');

    const dataSources$ = {
      requisitions: this.requisitionService.getRequests(undefined, undefined, undefined, 0, 50),
      projects: this.projectService.getAllProjects()
    };

    forkJoin(dataSources$).pipe(
      catchError(err => {
        this.toastService.showError('Failure during fetching of dashboard data')
        this.loading = false;
        return of(null);
      })
    ).subscribe((data) => {
      if (!data) return;

      this.stats.total = data.requisitions.totalElements || 0;
      this.stats.activeProjects = data.projects.length;

      const recentRequisitions = data.requisitions?.content || [];

      const openRequisitions = recentRequisitions.filter(requisition => !requisition.isClosed);
      const closedRequisitions = recentRequisitions.filter(requisition => requisition.isClosed);

      this.stats.openRequisitions = openRequisitions.length;
      this.stats.finishedRequisitions = closedRequisitions.length;

      if (isRequester) {
        this.pendingRequisitions = openRequisitions.filter(requisition => requisition.responsibleRole === 'REQUESTER' || requisition.state === 'DRAFT');
        this.inProgressRequisitions = openRequisitions.filter(requisition => requisition.responsibleRole !== 'REQUESTER' && requisition.state !== 'DRAFT');
      } else {
        this.pendingRequisitions = recentRequisitions.filter(requisition => requisition.responsibleRole === this.userRole);
        this.inProgressRequisitions = [];
      }

      if (this.authService.hasRole('PROCUREMENT_OFFICER')) {
        this.closedRequisitions = closedRequisitions.filter(req => req.vendorId != null);
      }

      this.stats.pendingActions = this.pendingRequisitions.length;
      this.loading = false;
    });
  }

  private fetchUserProfile(): void {
    const email = this.authService.getDecodedToken()?.sub;
    if (email) {
      this.userName = email;
    }
  }

  private setGreeting(): void {
    const hour = new Date().getHours();
    if (hour < 12) this.greeting = 'Good morning';
    else if (hour < 18) this.greeting = 'Good afternoon';
    else this.greeting = 'Good evening';
  }

  public get roleLabel(): string {
    const labels: Record<string, string> = {
      'ADMINISTRATOR': 'Administrator',
      'FINANCE_OFFICER': 'Finance Officer',
      'PROCUREMENT_OFFICER': 'Procurement Officer',
      'REQUESTER': 'Requester'
    };
    return labels[this.userRole] || this.userRole;
  }

  public get requisitionsScopeLabel(): string {
    if (this.authService.hasAnyRole(['ADMINISTRATOR', 'FINANCE_OFFICER'])) {
      return 'All Requisitions';
    }
    if (this.authService.hasRole('PROCUREMENT_OFFICER')) {
      return 'Department Requisitions';
    }
    return 'My Requisitions';
  }

  public get pendingSectionTitle(): string {
    return 'Awaiting Your Action';
  }

  public get pendingSectionDescription(): string {
    if (this.authService.hasRole('REQUESTER')) {
      return 'Requisitions that have been sent back or require your update';
    }
    return 'Requisitions that need your review or approval';
  }

  public get emptyPendingMessage(): string {
    if (this.authService.hasRole('REQUESTER')) {
      return 'No requisitions require your attention right now. They will appear here if a step is rejected or needs your update.';
    }
    return 'No requisitions are waiting for your action right now. New items will appear here when they need your attention.';
  }

  public viewDetails(id: number | undefined): void {
    if (id !== undefined) {
      this.router.navigate(['/requisitions', id]);
    }
  }

  public formatStatus(status: string | undefined): string {
    return status || 'Unknown';
  }

  public getPriorityIcon(priority: string | undefined): string {
    switch (priority) {
      case 'CRITICAL': return 'error';
      case 'HIGH': return 'priority_high';
      case 'MEDIUM': return 'remove';
      case 'LOW': return 'arrow_downward';
      default: return 'remove';
    }
  }

  public openEvaluationDialog(requisition: RequisitionDto): void {
    if (!requisition.id || !requisition.vendorId) return;

    const dialogRef = this.dialog.open(VendorEvaluationDialogComponent, {
      width: '500px',
      data: {
        vendorId: requisition.vendorId,
        requestId: requisition.id,
        vendorName: requisition.vendorName
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.loadDashboardData();
      }
    });
  }
}
