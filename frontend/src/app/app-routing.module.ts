import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';
import {LoginComponent} from "./features/login/login.component";
import {authGuard} from "./core/guards/auth.guard";
import {DashboardComponent} from "./features/dashboard/dashboard.component";
import {ProjectListComponent} from "./features/project/project-list/project-list.component";

const routes: Routes = [
  { path: 'login', component: LoginComponent },
  {
    path: 'projects',
    component: ProjectListComponent,
    canActivate: [authGuard]
  },
  {
    path: 'dashboard',
    component: DashboardComponent,
    canActivate: [authGuard]
  },
  { path: '', redirectTo: '/dashboard', pathMatch: 'full' },
  { path: '**', redirectTo: '/dashboard' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
