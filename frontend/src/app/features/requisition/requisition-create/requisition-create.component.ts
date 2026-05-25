import {Component, OnInit, ViewChild, ElementRef, OnDestroy} from '@angular/core';
import { FormBuilder, FormGroup, Validators, FormArray } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { ToastService } from '../../../core/services/toast.service';
import {
  debounceTime,
  distinctUntilChanged,
  forkJoin,
  Observable,
  of,
  startWith,
  Subject,
  Subscription,
  switchMap
} from 'rxjs';
import { catchError } from 'rxjs/operators';
import {
  ProjectModuleService,
  RequisitionModuleService,
  WorkflowModuleService,
  ProjectDto,
  WorkflowDto,
  RequisitionCreateDto,
  RequisitionCreateDtoPriorityEnum
} from '../../../core/api';
import { MatDialog } from '@angular/material/dialog';
import { WorkflowEditorComponent } from '../../workflow/workflow-editor/workflow-editor.component';
import { AssigneeSelectDialogComponent } from '../../../shared/components/assignee-select-dialog/assignee-select-dialog.component';

@Component({
  selector: 'app-requisition-create',
  templateUrl: './requisition-create.component.html'
})
export class RequisitionCreateComponent implements OnInit, OnDestroy {
  basicInfoForm!: FormGroup;
  lineItemsForm!: FormGroup;
  loading = false;
  isEditMode = false;
  requestId?: number;
  requestState?: string;

  projects: ProjectDto[] = [];
  projectSearch: string = '';
  workflows: WorkflowDto[] = [];
  workflowSearch: string = '';
  priorities = Object.values(RequisitionCreateDtoPriorityEnum)
  uploadedFiles: File[] = [];
  isDragging = false;

