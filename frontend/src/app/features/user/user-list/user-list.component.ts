import {Component, OnInit, ViewChild} from '@angular/core';
import {MatTableDataSource} from "@angular/material/table";
import {Pageable, RequisitionDto, UserDto, UserDtoRoleEnum, UserModuleService} from "../../../core/api";
import {PageEvent} from "@angular/material/paginator";
import {SharedTableComponent} from "../../../shared/components/table/shared-table.component";
import {AuthService} from "../../../core/services/auth.service";
import {Router} from "@angular/router";
import {MatDialog} from '@angular/material/dialog';
import {ToastService} from "../../../core/services/toast.service";
import {UserDeletionDialogComponent} from '../user-deletion-dialog/user-deletion-dialog.component';

@Component({
  selector: 'app-user-list',
  templateUrl: './user-list.component.html',
})
export class UserListComponent implements OnInit {
  dataSource = new MatTableDataSource<UserDto>();
  totalUserCount = 0;
  totalPageElements = 0;
  displayedColumns: string[] = ['name', 'email', 'team', 'department', 'role', 'actions'];
  loading = false;
  inactiveUserCount = 0;
  subtitle = "Configure access levels, audit identities, and manage enterprise-wide user\n" +
    "permissions with precision.";
  title = "User Management";

  userRoles = Object.values(UserDtoRoleEnum);
  selectedRole = "ALL"
  userSessions = 0;
  currentSearchString = "";

  @ViewChild(SharedTableComponent) sharedTable!: SharedTableComponent;

  constructor(
    private readonly userService: UserModuleService,
    private router: Router,
    protected authService: AuthService,
    private dialog: MatDialog,
    private toastService: ToastService
  ) { }

  ngOnInit(): void {
    this.loadUsers(0, 10);
    this.loadUserStats();
  }

  onSearchChanged(value: string) {
    this.currentSearchString = value;
    this.sharedTable.resetToFirstPage();
    this.loadUsers(0, 10);
  }

  onPageChange(event: PageEvent): void {
    this.loadUsers(event.pageIndex, event.pageSize);
  }

  private loadUsers(page: number, size: number): void {
    this.loading = true;

    const pageable: Pageable = {
      page: page,
      size: size,
      sort: ['name,asc']
    };

    const roleParam = this.selectedRole === 'ALL' ? undefined : (this.selectedRole as UserDtoRoleEnum);

    this.userService.getAllUsers(pageable, this.currentSearchString, roleParam).subscribe({
      next: (response) => {
        this.dataSource.data = response.content || [];
        this.totalPageElements = response.totalElements || 0;
        this.loading = false;
      },
      error: (err) => {
        console.error('VERITAS Error:', err);
        this.loading = false;
      }
    });
  }

  onRoleFilterChange(value: any) {
    const newValue = value ?? 'ALL';

    if (this.selectedRole === newValue && value === undefined) {
      this.selectedRole = 'ALL';
      return;
    }
    this.sharedTable.resetToFirstPage();

    this.selectedRole = value;
    this.loadUsers(0, 10)

  }

  editUser(user: UserDto) {
    this.router.navigate(['/users/edit', user.id]);
  }

  deleteUser(user: UserDto) {
    this.userService.getPendingRequisitions(user.id!).subscribe({
      next: (pendingRequests: RequisitionDto[]) => {
        this.userService.getAllUsers({ page: 0, size: 1000 }, "").subscribe({
          next: (response) => {
            const fallbackUsers = response.content!.filter(u => u.id !== user.id && u.active !== false && u.role === UserDtoRoleEnum.Requester && u.teamId === user.teamId);

            const dialogReturnValue = this.dialog.open(UserDeletionDialogComponent, {
              width: '500px',
              data: {
                user: user,
                pendingRequests: pendingRequests,
                fallbackUsers: fallbackUsers
              }
            });

            dialogReturnValue.afterClosed().subscribe(fallbackUserId => {
              if (fallbackUserId !== undefined) {
                this.userService.deleteByUserId(user.id!, fallbackUserId).subscribe({
                  next: () => {
                    this.toastService.showSuccess('User deactivated successfully');
                    this.loadUsers(0, 10);
                    this.loadUserStats();
                  },
                  error: (err) => {
                    this.toastService.showError("User Deactivation failed: " + err.message);
                  }
                });
              }
            });
          },
          error: (err) => {
            this.toastService.showError("Could not fetch users for fallback selection: " + err.message);
          }
        });
      },
      error: (err) => {
        this.toastService.showError("Could not fetch pending requests: " + err.message);
      }
    });
  }

  protected readonly UserDtoRoleEnum = UserDtoRoleEnum;

  private loadUserStats() {
    this.userService.getUserStats().subscribe({
      next: (response) => {
        this.inactiveUserCount = response.inactive ?? 0;
        this.totalUserCount = response.total ?? 0;
        this.userSessions = response.activeSessions ?? 0;
      },
      error: (err) => {
        console.error('Error fetching user stats:', err);
      }
    })

  }
}
