import {Component, OnInit, ViewChild, HostListener} from '@angular/core';
import {MatTableDataSource} from "@angular/material/table";
import {Pageable, RequisitionDto, UserDto, UserDtoRoleEnum, UserModuleService} from "../../../core/api";
import {PageEvent} from "@angular/material/paginator";
import {SharedTableComponent} from "../../../shared/components/table/shared-table.component";
import {AuthService} from "../../../core/services/auth.service";
import {Router} from "@angular/router";
import {MatDialog} from '@angular/material/dialog';
import {ToastService} from "../../../core/services/toast.service";
import {UserDeletionDialogComponent} from '../user-deletion-dialog/user-deletion-dialog.component';
import { extractErrorMessage } from '../../../shared/utils/error-utils';

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

  activeDropdown: string | null = null;
  dropdownX = 0;
  dropdownY = 0;

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
        this.toastService.showError(extractErrorMessage(err, 'Failed to load users'));
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

  //AI-Generated
  toggleDropdown(name: string, event: MouseEvent): void {
    event.stopPropagation();
    if (this.activeDropdown === name) {
      this.activeDropdown = null;
      return;
    }
    const btn = event.currentTarget as HTMLElement;
    const rect = btn.getBoundingClientRect();

    const estH = 200;
    const estW = 224;
    const spaceBelow = window.innerHeight - rect.bottom - 8;

    this.dropdownY = spaceBelow >= estH
      ? rect.bottom + 6
      : Math.max(8, rect.top - estH - 6);

    this.dropdownX = Math.min(rect.left, window.innerWidth - estW - 8);

    this.activeDropdown = name;
  }

  setRoleFilter(value: string): void {
    this.selectedRole = value || 'ALL';
    this.activeDropdown = null;
    this.sharedTable.resetToFirstPage();
    this.loadUsers(0, 10);
  }

  getSelectedRoleLabel(): string {
    if (this.selectedRole === 'ALL') return 'Role';
    return this.formatRoleLabel(this.selectedRole);
  }

  formatRoleLabel(role: string): string {
    if (!role) return '';
    return role.toLowerCase()
      .split('_')
      .map(word => word.charAt(0).toUpperCase() + word.slice(1))
      .join(' ');
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    const target = event.target as HTMLElement;
    if (!target.closest('.req-filter-pill-wrapper') && !target.closest('.req-dropdown')) {
      this.activeDropdown = null;
    }
  }

  @HostListener('window:scroll', [])
  onWindowScroll(): void {
    this.activeDropdown = null;
  }
//AI-Generated end

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
                    this.toastService.showError(extractErrorMessage(err, 'Failed to deactivate user'));
                  }
                });
              }
            });
          },
          error: (err) => {
            this.toastService.showError(extractErrorMessage(err, 'Failed to load users for fallback selection'));
          }
        });
      },
      error: (err) => {
        this.toastService.showError(extractErrorMessage(err, 'Failed to fetch pending requests'));
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
        this.toastService.showError(extractErrorMessage(err, 'Failed to load user stats'));
      }
    })

  }
}
