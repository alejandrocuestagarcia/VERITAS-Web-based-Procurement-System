import {Component, OnInit} from '@angular/core';
import {FormBuilder, FormGroup, Validators} from '@angular/forms';
import {MatSnackBar} from '@angular/material/snack-bar';
import {ActivatedRoute, Router} from '@angular/router';
import {JiraConfigControllerService} from '../../core/api/api/jiraConfigController.service';

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

  constructor(
    private fb: FormBuilder,
    private jiraConfigService: JiraConfigControllerService,
    private snackBar: MatSnackBar,
    private route: ActivatedRoute,
    private router: Router
  ) {
  }

  ngOnInit(): void {
    this.initForm();

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

  private initForm(): void {
    this.settingsForm = this.fb.group({
      name: ['', Validators.required],
      jiraUrl: ['', Validators.required],
      username: ['', Validators.required],
      apiToken: [''],
      jql: ['', Validators.required],
      syncIntervalMinutes: [60, [Validators.required, Validators.min(1)]],
      customFieldId: ['', Validators.required]
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
          customFieldId: config.customFieldId
        });
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.snackBar.open('Failed to load config', 'Close', {duration: 3000});
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
            this.snackBar.open('Config updated successfully', 'Close', {duration: 3000});
            this.router.navigate(['/integrations']);
          },
          error: () => {
            this.loading = false;
            this.snackBar.open('Failed to update config', 'Close', {duration: 3000});
          }
        });
      } else {
        this.jiraConfigService.createConfig(formValue).subscribe({
          next: () => {
            this.snackBar.open('Config created successfully', 'Close', {duration: 3000});
            this.router.navigate(['/integrations']);
          },
          error: () => {
            this.loading = false;
            this.snackBar.open('Failed to create config', 'Close', {duration: 3000});
          }
        });
      }
    } else {
      this.settingsForm.markAllAsTouched();
      this.snackBar.open('Please correct the highlighted errors before submitting.', 'Close', {duration: 4000});
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
          this.snackBar.open('Connection successful!', 'Close', {
            duration: 3000,
            panelClass: ['bg-green-600', 'text-white']
          });
        } else {
          this.snackBar.open('Connection failed. Check credentials.', 'Close', {
            duration: 4000,
            panelClass: ['bg-red-600', 'text-white']
          });
        }
      },
      error: () => {
        this.testingConnection = false;
        this.snackBar.open('Error testing connection.', 'Close', {duration: 3000});
      }
    });
  }
}
