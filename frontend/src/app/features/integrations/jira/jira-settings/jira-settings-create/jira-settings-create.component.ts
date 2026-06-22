import {Component, OnInit} from '@angular/core';
import {FormBuilder, FormGroup, Validators} from '@angular/forms';
import { ToastService } from '../../../../../core/services/toast.service';
import { extractErrorMessage } from '../../../../../shared/utils/error-utils';
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
  filteredUsers: UserDto[] = [];
  filteredProjects: ProjectDto[] = [];
  filteredWorkflows: WorkflowDto[] = [];
  userSearch = '';
  projectSearch = '';
  workflowSearch = '';

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
    this.setupFallbackFiltering();
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
      this.users = (res.content || []).filter(u => u.role === 'REQUESTER');
      this.filteredUsers = [...this.users];
      this.applyFallbackFilters();
    });
    this.projectService.getAllProjects().subscribe(res => {
      this.projects = res || [];
      this.filteredProjects = [...this.projects];
      this.applyFallbackFilters();
    });
    this.workflowService.getAllWorkflows({ page: 0, size: 1000 }, undefined, true ).subscribe(res => {
      this.workflows = res.content || [];
      this.filteredWorkflows = [...this.workflows];
      this.applyFallbackFilters();
    });
  }

  private setupFallbackFiltering(): void {
    this.settingsForm.get('fallbackProjectId')?.valueChanges.subscribe(() => {
      this.applyFallbackFilters();
    });
    this.settingsForm.get('fallbackUserId')?.valueChanges.subscribe(() => {
      this.applyFallbackFilters();
    });
    this.settingsForm.get('fallbackWorkflowId')?.valueChanges.subscribe(() => {
      this.applyFallbackFilters();
    });
  }

  private applyFallbackFilters(): void {
    const rawProjectId = this.settingsForm.get('fallbackProjectId')?.value;
    const rawUserId = this.settingsForm.get('fallbackUserId')?.value;
    const rawWorkflowId = this.settingsForm.get('fallbackWorkflowId')?.value;

    const selectedProjectId = rawProjectId ? Number(rawProjectId) : null;
    const selectedUserId = rawUserId ? Number(rawUserId) : null;
    const selectedWorkflowId = rawWorkflowId ? Number(rawWorkflowId) : null;

    let userTeamId: number | undefined = undefined;
    let userDeptId: number | undefined = undefined;
    if (selectedUserId) {
      const selectedUser = this.users.find(u => u.id === selectedUserId);
      if (selectedUser?.teamId) userTeamId = selectedUser.teamId;
      if (selectedUser?.departmentId) userDeptId = selectedUser.departmentId;
    }

    let projTeamId: number | undefined = undefined;
    let projDeptId: number | undefined = undefined;
    if (selectedProjectId) {
      const selectedProject = this.projects.find(p => p.id === selectedProjectId);
      if (selectedProject?.teamId) projTeamId = selectedProject.teamId;
      if (selectedProject?.departmentId) projDeptId = selectedProject.departmentId;
    }

    let workflowDeptId: number | undefined = undefined;
    if (selectedWorkflowId) {
      const selectedWorkflow = this.workflows.find(w => w.id === selectedWorkflowId);
      if (selectedWorkflow?.department?.id) workflowDeptId = selectedWorkflow.department.id;
    }

    const userFilterTeamId = projTeamId;
    const userFilterDeptId = projDeptId || workflowDeptId;

    this.filteredUsers = this.users.filter(u => {
      if (userFilterTeamId && u.teamId !== userFilterTeamId) return false;
      if (userFilterDeptId && u.departmentId !== userFilterDeptId) return false;
      return true;
    });

    const projFilterTeamId = userTeamId;
    const projFilterDeptId = userDeptId || workflowDeptId;

    this.filteredProjects = this.projects.filter(p => {
      if (projFilterTeamId && p.teamId !== projFilterTeamId) return false;
      if (projFilterDeptId && p.departmentId !== projFilterDeptId) return false;
      return true;
    });

    const workflowFilterDeptId = userDeptId || projDeptId;
    const isUserOrProjSelected = !!(selectedUserId || selectedProjectId);

    this.filteredWorkflows = this.workflows.filter(w => {
      if (!w.department?.id) {
        return true;
      }
      if (workflowFilterDeptId) {
        return w.department.id === workflowFilterDeptId;
      }
      if (isUserOrProjSelected) {
         return false;
      }
      return true;
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
      error: (err) => {
        this.loading = false;
        this.toastService.showError(extractErrorMessage(err, 'Failed to load config'));
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
          error: (err) => {
            this.loading = false;
            this.toastService.showError(extractErrorMessage(err, 'Failed to update config'));
          }
        });
      } else {
        this.jiraConfigService.createConfig(formValue).subscribe({
          next: () => {
            this.toastService.showSuccess('Config created successfully');
            this.router.navigate(['/integrations']);
          },
          error: (err) => {
            this.loading = false;
            this.toastService.showError(extractErrorMessage(err, 'Failed to create config'));
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
