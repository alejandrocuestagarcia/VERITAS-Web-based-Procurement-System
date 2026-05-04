import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { LoginComponent } from './features/login/login/login.component';
import { ForcePasswordResetComponent } from './features/login/force-password-reset/force-password-reset.component';
import { ForgotPasswordComponent } from './features/login/forgot-password/forgot-password.component';
import { ResetPasswordComponent } from './features/login/reset-password/reset-password.component';
import { DashboardComponent } from './features/dashboard/dashboard.component';
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import { HTTP_INTERCEPTORS, HttpClientModule } from "@angular/common/http";
import { MatCardModule } from "@angular/material/card";
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { ProjectListComponent } from './features/project/project-list/project-list.component';
import { MatTableModule } from "@angular/material/table";
import { MatPaginatorModule } from "@angular/material/paginator";
import { AuthInterceptor } from "./core/interceptors/AuthInterceptor";
import { UserCreateComponent } from './features/user/user-create/user-create.component';
import { SharedTableComponent } from './shared/components/table/shared-table.component';
import { StatCardComponent } from './shared/components/stat-card/stat-card.component';
import { UserListComponent } from './features/user/user-list/user-list.component';
import { VendorListComponent } from "./features/vendor/vendor-list/vendor-list.component";
import { MatChipsModule } from "@angular/material/chips";
import { FormatEnumPipe } from './shared/pipes/format-enum.pipe';
import { MatTooltipModule } from "@angular/material/tooltip";
import { MatListModule } from "@angular/material/list";
import { HasRoleDirective } from './core/directives/has-role.directive';
import { VendorCreateComponent } from './features/vendor/vendor-create/vendor-create.component';
import { MatSnackBarModule } from "@angular/material/snack-bar";
import { MatSlideToggleModule } from "@angular/material/slide-toggle";
import { MatNativeDateModule, MatOptionModule } from "@angular/material/core";
import { MatSelectModule } from "@angular/material/select";
import { SidebarComponent } from './core/components/sidebar/sidebar.component';
import { BASE_PATH } from './core/api';
import { environment } from '../environments/environment';
import { SharedFormComponent } from './shared/components/creation/shared-form/shared-form.component';
import { SharedFormCardComponent } from './shared/components/creation/shared-form-card/shared-form-card.component';
import { SharedFormFieldComponent } from './shared/components/creation/shared-form-field/shared-form-field.component';
import { ProjectCreateComponent } from './features/project/project-create/project-create.component';
import { TeamListComponent } from './features/team/team-list/team-list.component';
import { TeamCreateComponent } from './features/team/team-create/team-create.component';
import { MatDatepickerModule } from "@angular/material/datepicker";
import { MatSliderModule } from "@angular/material/slider";
import { UserEditComponent } from './features/user/user-edit/user-edit.component';
import {JiraSettingsListComponent} from "./features/integrations/jira/jira-settings/jira-settings-list/jira-settings-list.component";
import {JiraSettingsCreateComponent} from "./features/integrations/jira/jira-settings/jira-settings-create/jira-settings-create.component";
import { ResetPasswordDialogComponent } from './features/login/reset-password-dialog/reset-password-dialog.component';
import {MatDialogModule} from "@angular/material/dialog";
import { WorkflowEditorComponent } from './features/workflow/workflow-editor/workflow-editor.component';
import { JiraIssuesSyncHistoryComponent } from './features/integrations/jira/jira-issues-sync-history/jira-issues-sync-history.component';

@NgModule({
  declarations: [
    AppComponent,
    LoginComponent,
    ForcePasswordResetComponent,
    ForgotPasswordComponent,
    ResetPasswordComponent,
    DashboardComponent,
    UserCreateComponent,
    SharedTableComponent,
    StatCardComponent,
    UserListComponent,
    FormatEnumPipe,
    UserCreateComponent,
    HasRoleDirective,
    ProjectListComponent,
    SidebarComponent,
    VendorCreateComponent,
    SharedFormComponent,
    SharedFormCardComponent,
    SharedFormFieldComponent,
    ProjectCreateComponent,
    VendorListComponent,
    TeamListComponent,
    TeamCreateComponent,
    VendorCreateComponent,
    UserEditComponent,
    ResetPasswordDialogComponent,
    JiraSettingsListComponent,
    JiraSettingsCreateComponent,
    WorkflowEditorComponent,
    JiraIssuesSyncHistoryComponent
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    BrowserAnimationsModule,
    HttpClientModule,
    FormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatIconModule,
    MatButtonModule,
    MatCheckboxModule,
    MatTableModule,
    MatPaginatorModule,
    MatChipsModule,
    MatTooltipModule,
    MatListModule,
    MatPaginatorModule,
    MatSlideToggleModule,
    MatOptionModule,
    MatSelectModule,
    ReactiveFormsModule,
    MatSnackBarModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatSliderModule,
    MatDialogModule
  ],
  providers: [
    {
      provide: HTTP_INTERCEPTORS,
      useClass: AuthInterceptor,
      multi: true
    },
    {
      provide: BASE_PATH,
      useValue: environment.apiUrl
    }
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
