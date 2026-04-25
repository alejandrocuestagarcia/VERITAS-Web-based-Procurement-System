import { Injectable } from '@angular/core';


export interface NavItem {
  label: string;
  icon: string;
  route: string;
  roles: string[];
}


@Injectable({
  providedIn: 'root'
})
export class NavigationService {
  private readonly allItems: NavItem[] = [

    { label: 'Dashboard', icon: 'dashboard', route: '/dashboard',
      roles: ['REQUESTER', 'PROCUREMENT_OFFICER', 'FINANCE_OFFICER'] },

    { label: 'My Teams Requests', icon: 'description', route: '/requests/team',
      roles: ['REQUESTER', 'PROCUREMENT_OFFICER'] },

    { label: 'All Requests', icon: 'list_alt', route: '/requests/all',
      roles: ['FINANCE_OFFICER'] },

    { label: 'Workflows', icon: 'account_tree', route: '/workflows',
      roles: ['REQUESTER', 'PROCUREMENT_OFFICER', 'FINANCE_OFFICER', 'ADMINISTRATOR'] },

    { label: 'Projects', icon: 'assignment', route: '/projects',
      roles: ['REQUESTER', 'PROCUREMENT_OFFICER', 'FINANCE_OFFICER', 'ADMINISTRATOR'] },

    { label: 'Vendors', icon: 'store', route: '/vendors',
      roles: ['REQUESTER', 'PROCUREMENT_OFFICER'] },

    { label: 'Budgets', icon: 'payments', route: '/budget',
      roles: ['FINANCE_OFFICER'] },

    { label: 'Team Management', icon: 'groups', route: '/teams',
      roles: ['FINANCE_OFFICER', 'ADMINISTRATOR'] },

    { label: 'User Management', icon: 'manage_accounts', route: '/users',
      roles: ['FINANCE_OFFICER', 'ADMINISTRATOR'] },
  ];

  getLinksForRole(userRole: string): NavItem[] {
    return this.allItems.filter(item => item.roles.includes(userRole));
  }
}
