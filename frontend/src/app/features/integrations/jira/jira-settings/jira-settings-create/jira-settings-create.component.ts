import {Component, OnInit} from '@angular/core';
import {FormBuilder, FormGroup, Validators} from '@angular/forms';
import { ToastService } from '../../../../../core/services/toast.service';
import {ActivatedRoute, Router} from '@angular/router';
import {JiraConfigControllerService} from '../../../../../core/api/api/jiraConfigController.service';
import {UserModuleService} from '../../../../../core/api/api/userModule.service';
import {ProjectModuleService} from '../../../../../core/api/api/projectModule.service';
import {WorkflowModuleService} from '../../../../../core/api/api/workflowModule.service';
import {UserDto, ProjectDto, WorkflowDto} from '../../../../../core/api/model/models';

@Component({
  selector: 'app-jira-settings-create',
  templateUrl: './jira-settings-create.component.html',
})
export class JiraSettingsCreateComponent implements OnInit {
  settingsForm!: FormGroup;
  selectedSettingsId: number | null = null;
  isEditing = false;
  loading = false;
  testingConnection = false;
  isTokenSet = false;

  users: UserDto[] = [];
  projects: ProjectDto[] = [];
  workflows: WorkflowDto[] = [];

  constructor(
    private fb: FormBuilder,
    private jiraConfigService: JiraConfigControllerService,
    private toastService: ToastService,
    private route: ActivatedRoute,
    private router: Router,
    private userService: UserModuleService,
    private projectService: ProjectModuleService,
    private workflowService: WorkflowModuleService
  ) {
  }

  ngOnInit(): void {
    this.initForm();
    this.loadDropdownData();

    // Check if editing
    this.route.paramMap.subscribe(params => {
      const idParam = params.get('id');
      if (idParam) {
        this.selectedSettingsId = +idParam;
        this.isEditing = true;
        this.loadConfigForEdit(this.selectedSettingsId);
      }
    });
  }

  private loadDropdownData(): void {
    this.userService.getAllUsers({ page: 0, size: 1000 }).subscribe(res => {
      this.users = res.content || [];
    });
    this.projectService.getAllProjects().subscribe(res => {
      this.projects = res || [];
    });
    this.workflowService.getAllWorkflows({ page: 0, size: 1000 }).subscribe(res => {
      this.workflows = res.content || [];
    });
  }

  private initForm(): void {
    this.settingsForm = this.fb.group({
      name: ['', Validators.required],
      jiraUrl: ['', Validators.required],
      username: ['', Validators.required],
      apiToken: [''],
      jql: ['', Validators.required],
      syncIntervalMinutes: [60, [Validators.required, Validators.min(1)]],
      customFieldId: ['', Validators.required],
      fallbackUserId: [null, Validators.required],
      fallbackProjectId: [null, Validators.required],
      fallbackWorkflowId: [null, Validators.required]
    });
  }

  loadConfigForEdit(id: number): void {
    this.loading = true;
    this.jiraConfigService.getConfigById(id).subscribe({
      next: (config) => {
        this.isTokenSet = config.isTokenSet || false;
        this.settingsForm.patchValue({
          name: config.name,
          jiraUrl: config.jiraUrl,
          username: config.username,
          apiToken: '', // Keep empty for security
          jql: config.jql,
          syncIntervalMinutes: config.syncIntervalMinutes,
          customFieldId: config.customFieldId,
          fallbackUserId: config.fallbackUserId,
          fallbackProjectId: config.fallbackProjectId,
          fallbackWorkflowId: config.fallbackWorkflowId
        });
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.toastService.showError('Failed to load config');
        this.router.navigate(['/integrations']);
      }
    });
  }

  onCancel(): void {
    this.router.navigate(['/integrations']);
  }

  onSubmit(): void {
    if (this.settingsForm.valid) {
      this.loading = true;
      const formValue = this.settingsForm.value;

      if (this.isEditing && this.selectedSettingsId) {
        this.jiraConfigService.updateConfig(this.selectedSettingsId, formValue).subscribe({
          next: () => {
            this.toastService.showSuccess('Config updated successfully');
            this.router.navigate(['/integrations']);
          },
          error: () => {
            this.loading = false;
            this.toastService.showError('Failed to update config');
          }
        });
      } else {
        this.jiraConfigService.createConfig(formValue).subscribe({
          next: () => {
            this.toastService.showSuccess('Config created successfully');
            this.router.navigate(['/integrations']);
          },
          error: () => {
            this.loading = false;
            this.toastService.showError('Failed to create config');
          }
        });
      }
    } else {
      this.settingsForm.markAllAsTouched();
      this.toastService.showError('Please correct the highlighted errors before submitting.');
    }
  }

  testConnection(): void {
    const connectionFields = ['name', 'jiraUrl', 'username', 'apiToken'];
    let hasError = false;

    connectionFields.forEach(field => {
      const control = this.settingsForm.get(field);
      if (control?.invalid) {
        control.markAsTouched();
        hasError = true;
      }
    });

    if (hasError) return;

    this.testingConnection = true;
    const formValue = this.settingsForm.value;

    this.jiraConfigService.testConnection(formValue).subscribe({
      next: (res: any) => {
        this.testingConnection = false;
        if (res && res.success) {
          this.toastService.showSuccess('Connection successful!');
        } else {
          this.toastService.showError('Connection failed. Check credentials.');
        }
      },
      error: () => {
        this.testingConnection = false;
        this.toastService.showError('Error testing connection.');
      }
    });
  }
}
