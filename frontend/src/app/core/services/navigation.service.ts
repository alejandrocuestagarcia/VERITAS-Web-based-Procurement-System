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

    {
      label: 'Dashboard', icon: 'dashboard', route: '/dashboard',
      roles: ['REQUESTER', 'PROCUREMENT_OFFICER', 'FINANCE_OFFICER', 'ADMINISTRATOR']
    },
    {
      label: 'Notifications', icon: 'notifications', route: '/notifications',
      roles: ['REQUESTER', 'PROCUREMENT_OFFICER', 'FINANCE_OFFICER', 'ADMINISTRATOR']
    },
    {
      label: 'My Requisitions', icon: 'description', route: '/requisitions',
      roles: ['REQUESTER']
    },
    {
      label: 'Dep. Requisitions', icon: 'description', route: '/requisitions',
      roles: ['PROCUREMENT_OFFICER']
    },
    {
      label: 'All Requisitions', icon: 'description', route: '/requisitions',
      roles: ['FINANCE_OFFICER', 'ADMINISTRATOR']
    },
    {
      label: 'Workflows', icon: 'account_tree', route: '/workflows',
      roles: ['REQUESTER', 'PROCUREMENT_OFFICER', 'FINANCE_OFFICER', 'ADMINISTRATOR']
    },

    {
      label: 'Projects', icon: 'assignment', route: '/projects',
      roles: ['REQUESTER', 'PROCUREMENT_OFFICER', 'FINANCE_OFFICER', 'ADMINISTRATOR']
    },

    {
      label: 'Vendors', icon: 'store', route: '/vendors',
      roles: ['REQUESTER', 'PROCUREMENT_OFFICER']
    },

    {
      label: 'Budgets', icon: 'payments', route: '/budget',
      roles: ['FINANCE_OFFICER']
    },


    {
      label: 'Dep. Management', icon: 'business', route: '/departments',
      roles: ['FINANCE_OFFICER', 'ADMINISTRATOR']
    },


    {
      label: 'Team Management', icon: 'groups', route: '/teams',
      roles: ['FINANCE_OFFICER', 'ADMINISTRATOR']
    },

    {
      label: 'User Management', icon: 'manage_accounts', route: '/users',
      roles: ['FINANCE_OFFICER', 'ADMINISTRATOR']
    },

    {
      label: 'Integrations', icon: 'integration_instructions', route: '/integrations',
      roles: ['ADMINISTRATOR']
    },
  ];

  getLinksForRole(userRole: string): NavItem[] {
    return this.allItems.filter(item => item.roles.includes(userRole));
  }
}