  @ViewChild('projectSearchInput') projectSearchInput!: ElementRef<HTMLInputElement>;
  @ViewChild('workflowSearchInput') workflowSearchInput!: ElementRef<HTMLInputElement>;
  @ViewChild('fileInput') fileInput!: ElementRef<HTMLInputElement>;

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private route: ActivatedRoute,
    private projectService: ProjectModuleService,
    private workflowService: WorkflowModuleService,
    private requisitionService: RequisitionModuleService,
    private toastService: ToastService,
    private dialog: MatDialog
  ) { }

  private workflowSearch$ = new Subject<string>();
  private searchSubscription?: Subscription;

  ngOnInit(): void {
    this.initForms();
    this.loadData();

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.isEditMode = true;
      this.requestId = Number(idParam);
      this.loadRequestDetails(this.requestId);
    }

    this.searchSubscription = this.workflowSearch$.pipe(
      startWith(''),
      debounceTime(400),
      distinctUntilChanged(),
      switchMap(search => {
        const pageable = {page: 0, size: 100, sort: ['name,asc'] };
        return this.workflowService.getAllWorkflows(pageable, search, true);
      })
    ).subscribe({
      next: (response) => {
        this.workflows = response.content || [];
      },
      error: () => {
        this.toastService.showError('Error searching workflows');
      }
    });
  }

  loadRequestDetails(id: number): void {
    this.loading = true;
    this.projectService.getAllProjects().subscribe({
      next: (projects) => {
        this.projects = projects;
        const pageable = { page: 0, size: 100, sort: ['name,asc'] };
        this.workflowService.getAllWorkflows(pageable, '', true).subscribe({
          next: (workflowPage) => {
            this.workflows = workflowPage.content || [];
            this.requisitionService.getRequestById(id).subscribe({
              next: (req) => {
                if (req.state !== 'DRAFT') {
                  this.router.navigate(['/dashboard']);
                  return;
                }
                this.loading = false;
                this.requestState = req.state;
                this.patchFormWithRequest(req);
              },
              error: () => {
                this.loading = false;
                this.toastService.showError('Failed to load request details');
              }
            });
          },
          error: () => {
            this.loading = false;
            this.toastService.showError('Failed to load workflows');
          }
        });
      },
      error: () => {
        this.loading = false;
        this.toastService.showError('Failed to load projects');
      }
    });
  }

  patchFormWithRequest(req: any): void {
    const project = this.projects.find(p => p.name === req.projectName);
    const workflow = this.workflows.find(w => w.name === req.workflowName);

    this.basicInfoForm.patchValue({
      requestName: req.requestName,
      projectId: project ? project.id : null,
      priority: req.priority,
      workflowDefinitionId: workflow ? workflow.id : null,
      description: req.description
    });

    const itemsFormArray = this.items;
    itemsFormArray.clear();
    if (req.items && req.items.length > 0) {
      req.items.forEach((item: any) => {
        itemsFormArray.push(this.fb.group({
          name: [item.name, Validators.required],
          quantity: [item.quantity, [Validators.required, Validators.min(1)]],
          unit: [item.unit, Validators.required],
          description: [item.description || '']
        }));
      });
    } else {
      itemsFormArray.push(this.createItem());
    }
  }

  getFilteredProjects(): ProjectDto[] {
    if (!this.projectSearch) return this.projects;
    const search = this.projectSearch.toLowerCase();
    return this.projects.filter(p => p.name?.toLowerCase().includes(search));
  }

  onWorkflowSearchChange(value: string): void{
    this.workflowSearch = value;
    this.workflowSearch$.next(value);
  }

  getSelectedProjectName(): string {
    const id = this.basicInfoForm.get('projectId')?.value;
    const project = this.projects.find(p => p.id === id);
    return project?.name || 'Not selected';
  }

  getSelectedWorkflowName(): string {
    const id = this.basicInfoForm.get('workflowDefinitionId')?.value;
    const workflow = this.workflows.find(w => w.id === id);
    return workflow?.name || 'Not selected';
  }


  onOpenedChange(opened: boolean): void {
    if (!opened) {
      this.projectSearch = '';
    } else {
      setTimeout(() => {
        if (this.projectSearchInput) {
          this.projectSearchInput.nativeElement.focus();
        }
      }, 100);
    }
  }
  viewWorkflow(id: number | undefined): void {
    if (id) {
      this.dialog.open(WorkflowEditorComponent, {
        width: '90vw',
        height: '90vh',
        maxWidth: '1200px',
        panelClass: 'overflow-hidden',
        data: { mode: 'view', id: id }
      });
    }
  }

  onFileSelected(event: any): void {
    const files = event.target.files;
    this.addFiles(files);
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging = true;
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging = false;
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging = false;
    const files = event.dataTransfer?.files;
    this.addFiles(files);
  }

  private addFiles(files: FileList | null | undefined): void {
    if (files) {
      for (let i = 0; i < files.length; i++) {
        this.uploadedFiles.push(files[i]);
      }
    }
  }

  removeFile(index: number): void {
    this.uploadedFiles.splice(index, 1);
  }

  triggerFileInput(): void {
    this.fileInput.nativeElement.click();
  }

  private initForms(): void {
    this.basicInfoForm = this.fb.group({
      requestName: ['', Validators.required],
      projectId: [null, Validators.required],
      priority: ['MEDIUM', Validators.required],
      workflowDefinitionId: [null, Validators.required],
      description: ['']
    });

    this.lineItemsForm = this.fb.group({
      items: this.fb.array([this.createItem()])
    });
  }

  private createItem(): FormGroup {
    return this.fb.group({
      name: ['', Validators.required],
      quantity: [1, [Validators.required, Validators.min(1)]],
      unit: ['', Validators.required],
      description: ['']
    });
  }

  get items(): FormArray {
    return this.lineItemsForm.get('items') as FormArray;
  }

  addItem(): void {
    this.items.push(this.createItem());
  }

  removeItem(index: number): void {
    if (this.items.length > 1) {
      this.items.removeAt(index);
    }
  }

  private loadData(): void {
    if (!this.isEditMode) {
      this.projectService.getAllProjects().subscribe({
        next: (projects) => this.projects = projects,
        error: () => this.toastService.showError('Failed to load projects')
      });
    }
  }

  isDraft(): boolean {
    return !this.isEditMode || this.requestState === 'DRAFT';
  }

  onSubmit(submitAfterSave = false): void {
    if (this.basicInfoForm.valid && this.lineItemsForm.valid) {
      this.loading = true;
      const request: any = {
        requestName: this.basicInfoForm.value.requestName,
        projectId: this.basicInfoForm.value.projectId,
        workflowDefinitionId: this.basicInfoForm.value.workflowDefinitionId,
        priority: this.basicInfoForm.value.priority as any,
        description: this.basicInfoForm.value.description,
        items: this.items.value.map((item: any) => ({
          name: item.name,
          quantity: item.quantity,
          unit: item.unit,
          description: item.description
        }))
      };

      if (this.isEditMode && this.requestId) {
        this.requisitionService.updateRequest(this.requestId, request).subscribe({
          next: (updatedRequest) => {
            this.handleUploadAndNavigate(updatedRequest, 'Procurement request updated successfully', submitAfterSave);
          },
          error: (err) => {
            this.loading = false;
            this.toastService.showError('Failed to update request: ' + (err.error || 'Unknown error'));
          }
        });
      } else {
        this.requisitionService.createRequest(request).subscribe({
          next: (createdRequest) => {
            this.handleUploadAndNavigate(createdRequest, 'Procurement request created successfully', submitAfterSave);
          },
          error: (err) => {
            this.loading = false;
            this.toastService.showError('Failed to create request: ' + (err.error || 'Unknown error'));
          }
        });
      }
    } else {
      this.toastService.showError('Please fill out all required fields properly.');
      this.basicInfoForm.markAllAsTouched();
      this.lineItemsForm.markAllAsTouched();
    }
  }
  private handleUploadAndNavigate(req: any, successMessage: string, submitAfterSave: boolean): void {
    if (this.uploadedFiles.length > 0 && req.id) {
      const uploadTasks: Observable<any>[] = this.uploadedFiles.map(file => {
        return this.requisitionService.uploadQuotes(req.id!, file as any).pipe(
          catchError(() => of(null)) // Prevent a single bad upload from blocking everything
        );
      });

      forkJoin(uploadTasks).subscribe({
        next: () => this.finalizeNavigation(req.id, successMessage, submitAfterSave),
        error: () => this.finalizeNavigation(req.id, successMessage, submitAfterSave)
      });
    } else {
      this.finalizeNavigation(req.id, successMessage, submitAfterSave);
    }
  }
  private finalizeNavigation(requestId: number, successMessage: string, submitAfterSave: boolean): void {
    if (submitAfterSave && requestId) {
      this.loading = true;
      this.requisitionService.getNextStepRole(requestId).subscribe({
        next: (role) => {
          if (role) {
            if (role === 'REQUESTER') {
              this.executeSubmission(requestId, null, successMessage);
              return;
            }

            this.requisitionService.getEligibleAssignees(requestId, role).subscribe({
              next: (users) => {
                this.loading = false;
                const activeUsers = users.filter(u => u.active !== false);

                const dialogRef = this.dialog.open(AssigneeSelectDialogComponent, {
                  width: '80vw',
                  maxWidth: '550px',
                  disableClose: true,
                  data: {
                    role: role,
                    users: activeUsers
                  }
                });

                dialogRef.afterClosed().subscribe((assigneeId: number | null | undefined) => {
                  if (assigneeId === undefined) {
                    this.toastService.showSuccess(`${successMessage} (saved as draft, submission cancelled)`);
                    this.router.navigate(['/requisitions', requestId]);
                    return;
                  }
                  this.executeSubmission(requestId, assigneeId, successMessage);
                });
              },
              error: (err) => {
                this.loading = false;
                this.toastService.showError('Failed to fetch assignees for role ' + role);
                this.router.navigate(['/requisitions', requestId]);
              }
            });
          } else {
            this.executeSubmission(requestId, null, successMessage);
          }
        },
        error: (err) => {
          this.loading = false;
          this.toastService.showError('Failed to determine next step role');
          this.router.navigate(['/requisitions', requestId]);
        }
      });
    } else {
      this.loading = false;
      this.toastService.showSuccess(successMessage);
      this.router.navigate(['/requisitions', requestId]);
    }
  }

  private executeSubmission(requestId: number, assigneeId: number | null, successMessage: string): void {
    this.loading = true;
    const nextAssigneeId = assigneeId !== null ? assigneeId : undefined;
    this.requisitionService.submitRequest(requestId, nextAssigneeId).subscribe({
      next: () => {
        this.loading = false;
        this.toastService.showSuccess(`${successMessage} & submitted successfully`);
        this.router.navigate(['/requisitions', requestId]);
      },
      error: (err) => {
        this.loading = false;
        this.toastService.showError('Request was saved, but failed to submit: ' + (err.error?.message || 'Unknown error'));
        this.router.navigate(['/requisitions', requestId]);
      }
    });
  }

  onCancel(): void {
    this.router.navigate(['/requisitions']);
  }

  ngOnDestroy(): void {
    this.searchSubscription?.unsubscribe()
  }
}
