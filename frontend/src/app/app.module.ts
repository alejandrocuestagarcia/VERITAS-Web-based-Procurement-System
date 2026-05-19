import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientModule, HTTP_INTERCEPTORS } from "@angular/common/http";
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

// Material Modules
import { MatCardModule } from "@angular/material/card";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { MatFormFieldModule, MAT_FORM_FIELD_DEFAULT_OPTIONS } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatTableModule } from "@angular/material/table";
import { MatPaginatorModule } from "@angular/material/paginator";
import { MatChipsModule } from "@angular/material/chips";
import { MatTooltipModule } from "@angular/material/tooltip";
import { MatListModule } from "@angular/material/list";
import { MatSnackBarModule } from "@angular/material/snack-bar";
import { MatSlideToggleModule } from "@angular/material/slide-toggle";
import { MatNativeDateModule, MatOptionModule } from "@angular/material/core";
import { MatSelectModule } from "@angular/material/select";
import { MatDatepickerModule } from "@angular/material/datepicker";
import { MatSliderModule } from "@angular/material/slider";
import { MatDialogModule } from "@angular/material/dialog";
import { MatStepperModule } from "@angular/material/stepper";
import { MatAutocompleteModule } from '@angular/material/autocomplete';

// Routing & App
import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { environment } from '../environments/environment';
import { BASE_PATH } from './core/api';
import { AuthInterceptor } from "./core/interceptors/AuthInterceptor";

// Directives & Pipes
import { HasRoleDirective } from './core/directives/has-role.directive';
import { FormatEnumPipe } from './shared/pipes/format-enum.pipe';

// Core Components
import { SidebarComponent } from './core/components/sidebar/sidebar.component';
import { DashboardComponent } from './features/dashboard/dashboard.component';
import { LoginComponent } from './features/login/login/login.component';
import { ForcePasswordResetComponent } from './features/login/force-password-reset/force-password-reset.component';
import { ForgotPasswordComponent } from './features/login/forgot-password/forgot-password.component';
import { ResetPasswordComponent } from './features/login/reset-password/reset-password.component';
import { ResetPasswordDialogComponent } from './features/login/reset-password-dialog/reset-password-dialog.component';

// Shared Components
import { SharedTableComponent } from './shared/components/table/shared-table.component';
import { StatCardComponent } from './shared/components/stat-card/stat-card.component';
import { SharedFormComponent } from './shared/components/creation/shared-form/shared-form.component';
import { SharedFormCardComponent } from './shared/components/creation/shared-form-card/shared-form-card.component';
import { SharedFormFieldComponent } from './shared/components/creation/shared-form-field/shared-form-field.component';
import { ConfirmationDialogComponent } from './shared/components/confirmation-dialog/confirmation-dialog.component';
import { PriorityBadgeComponent } from './shared/components/priority-badge/priority-badge.component';

// Features
import { UserCreateComponent } from './features/user/user-create/user-create.component';
import { UserListComponent } from './features/user/user-list/user-list.component';
import { UserEditComponent } from './features/user/user-edit/user-edit.component';
import { UserDeletionDialogComponent } from './features/user/user-deletion-dialog/user-deletion-dialog.component';

import { ProjectListComponent } from './features/project/project-list/project-list.component';
import { ProjectCreateComponent } from './features/project/project-create/project-create.component';

import { VendorListComponent } from "./features/vendor/vendor-list/vendor-list.component";
import { VendorCreateComponent } from './features/vendor/vendor-create/vendor-create.component';
import { VendorEditComponent } from './features/vendor/vendor-edit/vendor-edit.component';
import { VendorEvaluationDialogComponent } from './features/vendor/vendor-evaluation-dialog/vendor-evaluation-dialog.component';

import { TeamListComponent } from './features/team/team-list/team-list.component';
import { TeamCreateComponent } from './features/team/team-create/team-create.component';
import { TeamEditComponent } from './features/team/team-edit/team-edit.component';

import { DepartmentListComponent } from './features/department/department-list/department-list.component';
import { DepartmentCreateComponent } from './features/department/department-create/department-create.component';

