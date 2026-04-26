import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { LoginComponent } from "./features/login/login.component";
import { authGuard } from "./core/guards/auth.guard";
import { DashboardComponent } from "./features/dashboard/dashboard.component";
import { ProjectListComponent } from "./features/project/project-list/project-list.component";
import { UserCreateComponent } from "./features/user/user-create/user-create.component";
import { UserListComponent } from "./features/user/user-list/user-list.component";
import { guestGuard } from "./core/guards/guest.guard";
import { VendorCreateComponent } from "./features/vendor/vendor-create/vendor-create.component";
import { ProjectCreateComponent } from "./features/project/project-create/project-create.component";
import { VendorListComponent } from "./features/vendor/vendor-list/vendor-list.component";
import { TeamListComponent } from "./features/team/team-list/team-list.component";
import { TeamCreateComponent } from "./features/team/team-create/team-create.component";

const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'users/create', component: UserCreateComponent },
  { path: 'users', component: UserListComponent },
  {
    path: 'login',
    component: LoginComponent,
    canActivate: [guestGuard]
  },
  {
    path: 'projects/create',
    component: ProjectCreateComponent,
    canActivate: [authGuard]
  },
  {
    path: 'projects',
    component: ProjectListComponent,
    canActivate: [authGuard]
  },
  {
    path: 'teams',
    component: TeamListComponent,
    canActivate: [authGuard]
  },
  {
    path: 'teams/add',
    component: TeamCreateComponent,
    canActivate: [authGuard]
  },
  {
    path: 'dashboard',
    component: DashboardComponent,
    canActivate: [authGuard]
  },
  {
    path: 'vendors/create',
    component: VendorCreateComponent,
    canActivate: [authGuard]
  },
  {
    path: 'vendors',
    component: VendorListComponent,
    canActivate: [authGuard]
  },
  { path: 'requisition/new', redirectTo: '/dashboard' }, //change later to the right page
  { path: '', redirectTo: '/dashboard', pathMatch: 'full' },
  { path: '**', redirectTo: '/dashboard' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
