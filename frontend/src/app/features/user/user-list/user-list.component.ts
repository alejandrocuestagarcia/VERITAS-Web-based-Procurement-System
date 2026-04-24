import { Component, OnInit, ViewChild } from '@angular/core';
import { MatTableDataSource } from "@angular/material/table";
import { Pageable, UserDto, UserDtoRoleEnum, UserModuleService } from "../../../core/api";
import { PageEvent } from "@angular/material/paginator";
import { SharedTableComponent } from "../../../shared/components/table/shared-table.component";

@Component({
  selector: 'app-user-list',
  templateUrl: './user-list.component.html',
})
export class UserListComponent implements OnInit {

  dataSource = new MatTableDataSource<UserDto>();
  totalUserCount = 0;
  totalPageElements = 0;
  displayedColumns: string[] = ['name', 'email', 'team', 'role', 'actions'];
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

  constructor(private readonly userService: UserModuleService) {
  }

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
        console.log(response);
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
    return ""
  }

  deleteUser(user: UserDto) {
    return ""
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
