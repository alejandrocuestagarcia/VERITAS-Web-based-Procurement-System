import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';
import {LoginComponent} from "./features/login/login/login.component";
import { ForcePasswordResetComponent } from './features/login/force-password-reset/force-password-reset.component';
import { ForgotPasswordComponent } from './features/login/forgot-password/forgot-password.component';
import { ResetPasswordComponent } from './features/login/reset-password/reset-password.component';
import {authGuard} from "./core/guards/auth.guard";
import {DashboardComponent} from "./features/dashboard/dashboard.component";
import {ProjectListComponent} from "./features/project/project-list/project-list.component";
import {UserCreateComponent} from "./features/user/user-create/user-create.component";
import {UserListComponent} from "./features/user/user-list/user-list.component";
import {guestGuard} from "./core/guards/guest.guard";
import {VendorCreateComponent} from "./features/vendor/vendor-create/vendor-create.component";
import {ProjectCreateComponent} from "./features/project/project-create/project-create.component";
import {VendorListComponent} from "./features/vendor/vendor-list/vendor-list.component";
import {UserEditComponent} from "./features/user/user-edit/user-edit.component";
import {roleGuard} from "./core/guards/role.guard";
import { TeamListComponent } from "./features/team/team-list/team-list.component";
import { TeamCreateComponent } from "./features/team/team-create/team-create.component";
import { JiraSettingsListComponent } from "./features/integrations/jira/jira-settings/jira-settings-list/jira-settings-list.component";
import { JiraSettingsCreateComponent } from "./features/integrations/jira/jira-settings/jira-settings-create/jira-settings-create.component";
import {WorkflowEditorComponent} from "./features/workflow/workflow-editor/workflow-editor.component";
import {JiraIssuesSyncHistoryComponent} from "./features/integrations/jira/jira-issues-sync-history/jira-issues-sync-history.component";

const routes: Routes = [
  {
    path: 'login',
    component: LoginComponent,
    canActivate: [guestGuard]
  },
  {
    path: 'force-password-reset',
    component: ForcePasswordResetComponent,
    canActivate: [authGuard]
  },
  { path: 'forgot-password', 
    component: ForgotPasswordComponent, 
    canActivate: [guestGuard] 
  },
  { path: 'reset-password', 
    component: ResetPasswordComponent, 
    canActivate: [guestGuard] },
  {
    path: 'users',
    component: UserListComponent,
    canActivate: [authGuard, roleGuard],
    data: { roles: ['ADMINISTRATOR', 'FINANCE_OFFICER'] }
  },
  {
    path: 'users/create',
    component: UserCreateComponent,
    canActivate: [authGuard, roleGuard],
    data: { roles: ['ADMINISTRATOR', 'FINANCE_OFFICER'] }
  },
  {
    path: 'users/edit/:id',
    component: UserEditComponent,
    canActivate: [authGuard, roleGuard],
    data: { roles: ['ADMINISTRATOR', 'FINANCE_OFFICER'] }
  },
  {
    path: 'projects',
    component: ProjectListComponent,
    canActivate: [authGuard]
  },
  {
    path: 'projects/create',
    component: ProjectCreateComponent,
    canActivate: [authGuard, roleGuard],
    data: { roles: ['ADMINISTRATOR', 'FINANCE_OFFICER'] }
  },
  {
    path: 'teams',
    component: TeamListComponent,
    canActivate: [authGuard, roleGuard],
    data: { roles: ['ADMINISTRATOR', 'FINANCE_OFFICER'] }
  },
  {
    path: 'teams/create',
    component: TeamCreateComponent,
    canActivate: [authGuard, roleGuard],
    data: { roles: ['ADMINISTRATOR', 'FINANCE_OFFICER'] }
  },
  {
    path: 'dashboard',
    component: DashboardComponent,
    canActivate: [authGuard]
  },
  {
    path: 'vendors',
    component: VendorListComponent,
    canActivate: [authGuard, roleGuard],
    data: { roles: ['REQUESTER', 'PROCUREMENT_OFFICER'] }
  },
  {
    path: 'vendors/create',
    component: VendorCreateComponent,
    canActivate: [authGuard, roleGuard],
    data: { roles: ['PROCUREMENT_OFFICER'] }
  },
  {
    path: 'workflows/view/:id',
    component: WorkflowEditorComponent,
    canActivate: [authGuard, roleGuard],
    data: { mode: 'view', roles: ['PROCUREMENT_OFFICER', 'FINANCE_OFFICER'] }
  },
  {
    path: 'workflows/create',
    component: WorkflowEditorComponent,
    canActivate: [authGuard, roleGuard],
    data: { mode: 'create', roles: ['FINANCE_OFFICER'] }
  },
  {
    path: 'workflows/edit/:id',
    component: WorkflowEditorComponent,
    canActivate: [authGuard, roleGuard],
    data: { mode: 'edit', roles: ['FINANCE_OFFICER'] }
  },
  { path: 'requisition/new', redirectTo: '/dashboard' }, //change later to the right page
  {
    path: 'integrations',
    component: JiraSettingsListComponent,
    canActivate: [authGuard, roleGuard],
    data: { roles: ['ADMINISTRATOR'] }
  },
  {
    path: 'integrations/create',
    component: JiraSettingsCreateComponent,
    canActivate: [authGuard, roleGuard],
    data: { roles: ['ADMINISTRATOR'] }
  },
  {
    path: 'integrations/edit/:id',
    component: JiraSettingsCreateComponent,
    canActivate: [authGuard, roleGuard],
    data: { roles: ['ADMINISTRATOR'] }
  },
  {
    path: 'integrations/sync-history',
    component: JiraIssuesSyncHistoryComponent,
    canActivate: [authGuard, roleGuard],
    data: { roles: ['ADMINISTRATOR'] }
  },
  { path: 'requisition/new', redirectTo: '/dashboard' },
  { path: '', redirectTo: '/dashboard', pathMatch: 'full' },
  { path: '**', redirectTo: '/dashboard' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