import { WorkflowEditorComponent } from './features/workflow/workflow-editor/workflow-editor.component';
import { WorkflowListComponent } from './features/workflow/workflow-list/workflow-list.component';

import { JiraSettingsListComponent } from "./features/integrations/jira/jira-settings/jira-settings-list/jira-settings-list.component";
import { JiraSettingsCreateComponent } from "./features/integrations/jira/jira-settings/jira-settings-create/jira-settings-create.component";
import { JiraIssuesSyncHistoryComponent } from './features/integrations/jira/jira-issues-sync-history/jira-issues-sync-history.component';

import { RequisitionCreateComponent } from './features/requisition/requisition-create/requisition-create.component';
import { RequisitionListComponent } from './features/requisition/requisition-list/requisition-list.component';
import { RequisitionDetailComponent } from './features/requisition/requisition-detail/requisition-detail.component';
import { RequisitionVendorQuotesComponent } from './features/requisition/requisition-vendor-quotes/requisition-vendor-quotes-list/requisition-vendor-quotes.component';
import { RequisitionVendorQuoteCreateComponent } from './features/requisition/requisition-vendor-quotes/requisition-vendor-quote-create/requisition-vendor-quote-create.component';
import { RequisitionVendorQuoteViewComponent } from './features/requisition/requisition-vendor-quotes/requisition-vendor-quote-view/requisition-vendor-quote-view.component';
import { RequisitionChangeRequesterDialogComponent } from './features/requisition/requisition-change-requester-dialog/requisition-change-requester-dialog.component';
import { RejectDialogComponent } from './shared/components/reject-dialog/reject-dialog.component';

@NgModule({
  declarations: [
    AppComponent,
    LoginComponent,
    ForcePasswordResetComponent,
    ForgotPasswordComponent,
    ResetPasswordComponent,
    DashboardComponent,
    SidebarComponent,

    // User
    UserCreateComponent,
    UserListComponent,
    UserEditComponent,
    UserDeletionDialogComponent,
    ResetPasswordDialogComponent,

    // Project
    ProjectListComponent,
    ProjectCreateComponent,

    // Vendor
    VendorListComponent,
    VendorCreateComponent,
    VendorEditComponent,
    VendorEvaluationDialogComponent,

    // Team
    TeamListComponent,
    TeamCreateComponent,
    TeamEditComponent,

    // Department
    DepartmentListComponent,
    DepartmentCreateComponent,

    // Workflow & Jira
    WorkflowEditorComponent,
    WorkflowListComponent,
    JiraSettingsListComponent,
    JiraSettingsCreateComponent,
    JiraIssuesSyncHistoryComponent,

    // Requisition & Procurement
    RequisitionCreateComponent,
    RequisitionListComponent,
    RequisitionDetailComponent,
    RequisitionVendorQuotesComponent,
    RequisitionVendorQuoteCreateComponent,
    RequisitionVendorQuoteViewComponent,
    RequisitionChangeRequesterDialogComponent,

    // Shared
    SharedTableComponent,
    StatCardComponent,
    SharedFormComponent,
    SharedFormCardComponent,
    SharedFormFieldComponent,
    ConfirmationDialogComponent,
    PriorityBadgeComponent,
    RejectDialogComponent,

    // Directives & Pipes
    HasRoleDirective,
    FormatEnumPipe
  ],
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    HttpClientModule,
    FormsModule,
    ReactiveFormsModule,
    CommonModule,
    RouterModule,
    AppRoutingModule,

    // Material
    MatCardModule,
    MatProgressSpinnerModule,
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
    MatSnackBarModule,
    MatSlideToggleModule,
    MatNativeDateModule,
    MatOptionModule,
    MatSelectModule,
    MatDatepickerModule,
    MatSliderModule,
    MatDialogModule,
    MatStepperModule,
    MatAutocompleteModule
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
    },
    {
      provide: MAT_FORM_FIELD_DEFAULT_OPTIONS,
      useValue: { subscriptSizing: 'dynamic', appearance: 'outline' }
    }
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
