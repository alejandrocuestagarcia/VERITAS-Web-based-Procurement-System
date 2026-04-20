import { Component, OnInit} from '@angular/core';
import {MatTableDataSource} from "@angular/material/table";
import {Pageable, UserDto, UserDtoRoleEnum, UserModuleService} from "../../core/api";
import {PageEvent} from "@angular/material/paginator";

@Component({
  selector: 'app-user-list',
  templateUrl: './user-list.component.html',
  styleUrls: ['./user-list.component.scss']
})
export class UserListComponent implements OnInit {

  dataSource = new MatTableDataSource<UserDto>();
  totalElements = 0;
  displayedColumns: string[] = ['name', 'email', 'team', 'role', 'actions'];
  loading = false;
  inactiveUserCount = 0;
  subtitle = "Configure access levels, audit identities, and manage enterprise-wide user\n" +
    "permissions with precision.";
  title = "User Management";

  userRoles = Object.values(UserDtoRoleEnum);
  selectedRole = "ALL"
  userSessions = 0;

  constructor(private readonly userService: UserModuleService) {}

  ngOnInit(): void {
    this.loadUsers(0, 10);
    this.loadUserStats();
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

    this.userService.getAllUsers(pageable).subscribe({
      next: (response) => {
        console.log(response);
        this.dataSource.data = response.content || [];
        this.loading = false;
      },
      error: (err) => {
        console.error('VERITAS Error:', err);
        this.loading = false;
      }
    });
  }

  onRoleFilterChange(value: any) {
    this.selectedRole = value;
    this.loadUsers(0, 10)

  }

  openEditDialog(user: UserDto) {
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
        this.totalElements = response.total ?? 0;
        this.userSessions = response.activeSessions ?? 0;
      },
      error: (err) => {
        console.error('Error fetching user stats:', err);
      }
    })

  }
}
